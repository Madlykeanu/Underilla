package com.jkantrell.mc.underilla.spigot.io;

import fr.formiko.mc.biomeutils.NMSBiomeUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import com.jkantrell.mc.underilla.spigot.Underilla;

public class UnderillaConfig {
    private final EnumMap<BooleanKeys, Boolean> booleanMap;
    private final EnumMap<IntegerKeys, Integer> integerMap;
    private final EnumMap<StringKeys, String> stringMap;
    private final EnumMap<SetStringKeys, Set<String>> listStringMap;
    private final EnumMap<SetBiomeStringKeys, Set<String>> listBiomeStringMap;
    private final EnumMap<SetMaterialKeys, Set<Material>> listMaterialMap;


    public UnderillaConfig(FileConfiguration fileConfiguration) {
        booleanMap = new EnumMap<>(BooleanKeys.class);
        integerMap = new EnumMap<>(IntegerKeys.class);
        stringMap = new EnumMap<>(StringKeys.class);
        listStringMap = new EnumMap<>(SetStringKeys.class);
        listBiomeStringMap = new EnumMap<>(SetBiomeStringKeys.class);
        listMaterialMap = new EnumMap<>(SetMaterialKeys.class);
        reload(fileConfiguration);
    }

    public boolean getBoolean(BooleanKeys key) { return booleanMap.get(key); }
    public int getInt(IntegerKeys key) { return integerMap.get(key); }
    public String getString(StringKeys key) { return stringMap.get(key); }
    public Set<String> getSetString(SetStringKeys key) { return listStringMap.get(key); }
    public Set<String> getSetBiomeString(SetBiomeStringKeys key) { return listBiomeStringMap.get(key); }
    public boolean isStringInSet(SetStringKeys key, String value) { return getSetString(key).contains(value); }
    public boolean isBiomeInSet(SetBiomeStringKeys key, String biome) { return getSetBiomeString(key).contains(biome); }


    public void reload(FileConfiguration fileConfiguration) {
        booleanMap.clear();
        for (BooleanKeys key : BooleanKeys.values()) {
            if (!fileConfiguration.contains(key.path)) {
                Underilla.warning("Key " + key + " not found in config");
            }
            booleanMap.put(key, fileConfiguration.getBoolean(key.path, key.defaultValue));
        }

        integerMap.clear();
        for (IntegerKeys key : IntegerKeys.values()) {
            if (!fileConfiguration.contains(key.path)) {
                Underilla.warning("Key " + key + " not found in config");
            }
            int value = fileConfiguration.getInt(key.path, key.defaultValue);
            if (value > key.max) {
                Underilla.warning("Value " + value + " is greater than max " + key.max + " for key " + key);
                value = key.max;
            }
            if (value < key.min) {
                Underilla.warning("Value " + value + " is less than min " + key.min + " for key " + key);
                value = key.min;
            }
            integerMap.put(key, value);
        }

        stringMap.clear();
        for (StringKeys key : StringKeys.values()) {
            if (!fileConfiguration.contains(key.path)) {
                Underilla.warning("Key " + key + " not found in config");
            }
            stringMap.put(key, fileConfiguration.getString(key.path));
        }

        listStringMap.clear();
        for (SetStringKeys key : SetStringKeys.values()) {
            if (fileConfiguration.contains(key.path)) {
                listStringMap.put(key, new HashSet<>(fileConfiguration.getStringList(key.path)));
            } else {
                Underilla.warning("Key " + key + " not found in config");
                listStringMap.put(key, key.defaultValue);
            }
        }

        listMaterialMap.clear();
        for (SetMaterialKeys key : SetMaterialKeys.values()) {
            List<String> regexList = new ArrayList<>();
            if (fileConfiguration.contains(key.path)) {
                regexList.addAll(fileConfiguration.getStringList(key.path));
            } else {
                Underilla.warning("Key " + key + " not found in config");
                regexList.addAll(key.defaultValue);
            }
            Set<Material> materialSet = Arrays.stream(Material.values())
                    .filter(material -> regexList.stream().anyMatch(s -> material.toString().matches(s))).collect(Collectors.toSet());
            listMaterialMap.put(key, materialSet);
        }

        initListBiomeStringMap(fileConfiguration);

        Underilla.info("Config reloaded with values: " + this);
    }

