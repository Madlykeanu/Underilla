package com.jkantrell.mc.underilla.spigot.generation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.bukkit.HeightMap;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import com.jkantrell.mc.underilla.core.api.HeightMapType;
import com.jkantrell.mc.underilla.core.generation.Generator;
import com.jkantrell.mc.underilla.core.reader.ChunkReader;
import com.jkantrell.mc.underilla.core.reader.WorldReader;
import com.jkantrell.mc.underilla.spigot.Underilla;
import com.jkantrell.mc.underilla.spigot.impl.BukkitChunkData;
import com.jkantrell.mc.underilla.spigot.impl.BukkitRegionChunkData;
import com.jkantrell.mc.underilla.spigot.impl.BukkitWorldInfo;
import com.jkantrell.mc.underilla.spigot.impl.BukkitWorldReader;
import com.jkantrell.mc.underilla.spigot.impl.CustomBiomeSource;
import com.jkantrell.mc.underilla.spigot.io.Config;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class UnderillaChunkGenerator extends ChunkGenerator {
    // TODO : For performance reason, we should generate and empty world if transfer_world_from_caves_world==true

    // ASSETS
    private static final Config CONFIG = Underilla.CONFIG;
    private static final Map<HeightMap, HeightMapType> HEIGHTMAPS_MAP = Map.of(HeightMap.OCEAN_FLOOR, HeightMapType.OCEAN_FLOOR,
            HeightMap.OCEAN_FLOOR_WG, HeightMapType.OCEAN_FLOOR_WG, HeightMap.MOTION_BLOCKING, HeightMapType.MOTION_BLOCKING,
            HeightMap.MOTION_BLOCKING_NO_LEAVES, HeightMapType.MOTION_BLOCKING_NO_LEAVES, HeightMap.WORLD_SURFACE,
            HeightMapType.WORLD_SURFACE, HeightMap.WORLD_SURFACE_WG, HeightMapType.WORLD_SURFACE_WG);


    // FIELDS
    private final Generator delegate_;
    private final @Nonnull com.jkantrell.mc.underilla.core.reader.WorldReader worldSurfaceReader;
    private final @Nullable com.jkantrell.mc.underilla.core.reader.WorldReader worldCavesReader;
    private static CustomBiomeSource customBiomeSource;


    // CONSTRUCTORS
    public UnderillaChunkGenerator(@Nonnull BukkitWorldReader worldSurfaceReader, @Nullable BukkitWorldReader worldCavesReader) {
        this.worldSurfaceReader = worldSurfaceReader;
        this.worldCavesReader = worldCavesReader;
        this.delegate_ = new Generator(worldSurfaceReader, CONFIG.toGenerationConfig());
    }


    // IMPLEMENTATIONS
    @Override
    public int getBaseHeight(WorldInfo worldInfo, Random random, int x, int z, HeightMap heightMap) {
        BukkitWorldInfo info = new BukkitWorldInfo(worldInfo);
        return this.delegate_.getBaseHeight(info, x, z, HEIGHTMAPS_MAP.get(heightMap));
    }

    @Override
    public void generateSurface(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {
        Optional<ChunkReader> reader = this.worldSurfaceReader.readChunk(chunkX, chunkZ);
        if (reader.isEmpty()) {
            return;
        }
        BukkitChunkData data = new BukkitChunkData(chunkData);
        // Bukkit.getLogger().info("Generating chunk [" + chunkX + ", " + chunkZ + "] from " + this.worldReader_.getWorldName() + ".");
        ChunkReader cavesReader = null;
        if (this.worldCavesReader != null && CONFIG.transferBlocksFromCavesWorld) {
            cavesReader = this.worldCavesReader.readChunk(chunkX, chunkZ).orElse(null);
        }
        this.delegate_.generateSurface(reader.get(), data, cavesReader);

    }


    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        // Caves are vanilla generated, but they are carved underwater, this re-places the water blocks in case they were carved into.
        return List.of(new Populator(this.worldSurfaceReader, this.delegate_));
    }

    @Override
    public boolean shouldGenerateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        return this.delegate_.shouldGenerateNoise(chunkX, chunkZ);
    }


    @Override
    public boolean shouldGenerateSurface(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        // Must always return true, bedrock and deepslate layers are generated in this step
        return this.delegate_.shouldGenerateSurface(chunkX, chunkZ);
    }


    @Override
    public boolean shouldGenerateCaves(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        return this.delegate_.shouldGenerateCaves(chunkX, chunkZ);
    }

    @Override
    public boolean shouldGenerateDecorations(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        return this.delegate_.shouldGenerateDecorations(chunkX, chunkZ);
    }

    @Override
    public boolean shouldGenerateMobs(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        return this.delegate_.shouldGenerateMobs(chunkX, chunkZ);
    }

    @Override
    public boolean shouldGenerateStructures(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        return this.delegate_.shouldGenerateStructures(chunkX, chunkZ);
    }

    // To support custom biomes, we can't use bukkit biome provider. So biome merging is done in CustomBiomeSource.
    @Override
    public BiomeProvider getDefaultBiomeProvider(@Nonnull WorldInfo worldInfo) {
        // TODO Mvndi patch, if biome name contains "beach" return beach biome, if biome names contains "ocean" but not "deep" return ocean.
        // This way we will have shipwrecks well placed.
        if (Underilla.CONFIG.customBiomeEnabled) {
            Underilla.getInstance().getLogger().info(
                    "Custom biomes are enable. Structures won't be generate in the right biome. But features will be in the right biome.");
            return null;
        } else if (!Underilla.CONFIG.transferBiomes) {
            Underilla.getInstance().getLogger().info(
                    "Biome aren't transfered from the reference world. This will generate the world with the default biome provider.");
            return null;
        } else {
            Underilla.getInstance().getLogger()
                    .info("Underilla Use the custom biome provider from file data. Structures will be generate in the right biome.");
            return new BiomeProviderFromFile();
        }
    }

    public static Map<String, Long> getBiomesPlaced() { return customBiomeSource.getBiomesPlaced(); }


    // CLASSES
    private static class Populator extends BlockPopulator {

        // FIELDS
        private final WorldReader worldReader_;
        private final Generator generator_;


        // CONSTRUCTORS
        public Populator(WorldReader reader, Generator generator) {
            this.worldReader_ = reader;
            this.generator_ = generator;
        }


        // OVERRITES
        @Override
        public void populate(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, LimitedRegion limitedRegion) {
            if (!CONFIG.generateCaves) {
                return;
            }
            ChunkReader reader = this.worldReader_.readChunk(chunkX, chunkZ).orElse(null);
            if (reader == null) {
                return;
            }
            BukkitRegionChunkData chunkData = new BukkitRegionChunkData(limitedRegion, chunkX, chunkZ, worldInfo.getMinHeight(),
                    worldInfo.getMaxHeight());
            this.generator_.reInsertLiquids(reader, chunkData);
        }
    }

    private class BiomeProviderFromFile extends BiomeProvider {

        private BiomeProviderFromFile() {
            customBiomeSource = new CustomBiomeSource(null, ((BukkitWorldReader) worldSurfaceReader),
                    ((BukkitWorldReader) worldCavesReader));
        }

        @Override
        public @Nonnull Biome getBiome(@Nonnull WorldInfo worldInfo, int x, int y, int z) {
            return customBiomeSource.getBiome(x, y, z, null).getBiome();
        }

        @Override
        public @Nonnull List<Biome> getBiomes(@Nonnull WorldInfo worldInfo) {
            return List.of(Biome.values()).stream().filter(b -> !b.equals(Biome.CUSTOM)).toList();
        }

    }
}
