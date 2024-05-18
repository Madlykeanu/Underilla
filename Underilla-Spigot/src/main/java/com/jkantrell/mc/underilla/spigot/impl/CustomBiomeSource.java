package com.jkantrell.mc.underilla.spigot.impl;

import java.util.stream.Stream;
import com.jkantrell.mc.underilla.spigot.Underilla;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate.Sampler;

public class CustomBiomeSource extends BiomeSource {
    private final BiomeSource vanillaBiomeSource;

    public CustomBiomeSource(BiomeSource vanillaBiomeSource) { this.vanillaBiomeSource = vanillaBiomeSource; }

    @Override
    protected MapCodec<? extends BiomeSource> codec() { throw new UnsupportedOperationException("Not supported"); }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        Registry<Biome> biomeRegistry = NMSBiomeUtils.getBiomeRegistry();
        Underilla.getInstance().getLogger().info(
                "Collecting possible biomes: " + biomeRegistry.stream().map(biome -> biomeRegistry.getKey(biome).toString()).toList());
        return biomeRegistry.stream().map(biome -> biomeRegistry.wrapAsHolder(biome));
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Sampler noise) {
        Holder<Biome> vanillaBiome = vanillaBiomeSource.getNoiseBiome(x, y, z, noise);
        if (vanillaBiome != null && Underilla.CONFIG.keptUndergroundBiomes.contains(vanillaBiome.getRegisteredName())) {
            Underilla.getInstance().getLogger().info("Use vanillaBiome because it's a keptUndergroundBiomes: "
                    + vanillaBiome.getRegisteredName() + " at " + x + ", " + y + ", " + z);
            return vanillaBiome;
        }
        // TODO get key from custom surface world.
        String biomeKey = "mvndi:baltic_coast";
        // Underilla.getInstance().getLogger().info("Getting noise biome: " + x + ", " + y + ", " + z + ": " + biomeKey);
        return NMSBiomeUtils.getBiomeRegistry().wrapAsHolder(NMSBiomeUtils.getBiome(biomeKey));
    }

}