    private void initListBiomeStringMap(FileConfiguration fileConfiguration) {
        Set<String> allBiomes = NMSBiomeUtils.getAllBiomes().keySet();
        listBiomeStringMap.clear();
        for (SetBiomeStringKeys key : SetBiomeStringKeys.values()) {
            List<String> biomes = new ArrayList<>();
            if (fileConfiguration.contains(key.path)) {
                biomes.addAll(fileConfiguration.getStringList(key.path));
            } else {
                Underilla.warning("Key " + key + " not found in config");
                biomes.addAll(key.defaultValue);
            }
            biomes = NMSBiomeUtils.normalizeBiomeNameList(biomes);
            Set<String> existingBiomes = new HashSet<>();
            for (String biome : biomes) {
                if (allBiomes.contains(biome)) {
                    existingBiomes.add(biome);
                } else {
                    Underilla.warning("Biome " + biome + " not found in the biome list of the server.");
                }
            }
            listBiomeStringMap.put(key, existingBiomes);
        }

        // Special rules for ...OnlyOn & ...ExceptOn
        if (getBoolean(BooleanKeys.CARVERS_ENABLED)) {
            mergeOnlyOnAndExceptOn(SetBiomeStringKeys.APPLY_CARVERS_ONLY_ON_BIOMES, SetBiomeStringKeys.APPLY_CARVERS_EXCEPT_ON_BIOMES,
                    allBiomes);
            // Do not apply carvers on the biomes that should have surface world only.
            listBiomeStringMap.get(SetBiomeStringKeys.APPLY_CARVERS_ONLY_ON_BIOMES)
                    .removeAll(listBiomeStringMap.get(SetBiomeStringKeys.SURFACE_WORLD_ONLY_ON_THIS_BIOMES));
        } else {
            listBiomeStringMap.put(SetBiomeStringKeys.APPLY_CARVERS_ONLY_ON_BIOMES, Set.of());
            listBiomeStringMap.remove(SetBiomeStringKeys.APPLY_CARVERS_EXCEPT_ON_BIOMES);
        }

        if (getBoolean(BooleanKeys.CARVERS_ENABLED) && getBoolean(BooleanKeys.PRESERVE_SURFACE_WORLD_FROM_CAVERS)) {
            mergeOnlyOnAndExceptOn(SetBiomeStringKeys.PRESERVE_SURFACE_WORLD_FROM_CAVERS_ONLY_ON_BIOMES,
                    SetBiomeStringKeys.PRESERVE_SURFACE_WORLD_FROM_CAVERS_EXCEPT_ON_BIOMES, allBiomes);
        } else {
            listBiomeStringMap.put(SetBiomeStringKeys.PRESERVE_SURFACE_WORLD_FROM_CAVERS_ONLY_ON_BIOMES, Set.of());
            listBiomeStringMap.remove(SetBiomeStringKeys.PRESERVE_SURFACE_WORLD_FROM_CAVERS_EXCEPT_ON_BIOMES);
        }
    }


    @Override
    public String toString() {
        return "UnderillaConfig{" + "booleanMap=" + booleanMap + "\nintegerMap=" + integerMap + "\nstringMap=" + stringMap
                + "\nlistStringMap=" + toString(listStringMap) + "\nlistBiomeStringMap=" + toString(listBiomeStringMap)
                + "\nlistMaterialMap=" + toString(listMaterialMap) + '}';
    }
    private String toString(Map<?, ? extends Collection<?>> map) {
        return map.entrySet().stream().map(e -> e.getKey() + " (" + e.getValue().size() + ") = " + e.getValue().stream().sorted().toList())
                .collect(Collectors.joining(",\n", "{", "}"));
    }

    // private --------------------------------------------------------------------------------------------------------

    private void mergeOnlyOnAndExceptOn(SetBiomeStringKeys onlyOnKey, SetBiomeStringKeys exceptOnKey, Collection<String> allBiomes) {
        Set<String> onlyOn = listBiomeStringMap.get(onlyOnKey);
        Set<String> exceptOn = listBiomeStringMap.get(exceptOnKey);
        boolean onlyOnEmpty = onlyOn.isEmpty();
        boolean exceptOnEmpty = exceptOn.isEmpty();

        if (!onlyOnEmpty && !exceptOnEmpty) {
            Underilla.warning("Both " + onlyOnKey + " and " + exceptOnKey + " are set. Ignoring " + exceptOnKey + ".");
        } else if (onlyOnEmpty && exceptOnEmpty) {
            listBiomeStringMap.put(onlyOnKey, new HashSet<>(allBiomes));
        } else if (onlyOnEmpty) {
            listBiomeStringMap.put(onlyOnKey, allBiomes.stream().filter(biome -> !exceptOn.contains(biome)).collect(Collectors.toSet()));
        }

        listBiomeStringMap.remove(exceptOnKey);
    }


