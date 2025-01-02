package com.jkantrell.mc.underilla.spigot;

import fr.formiko.mc.voidworldgenerator.VoidWorldGeneratorPlugin;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.popcraft.chunky.Chunky;
import org.popcraft.chunky.ChunkyProvider;
import org.popcraft.chunky.api.event.task.GenerationProgressEvent;
import com.jkantrell.mc.underilla.core.generation.Generator;
import com.jkantrell.mc.underilla.spigot.cleaning.CleanBlocksTask;
import com.jkantrell.mc.underilla.spigot.cleaning.CleanEntitiesTask;
import com.jkantrell.mc.underilla.spigot.cleaning.FollowableProgressTask;
import com.jkantrell.mc.underilla.spigot.generation.UnderillaChunkGenerator;
import com.jkantrell.mc.underilla.spigot.impl.BukkitWorldReader;
import com.jkantrell.mc.underilla.spigot.io.UnderillaConfig;
import com.jkantrell.mc.underilla.spigot.io.UnderillaConfig.BooleanKeys;
import com.jkantrell.mc.underilla.spigot.io.UnderillaConfig.IntegerKeys;
import com.jkantrell.mc.underilla.spigot.io.UnderillaConfig.StringKeys;
import com.jkantrell.mc.underilla.spigot.listener.StructureEventListener;
import com.jkantrell.mc.underilla.spigot.listener.WorldListener;
import com.jkantrell.mc.underilla.spigot.preparing.ServerSetup;

public final class Underilla extends JavaPlugin {

    private UnderillaConfig underillaConfig;
    private BukkitWorldReader worldSurfaceReader;
    private @Nullable BukkitWorldReader worldCavesReader;
    public static final int CHUNK_SIZE = 16;
    public static final int REGION_SIZE = 512;
    public static final int BIOME_AREA_SIZE = 4;


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

        // Loading reference world
        try {
            this.worldSurfaceReader = new BukkitWorldReader(Underilla.getUnderillaConfig().getString(StringKeys.SURFACE_WORLD_NAME));
            getLogger().info("World '" + Underilla.getUnderillaConfig().getString(StringKeys.SURFACE_WORLD_NAME) + "' found.");
        } catch (NoSuchFieldException e) {
            getLogger()
                    .warning("No world with name '" + Underilla.getUnderillaConfig().getString(StringKeys.SURFACE_WORLD_NAME) + "' found");
            e.printStackTrace();
        }
        // Loading caves world if we should use it.
        if (Underilla.getUnderillaConfig().getBoolean(BooleanKeys.TRANSFER_BLOCKS_FROM_CAVES_WORLD)
                || Underilla.getUnderillaConfig().getBoolean(BooleanKeys.TRANSFER_BIOMES_FROM_CAVES_WORLD)) {
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
        if (Underilla.getUnderillaConfig().getBoolean(BooleanKeys.STRUCTURES_ENABLED)) {
            this.getServer().getPluginManager().registerEvents(new StructureEventListener(), this);
        }
        this.getServer().getPluginManager().registerEvents(new WorldListener(), this);

        runSteps();
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
            // Map<String, Long> biomesPlaced = worldInitListener != null ? worldInitListener.getCustomBiomeSource().getBiomesPlaced()
            // : UnderillaChunkGenerator.getBiomesPlaced();
            Map<String, Long> biomesPlaced = UnderillaChunkGenerator.getBiomesPlaced();
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


    private void runSteps() {
        // TODO If there is config steps todo:
        ServerSetup.setupPaper();
        // runChunky();
    }
    public void runChunky() {
        Chunky chunky = ChunkyProvider.get();
        // startTask(String world, String shape, double centerX, double centerZ, double radiusX, double radiusZ, String pattern)
        String worldName = Underilla.getUnderillaConfig().getString(StringKeys.FINAL_WORLD_NAME);
        int minX = Underilla.getUnderillaConfig().getInt(IntegerKeys.GENERATION_AREA_MIN_X);
        int minZ = Underilla.getUnderillaConfig().getInt(IntegerKeys.GENERATION_AREA_MIN_Z);
        int maxX = Underilla.getUnderillaConfig().getInt(IntegerKeys.GENERATION_AREA_MAX_X);
        int maxZ = Underilla.getUnderillaConfig().getInt(IntegerKeys.GENERATION_AREA_MAX_Z);
        int centerX = (minX + maxX) / 2;
        int centerZ = (minZ + maxZ) / 2;
        int radiusX = (maxX - minX) / 2;
        int radiusZ = (maxZ - minZ) / 2;
        final long startTime = System.currentTimeMillis();
        // Set chunky silent
        chunky.getConfig().setSilent(true);

        chunky.getApi().onGenerationProgress(new Consumer<GenerationProgressEvent>() {
            long printTime = 0;
            long printTimeEachXMs = 1000 * Underilla.getUnderillaConfig().getInt(IntegerKeys.PRINT_PROGRESS_EVERY_X_SECONDS);
            @Override
            public void accept(GenerationProgressEvent generationProgressEvent) {
                if (printTime + printTimeEachXMs < System.currentTimeMillis()) {
                    printTime = System.currentTimeMillis();
                    FollowableProgressTask.printProgress(generationProgressEvent.chunks(), startTime,
                            generationProgressEvent.progress() / 100, 1, 3, "Rate: " + (int) (generationProgressEvent.rate())
                                    + ", Current: " + generationProgressEvent.x() + " " + generationProgressEvent.z());
                }
            }
        });

        chunky.getApi().onGenerationComplete(generationCompleteEvent -> {
            info("Chunky task for world " + worldName + " has finished");
            if (Underilla.getUnderillaConfig().getBoolean(BooleanKeys.CLEAN_BLOCKS_ENABLED)) {
                runCleanBlocks();
            } else if (Underilla.getUnderillaConfig().getBoolean(BooleanKeys.CLEAN_ENTITIES_ENABLED)) {
                runCleanEntities();
            }
        });

        boolean worked = chunky.getApi().startTask(worldName, "rectangle", centerX, centerZ, radiusX, radiusZ, "region");
        if (worked) {
            info("Started Chunky task for world " + worldName);
        } else {
            warning("Failed to start Chunky task for world " + worldName);
        }


    }
    public void runCleanBlocks() {
        info("Starting clean blocks task");
        CleanBlocksTask cleanBlocksTask = new CleanBlocksTask(2, 3);
        cleanBlocksTask.run();
    }
    public void runCleanEntities() {
        info("Starting clean entities task");
        CleanEntitiesTask cleanBlocksTask = new CleanEntitiesTask(3, 3);
        cleanBlocksTask.run();
    }
}
