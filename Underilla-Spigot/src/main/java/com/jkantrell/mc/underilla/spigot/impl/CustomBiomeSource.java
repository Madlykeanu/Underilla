package com.jkantrell.mc.underilla.spigot.impl;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import com.jkantrell.mc.underilla.spigot.Underilla;
import com.mojang.serialization.MapCodec;
import jakarta.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate.Sampler;

public class CustomBiomeSource extends BiomeSource implements java.util.function.Supplier<Set<Holder<Biome>>> {
    private final BiomeSource vanillaBiomeSource;
    private final BukkitWorldReader worldSurfaceReader;
    private final BukkitWorldReader worldCavesReader;
    private final Map<String, Long> biomesPlaced;
    private long lastInfoPrinted = 0;
    private long lastWarnningPrinted = 0;

    public CustomBiomeSource(@Nullable BiomeSource vanillaBiomeSource, @Nonnull BukkitWorldReader worldSurfaceReader,
            @Nullable BukkitWorldReader worldCavesReader) {
        this.vanillaBiomeSource = vanillaBiomeSource;
        this.worldSurfaceReader = worldSurfaceReader;
        this.worldCavesReader = worldCavesReader;
        this.biomesPlaced = new ConcurrentHashMap<>();
    }

    public Map<String, Long> getBiomesPlaced() { return biomesPlaced; }

    @Override
    protected MapCodec<? extends BiomeSource> codec() { return null; }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        Registry<Biome> biomeRegistry = NMSBiomeUtils.getBiomeRegistry();
        Underilla.getInstance().getLogger().info(
                "Collecting possible biomes: " + biomeRegistry.stream().map(biome -> biomeRegistry.getKey(biome).toString()).toList());
        return biomeRegistry.stream().map(biomeRegistry::wrapAsHolder);
    }

    /**
     * Get biome at x, y, z.
     * 
     * @param x Biome coordinate. (*4 to get world coordinate)
     * @param y Biome coordinate. (*4 to get world coordinate)
     * @param z Biome coordinate. (*4 to get world coordinate)
     */
    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, @Nonnull Sampler noise) {
        BukkitBiome vanillaBiome = null;
        if (vanillaBiomeSource != null && noise != null) {
            vanillaBiome = new BukkitBiome(vanillaBiomeSource.getNoiseBiome(x, y, z, noise).getRegisteredName());
        }
        // Edit value to mach actual world coordinates.
        x = x << 2;
        y = y << 2;
        z = z << 2;
        return getBiome(x, y, z, vanillaBiome).getBiomeHolder();
    }

    /**
     * Get biome at x, y, z.
     * 
     * @param x     Actual world coordinate.
     * @param y     Actual world coordinate.
     * @param z     Actual world coordinate.
     * @param noise Noise used to get biome.
     * @return
     */
    public BukkitBiome getBiome(int x, int y, int z, @Nullable BukkitBiome vanillaBiome) {

        // Keep biome from vanilla noise biome generation if it's in the list of keptUndergroundBiomes.
        // Cave biomes are never kept here from vanilla world, you need a cave world to keep them.
        if (!Underilla.CONFIG.transferBiomes) {
            if (vanillaBiome == null) {
                warning("We can't use vanillaBiome because it's null at " + x + " " + y + " " + z);
                String key = "error:plains";
                biomesPlaced.put(key, biomesPlaced.getOrDefault(key, 0L) + 1);
                return BukkitBiome.DEFAULT;
            } else {
                info("Use vanillaBiome because we don't transfer biome or it's a keptUndergroundBiomes: " + vanillaBiome.getName() + " at "
                        + x + " " + y + " " + z);
                String key = "noise:" + vanillaBiome.getName();
                biomesPlaced.put(key, biomesPlaced.getOrDefault(key, 0L) + 1);
                return vanillaBiome;
            }
        }

        // Needed to get surface biome & test if caves biome will override a preserved biome.
        BukkitBiome surfaceWorldBiome = (BukkitBiome) worldSurfaceReader.biomeAt(x, y, z).orElse(null);

        // Get biome from cave world if it's in the list of transferWorldFromCavesWorld.
        // & surface biome does not have a preserved biome here.
        // & it's below the surface.
        if (Underilla.CONFIG.transferBiomesFromCavesWorld && worldCavesReader != null
                && (surfaceWorldBiome == null || !Underilla.CONFIG.preserveBiomes.contains(surfaceWorldBiome.getName()))
                && y < Underilla.CONFIG.mergeLimit - Underilla.CONFIG.mergeDepth && y < 50) {
            // TODO use real surface height instead of the max one (mergeLimit - mergeDepth).
            // For now there is as 50 hard max limits.
            BukkitBiome cavesWorldBiome = (BukkitBiome) worldCavesReader.biomeAt(x, y, z).orElse(null);
            if (cavesWorldBiome != null && Underilla.CONFIG.transferCavesWorldBiomes.contains(cavesWorldBiome.getName())) {
                info("Use cavesWorldBiome because it's a transferedCavesWorldBiomes: " + cavesWorldBiome.getName() + " at " + x + " " + y
                        + " " + z);
                String key = "caves:" + cavesWorldBiome.getName();
                biomesPlaced.put(key, biomesPlaced.getOrDefault(key, 0L) + 1);
                return cavesWorldBiome;
            }
        }

        // Get biome from surface world.
        if (surfaceWorldBiome != null) {
            info("Use surfaceWorldBiome: " + surfaceWorldBiome.getName() + " at " + x + " " + y + " " + z);
            biomesPlaced.put("surface:" + surfaceWorldBiome.getName(),
                    biomesPlaced.getOrDefault("surface:" + surfaceWorldBiome.getName(), 0L) + 1);
            return surfaceWorldBiome;
        }

        // If no other biome found, use vanilla biome.
        warning("Use vanilla because no other biome found at " + x + " " + y + " " + z);
        if (vanillaBiome != null) {
            String key = "notfound:" + vanillaBiome.getName();
            biomesPlaced.put(key, biomesPlaced.getOrDefault(key, 0L) + 1);
            return vanillaBiome;
        } else {
            String key = "error:plains";
            biomesPlaced.put(key, biomesPlaced.getOrDefault(key, 0L) + 1);
            return BukkitBiome.DEFAULT;
        }
    }

    private synchronized void info(String message) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInfoPrinted > 1000) {
            Underilla.getInstance().getLogger().info(message);
            lastInfoPrinted = currentTime;
        }
    }
    private synchronized void warning(String message) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastWarnningPrinted > 1000) {
            Underilla.getInstance().getLogger().warning(message);
            lastWarnningPrinted = currentTime;
        }
    }

    /**
     * I don't understand why BiomeSource need to extend Supplier, but it's needed to work with paper.
     */
    @Override
    public Set<Holder<Biome>> get() { return collectPossibleBiomes().distinct().collect(java.util.stream.Collectors.toSet()); }

}