    // enum keys ------------------------------------------------------------------------------------------------------
    public enum BooleanKeys {
        // @formatter:off
        DEBUG("debug", false),
        TRANSFER_BIOMES("transfer_biomes", false),
        TRANSFER_BLOCKS_FROM_CAVES_WORLD("transfer_blocks_from_caves_world", false),
        TRANSFER_BIOMES_FROM_CAVES_WORLD("transfer_biomes_from_caves_world", false),
        CUSTOM_BIOME_ENABLED("custom_biome_enabled", false),
        VANILLA_POPULATION("vanilla_population", true),
        STRUCTURES_ENABLED("structures.enabled", true),
        CARVERS_ENABLED("carvers.enabled", true),
        PRESERVE_SURFACE_WORLD_FROM_CAVERS("carvers.preserveSurfaceWorldFromCavers", true);
        // @formatter:on

        private final String path;
        private final boolean defaultValue;
        BooleanKeys(String path, boolean defaultValue) {
            this.path = path;
            this.defaultValue = defaultValue;
        }
    }
    public enum IntegerKeys {
        // @formatter:off
        SURFACE_LAYER_THICKNESS("surface.depth", 6, 0, Integer.MAX_VALUE),
        GENERATION_AREA_MIN_X("generationArea.minX", 0),
        GENERATION_AREA_MIN_Z("generationArea.minZ", 0),
        GENERATION_AREA_MAX_X("generationArea.maxX", 512),
        GENERATION_AREA_MAX_Z("generationArea.maxZ", 512);
        // @formatter:on

        private final String path;
        private final int defaultValue;
        private final int min;
        private final int max;
        IntegerKeys(String path, int defaultValue, int min, int max) {
            this.path = path;
            this.defaultValue = defaultValue;
            this.min = min;
            this.max = max;
        }
        IntegerKeys(String path, int defaultValue) { this(path, defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE); }
        IntegerKeys(String path) { this(path, 0); }
    }
    public enum StringKeys {
        // @formatter:off
        FINAL_WORLD_NAME("final_world", "world"),
        SURFACE_WORLD_NAME("reference_world", "world_surface"),
        CAVES_WORLD_NAME("caves_world", "world_caves"),
        OUT_OF_THE_SURFACE_WORLD_GENERATOR("outOfTheSurfaceWorldGenerator", "VoidWorldGenerator"),
        STRATEGY("strategy", "SURFACE");
        // @formatter:on

        private final String path;
        private final String defaultValue;
        StringKeys(String path, String defaultValue) {
            this.path = path;
            this.defaultValue = defaultValue;
        }
        StringKeys(String path) { this(path, ""); }
    }
    public enum SetStringKeys {
        // @formatter:off
        IGNORED_BLOCK_FOR_SURFACE_CALCULATION("ignored_block_for_surface_calculation");
        // @formatter:on

        private final String path;
        private final Set<String> defaultValue;
        SetStringKeys(String path, Set<String> defaultValue) {
            this.path = path;
            this.defaultValue = defaultValue;
        }
        SetStringKeys(String path) { this(path, Set.of()); }
    }
    public enum SetBiomeStringKeys {
        // @formatter:off
        TRANSFERED_CAVES_WORLD_BIOMES("transfered_caves_world_biomes", Set.of("minecraft:deep_dark", "minecraft:dripstone_caves", "minecraft:lush_caves")),
        SURFACE_WORLD_ONLY_ON_THIS_BIOMES("preserve_biomes"),
        APPLY_CARVERS_ONLY_ON_BIOMES("carvers.applyCarversOnBiomes.onlyOn"),
        APPLY_CARVERS_EXCEPT_ON_BIOMES("carvers.applyCarversOnBiomes.exceptOn"),
        PRESERVE_SURFACE_WORLD_FROM_CAVERS_ONLY_ON_BIOMES("carvers.preserveSurfaceWorldFromCaversOnBiomes.onlyOn"),
        PRESERVE_SURFACE_WORLD_FROM_CAVERS_EXCEPT_ON_BIOMES("carvers.preserveSurfaceWorldFromCaversOnBiomes.exceptOn");
        // @formatter:on

        private final String path;
        private final Set<String> defaultValue;
        SetBiomeStringKeys(String path, Set<String> defaultValue) {
            this.path = path;
            this.defaultValue = defaultValue;
        }
        SetBiomeStringKeys(String path) { this(path, Set.of()); }
    }
    public enum SetMaterialKeys {
        // @formatter:off
        IGNORED_BLOCK_FOR_SURFACE_CALCULATION("ignored_block_for_surface_calculation");
        // @formatter:on
        private final String path;
        private final Set<String> defaultValue;
        SetMaterialKeys(String path, Set<String> defaultValue) {
            this.path = path;
            this.defaultValue = defaultValue;
        }
        SetMaterialKeys(String path) { this(path, Set.of()); }
    }
}
