package com.jkantrell.mc.underilla.spigot.io;

import java.util.Collections;
import java.util.List;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.generator.structure.Structure;
import com.jkantrell.mc.underilla.core.generation.GenerationConfig;
import com.jkantrell.mc.underilla.core.generation.MergeStrategy;
import com.jkantrell.mc.underilla.spigot.impl.NMSBiomeUtils;
import com.jkantrell.yamlizer.yaml.AbstractYamlConfig;
import com.jkantrell.yamlizer.yaml.ConfigField;
import com.jkantrell.yamlizer.yaml.YamlElementType;

public class Config extends AbstractYamlConfig {

    // CONSTRUCTORS
    public Config(String filePath) {
        super(filePath);
        this.yamlizer.addSerializationRule(Structure.class, (element, type) -> {
            String name = element.get(YamlElementType.STRING).toLowerCase();
            return Registry.STRUCTURE.get(NamespacedKey.minecraft(name));
        });
    }


    // FIELDS
    @ConfigField(path = "reference_world")
    public String referenceWorldName = "backup";

    @ConfigField(path = "generate_noodle_caves")
    public boolean generateCaves = true;

    @ConfigField(path = "vanilla_population")
    public boolean vanillaPopulation = true;

    @ConfigField(path = "transfer_biomes")
    public boolean transferBiomes = true;

    @ConfigField(path = "transfer_blocks_from_caves_world")
    public boolean transferBlocksFromCavesWorld = false;

    @ConfigField(path = "transfer_biomes_from_caves_world")
    public boolean transferBiomesFromCavesWorld = false;

    @ConfigField(path = "caves_world")
    public String cavesWorldName = "caves_world";

    @ConfigField(path = "transfered_caves_world_biomes")
    public List<String> transferCavesWorldBiomes = List.of("minecraft:deep_dark", "minecraft:dripstone_caves", "minecraft:lush_caves");

    @ConfigField(path = "custom_biome_enabled")
    public boolean customBiomeEnabled = false;

    @ConfigField(path = "strategy")
    public MergeStrategy mergeStrategy = MergeStrategy.SURFACE;

    @ConfigField(path = "relative.upper_limit")
    public int mergeUpperLimit = 320;

    @ConfigField(path = "relative.lower_limit")
    public int mergeLowerLimit = -64;

    @ConfigField(path = "relative_and_surface.depth")
    public int mergeDepth = 12;

    @ConfigField(path = "kept_reference_world_blocks")
    public List<String> keptReferenceWorldBlocks = List.of();

    @ConfigField(path = "surface_and_absolute.limit")
    public int mergeLimit = 22;

    @ConfigField(path = "ignored_block_for_surface_calculation")
    public List<String> ignoredBlockForSurfaceCalculation = List.of("LEAVES", "LOGS");

    @ConfigField(path = "preserve_biomes")
    public List<String> preserveBiomes = List.of();

    @ConfigField(path = "ravin_biomes")
    public List<String> ravinBiomes = List.of();

    @ConfigField(path = "structures.enabled")
    public boolean generateStructures = true;

    @ConfigField(path = "structures.blacklist")
    public List<Structure> structureBlackList = Collections.emptyList();

    public GenerationConfig toGenerationConfig() {
        GenerationConfig r = new GenerationConfig();

        r.referenceWorldName = this.referenceWorldName;
        r.generateCaves = this.generateCaves;
        r.vanillaPopulation = this.vanillaPopulation;
        r.transferBiomes = this.transferBiomes;
        r.transferBlocksFromCavesWorld = this.transferBlocksFromCavesWorld;
        r.transferBiomesFromCavesWorld = this.transferBiomesFromCavesWorld;
        r.cavesWorldName = this.cavesWorldName;
        r.transferCavesWorldBiomes = NMSBiomeUtils.normalizeBiomeNameList(this.transferCavesWorldBiomes);
        r.mergeStrategy = this.mergeStrategy;
        r.mergeUpperLimit = this.mergeUpperLimit;
        r.mergeLowerLimit = this.mergeLowerLimit;
        r.mergeDepth = this.mergeDepth;
        r.keptReferenceWorldBlocks = this.keptReferenceWorldBlocks;
        r.preserveBiomes = NMSBiomeUtils.normalizeBiomeNameList(this.preserveBiomes);
        r.ravinBiomes = NMSBiomeUtils.normalizeBiomeNameList(this.ravinBiomes);
        r.mergeLimit = this.mergeLimit;
        r.generateStructures = this.generateStructures;

        return r;
    }
}
