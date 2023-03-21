package com.dotkntrell.mc.terraformer.generation;

import com.dotkntrell.mc.terraformer.io.reader.ChunkReader;
import com.dotkntrell.mc.terraformer.io.reader.WorldReader;
import com.dotkntrell.mc.terraformer.util.VectorIterable;
import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.util.Vector;
import java.util.Random;

class AbsoluteMerger implements Merger {

    //FIELDS
    private final WorldReader worldReader_;
    private final int height_;


    //CONSTRUCTORS
    AbsoluteMerger(WorldReader worldReader, int height) {
        this.worldReader_ = worldReader;
        this.height_ = height;
    }


    //OVERWRITES
    @Override
    public void merge(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkGenerator.ChunkData chunkData, ChunkReader chunkReader) {
        int airColumn = Math.max(chunkReader.airColumnBottomHeight(), this.height_);
        chunkData.setRegion(0, airColumn, 0, 15, chunkData.getMaxHeight(), 15, Material.AIR);

        VectorIterable iterable = new VectorIterable(0, 16, this.height_, airColumn, 0, 16);
        for (Vector v : iterable) {
            Material material = chunkReader.materialAt(v.getBlockX(),v.getBlockY(),v.getBlockZ()).orElse(Material.AIR);
            chunkData.setBlock(v.getBlockX(), v.getBlockY(), v.getBlockZ(), material);
        }
    }
}
