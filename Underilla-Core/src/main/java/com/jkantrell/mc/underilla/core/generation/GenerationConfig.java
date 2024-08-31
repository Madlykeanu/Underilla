package com.jkantrell.mc.underilla.core.generation;

import java.util.Collections;
import java.util.List;

public class GenerationConfig {

    // FIELDS
    public String referenceWorldName = "backup";

    public boolean generateCaves = true;

    public boolean vanillaPopulation = true;

    public boolean transferBiomes = true;

    public boolean transferBlocksFromCavesWorld = false;

    public boolean transferBiomesFromCavesWorld = false;

    public String cavesWorldName = "caves_world";

    public List<String> transferCavesWorldBiomes = Collections.emptyList();

    public boolean customBiomeEnabled = false;

    public MergeStrategy mergeStrategy = MergeStrategy.SURFACE;

    public int mergeUpperLimit = 320;

    public int mergeLowerLimit = -64;

    public int mergeDepth = 12;

    public List<String> keptReferenceWorldBlocks = Collections.emptyList();

    public List<String> preserveBiomes = Collections.emptyList();

    public List<String> ravinBiomes = Collections.emptyList();

    public int mergeLimit = 22;

    public int mergeBlendRange = 8;

    public boolean generateStructures = true;

    public boolean needToMixBiomes() {
        // true if we transfer biomes and we have kept underground biomes and we are using relative merge strategy
        // return this.transferBiomes && !this.keptUndergroundBiomes.isEmpty() && MergeStrategy.RELATIVE.equals(this.mergeStrategy);
        return false;
    }
}
