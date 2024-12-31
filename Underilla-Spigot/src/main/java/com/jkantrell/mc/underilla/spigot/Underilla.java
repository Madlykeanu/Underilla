package com.jkantrell.mc.underilla.spigot;

import fr.formiko.mc.biomeutils.NMSBiomeUtils;
import fr.formiko.mc.voidworldgenerator.VoidWorldGeneratorPlugin;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import com.jkantrell.mc.underilla.core.generation.Generator;
import com.jkantrell.mc.underilla.spigot.generation.UnderillaChunkGenerator;
import com.jkantrell.mc.underilla.spigot.impl.BukkitWorldReader;
import com.jkantrell.mc.underilla.spigot.io.Config;
import com.jkantrell.mc.underilla.spigot.io.UnderillaConfig;
import com.jkantrell.mc.underilla.spigot.io.UnderillaConfig.StringKeys;
import com.jkantrell.mc.underilla.spigot.listener.StructureEventListener;

public final class Underilla extends JavaPlugin {

    public static final Config CONFIG = new Config("");
    private UnderillaConfig underillaConfig;
    private BukkitWorldReader worldSurfaceReader;
    private @Nullable BukkitWorldReader worldCavesReader;
    private com.jkantrell.mc.underilla.spigot.generation.WorldInitListener worldInitListener;
    public static final int CHUNK_SIZE = 16;


    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        if (this.worldSurfaceReader == null) {
            getLogger()
                    .warning("No world with name '" + Underilla.getUnderillaConfig().getString(StringKeys.SURFACE_WORLD_NAME) + "' found");
            return super.getDefaultWorldGenerator(worldName, id);
        }
        String outOfTheSurfaceWorldGeneratorName = Underilla.getUnderillaConfig().getString(StringKeys.OUT_OF_THE_SURFACE_WORLD_GENERATOR);
        ChunkGenerator outOfTheSurfaceWorldGenerator;
        if (outOfTheSurfaceWorldGeneratorName == null || "VANILLA".equals(outOfTheSurfaceWorldGeneratorName)) {
            outOfTheSurfaceWorldGenerator = null;
        } else if ("VoidWorldGenerator".equals(outOfTheSurfaceWorldGeneratorName)) {
            outOfTheSurfaceWorldGenerator = getProvidingPlugin(VoidWorldGeneratorPlugin.class).getDefaultWorldGenerator(worldName, id);
        } else {
            outOfTheSurfaceWorldGenerator = null;
        }
        getLogger().info(
                "Using Underilla as main world generator (with " + outOfTheSurfaceWorldGenerator + " as outOfTheSurfaceWorldGenerator)!");
        return new UnderillaChunkGenerator(this.worldSurfaceReader, this.worldCavesReader, outOfTheSurfaceWorldGenerator);
    }

    @Override
    public void onEnable() {
        // save default config
        this.saveDefaultConfig();
        reloadConfig();

        // Loading config
        Underilla.CONFIG.setFilePath(this.getDataFolder() + File.separator + "config.yml");
        try {
            Underilla.CONFIG.load();
            Underilla.CONFIG.transferCavesWorldBiomes = NMSBiomeUtils.normalizeBiomeNameList(Underilla.CONFIG.transferCavesWorldBiomes);
            Underilla.CONFIG.preserveBiomes = NMSBiomeUtils.normalizeBiomeNameList(Underilla.CONFIG.preserveBiomes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Loading reference world
        try {
            this.worldSurfaceReader = new BukkitWorldReader(Underilla.getUnderillaConfig().getString(StringKeys.SURFACE_WORLD_NAME));
            getLogger().info("World + '" + Underilla.getUnderillaConfig().getString(StringKeys.SURFACE_WORLD_NAME) + "' found.");
        } catch (NoSuchFieldException e) {
            getLogger()
                    .warning("No world with name '" + Underilla.getUnderillaConfig().getString(StringKeys.SURFACE_WORLD_NAME) + "' found");
            e.printStackTrace();
        }
        // Loading caves world if we should use it.
        if (Underilla.CONFIG.transferBlocksFromCavesWorld || Underilla.CONFIG.transferBiomesFromCavesWorld) {
            try {
                getLogger().info("Loading caves world");
                this.worldCavesReader = new BukkitWorldReader(Underilla.getUnderillaConfig().getString(StringKeys.CAVES_WORLD_NAME));
            } catch (NoSuchFieldException e) {
                getLogger().warning(
                        "No world with name '" + Underilla.getUnderillaConfig().getString(StringKeys.CAVES_WORLD_NAME) + "' found");
                e.printStackTrace();
            }
        }

        // Registering listeners
        if (CONFIG.generateStructures) {
            this.getServer().getPluginManager().registerEvents(new StructureEventListener(CONFIG.structureBlackList), this);
        }
        if (CONFIG.transferBiomes && CONFIG.customBiomeEnabled) {
            worldInitListener = new com.jkantrell.mc.underilla.spigot.generation.WorldInitListener(worldSurfaceReader, worldCavesReader);
            this.getServer().getPluginManager().registerEvents(worldInitListener, this);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (Generator.times != null) {
                long totalTime = Generator.times.entrySet().stream().mapToLong(Map.Entry::getValue).sum();
                for (Map.Entry<String, Long> entry : Generator.times.entrySet()) {
                    getLogger().info(entry.getKey() + " took " + entry.getValue() + "ms (" + (entry.getValue() * 100 / totalTime) + "%)");
                }
            }
            Map<String, Long> biomesPlaced = worldInitListener != null ? worldInitListener.getCustomBiomeSource().getBiomesPlaced()
                    : UnderillaChunkGenerator.getBiomesPlaced();
            getLogger().info("Map of chunks: " + biomesPlaced.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .map(entry -> entry.getKey() + ": " + entry.getValue()).reduce((a, b) -> a + ", " + b).orElse(""));
        } catch (Exception e) {
            getLogger().info("Fail to print times or biomes placed.");
            e.printStackTrace();
        }
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        if (underillaConfig == null) {
            underillaConfig = new UnderillaConfig(getConfig());
        } else {
            underillaConfig.reload(getConfig());
        }
    }

    public static Underilla getInstance() { return getPlugin(Underilla.class); }
    public static UnderillaConfig getUnderillaConfig() { return getInstance().underillaConfig; }


    public static void log(Level level, String message) { getInstance().getLogger().log(level, message); }
    public static void log(Level level, String message, Throwable e) { getInstance().getLogger().log(level, message, e); }
    public static void debug(String message) {
        if (getInstance().getConfig().getBoolean("debug", false)) {
            log(Level.INFO, message);
        }
    }
    public static void debug(Supplier<String> messageProvider) {
        if (getInstance().getConfig().getBoolean("debug", false)) {
            log(Level.INFO, messageProvider.get());
        }
    }
    public static void info(String message) { log(Level.INFO, message); }
    public static void info(String message, Throwable e) { log(Level.INFO, message, e); }
    public static void warning(String message) { log(Level.WARNING, message); }
    public static void warning(String message, Throwable e) { log(Level.WARNING, message, e); }
    public static void error(String message) { log(Level.SEVERE, message); }
    public static void error(String message, Throwable e) { log(Level.SEVERE, message, e); }
}
