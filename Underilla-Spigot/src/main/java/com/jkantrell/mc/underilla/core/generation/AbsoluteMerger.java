package com.jkantrell.mc.underilla.core.generation;

import fr.formiko.mc.biomeutils.NMSBiomeUtils;
import java.util.List;
import javax.annotation.Nullable;
import com.jkantrell.mc.underilla.core.api.Block;
import com.jkantrell.mc.underilla.core.api.ChunkData;
import com.jkantrell.mc.underilla.core.reader.ChunkReader;
import com.jkantrell.mc.underilla.core.reader.Reader;
import com.jkantrell.mc.underilla.core.vector.IntVector;
import com.jkantrell.mc.underilla.core.vector.Vector;
import com.jkantrell.mc.underilla.core.vector.VectorIterable;
import com.jkantrell.mc.underilla.spigot.Underilla;
import com.jkantrell.mc.underilla.spigot.io.UnderillaConfig.SetBiomeStringKeys;
import com.jkantrell.mca.MCAUtil;

class AbsoluteMerger implements Merger {

    // FIELDS
    private final int height_;
    private final List<String> keptReferenceWorldBlocks_;
    private final int mergeDepth_;

    // CONSTRUCTORS
    AbsoluteMerger(int height, List<String> keptReferenceWorldBlocks, int mergeDepth) {
        this.height_ = height;
        this.keptReferenceWorldBlocks_ = keptReferenceWorldBlocks;
        this.mergeDepth_ = mergeDepth;
    }


    // IMPLEMENTATIONS
    @Override
    public void mergeLand(ChunkReader surfaceReader, ChunkData chunkData, @Nullable ChunkReader cavesReader) {
        long startTime = System.currentTimeMillis();
        Block airBlock = surfaceReader.blockFromTag(MCAUtil.airBlockTag()).get();
        // int airColumn = Math.max(reader.airSectionsBottom(), -64);
        int airColumn = surfaceReader.airSectionsBottom();
        chunkData.setRegion(0, airColumn, 0, 16, chunkData.getMaxHeight(), 16, airBlock);

        VectorIterable iterable = new VectorIterable(0, 16, -64, airColumn, 0, 16);
        int columnHeigth = this.height_;
        int lastX = -1;
        int lastZ = -1;
        Generator.addTime("Create VectorIterable to merge land", startTime);
        for (Vector<Integer> v : iterable) {
            startTime = System.currentTimeMillis();
            Block customBlock = surfaceReader.blockAt(v).orElse(airBlock);
            Generator.addTime("Read block data from custom world", startTime);
            startTime = System.currentTimeMillis();
            Block vanillaBlock = cavesReader == null ? chunkData.getBlock(v) : cavesReader.blockAt(v).orElse(airBlock);
            Generator.addTime("Read block data from vanilla world", startTime);

            // For every collumn of bloc calculate the lower block to remove that migth be lower than height_.
            startTime = System.currentTimeMillis();
            if (v.x() != lastX || v.z() != lastZ) {
                lastX = v.x();
                lastZ = v.z();
                columnHeigth = (isPreservedBiome(surfaceReader, v) ? -64 : getLowerBlockToRemove(surfaceReader, v.x(), v.z(), airBlock));
            }
            Generator.addTime("Calculate lower block to remove", startTime);

            startTime = System.currentTimeMillis();
            // Place the custom world bloc over 55 (or -64 if is preseved biome) or if it is a custom ore or if it is air,
            // or if vanilla world have watter or grass or sand over 30
            // and do not replace liquid vanilla blocks by air. (to preserve water and lava lackes)
            if (((v.y() > columnHeigth) // block over surface or close to surface are kept from custom surface world.
                    || (isCustomWorldOreOutOfVanillaCaves(customBlock, vanillaBlock)) // custom world ores are kept from custom world.
                    // vanilla sea ground are replaced by custom world blocks.
                    || (v.y() > 30 && (vanillaBlock.isLiquid() || vanillaBlock.getName().equalsIgnoreCase("GRASS_BLOCK")
                            || vanillaBlock.getName().equalsIgnoreCase("SAND") || vanillaBlock.getName().equalsIgnoreCase("SAND_STONE")
                            || vanillaBlock.getName().equalsIgnoreCase("GRAVEL"))))
                    // Keep custom block if it's air to preserve custom world caves if there is any. (If vanilla block is liquid, we
                    // preserve vanilla block as we want to avoid holes in vanilla underground lakes)
                    || (customBlock.isAir() && !vanillaBlock.isLiquid())) {
                // Use custom block
                chunkData.setBlock(v, customBlock);
            } else {
                if (cavesReader != null) {
                    // Use vanilla from caves world
                    chunkData.setBlock(v, vanillaBlock);
                }
                // Use vanilla block from current world
            }

            Generator.addTime("Merge block or not", startTime);
        }
    }

    /** return the 1st block mergeDepth_ blocks under surface or heigth_ */
    private int getLowerBlockToRemove(Reader surfaceReader, int x, int z, Block defaultBlock) {
        int lbtr = this.height_ + mergeDepth_;
        while (!surfaceReader.blockAt(x, lbtr, z).orElse(defaultBlock).isSolidAndSurfaceBlock() && lbtr > -64) {
            lbtr--;
        }
        return lbtr - mergeDepth_;
    }


    // private --------------------------------------------------------------------------------------------------------
    /**
     * Return true if this block is a block to preserve from the custom world and a solid block in vanilla world (This avoid to have ores
     * floating in the air in vanilla caves).
     * 
     * @param customBlock  the block to check if is ore
     * @param vanillaBlock the block in vanilla world
     */
    private boolean isCustomWorldOreOutOfVanillaCaves(Block customBlock, Block vanillaBlock) {
        return keptReferenceWorldBlocks_.contains(customBlock.getName().toUpperCase()) && (vanillaBlock == null || vanillaBlock.isSolid());
    }
    /** Return true if this biome need to be only custom world */
    private boolean isPreservedBiome(ChunkReader reader, Vector<Integer> v) {
        String biomeKey = NMSBiomeUtils.normalizeBiomeName(reader.biomeAt(v).orElseThrow().getName());
        return Underilla.getUnderillaConfig().isBiomeInSet(SetBiomeStringKeys.SURFACE_WORLD_ONLY_ON_THIS_BIOMES, biomeKey);
    }

    /** Return true if all the collumn is air. */
    private boolean isAirCollumn(ChunkData chunkData, Vector<Integer> v, int len) {
        for (int i = 0; i < len; i++) {
            if (!chunkData.getBlock(new IntVector(v.x(), v.y() - i, v.z())).isAir()) {
                return false;
            }
        }
        return true;
    }
}
