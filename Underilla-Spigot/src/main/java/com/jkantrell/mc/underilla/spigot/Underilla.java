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
import com.jkantrell.mc.underilla.spigot.selector.Selector;

public final class Underilla extends JavaPlugin {

    private UnderillaConfig underillaConfig;
    private BukkitWorldReader worldSurfaceReader;
    private @Nullable BukkitWorldReader worldCavesReader;
    public static final int CHUNK_SIZE = 16;
    public static final int REGION_SIZE = 512;
    public static final int BIOME_AREA_SIZE = 4;
    private CleanBlocksTask cleanBlocksTask;
    private CleanEntitiesTask cleanEntitiesTask;


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

        runStepsOnEnabled();
    }

    @Override
    public void onDisable() {
        try {
            stopTasks();
            if (Generator.times != null) {
                long totalTime = Generator.times.entrySet().stream().mapToLong(Map.Entry::getValue).sum();
                for (Map.Entry<String, Long> entry : Generator.times.entrySet()) {
                    getLogger().info(entry.getKey() + " took " + entry.getValue() + "ms (" + (entry.getValue() * 100 / totalTime) + "%)");
                }
            }
            Map<String, Long> biomesPlaced = UnderillaChunkGenerator.getBiomesPlaced();
            if (biomesPlaced != null) {
                getLogger().info("Map of biome placed: "
                        + biomesPlaced.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                                .map(entry -> entry.getKey() + ": " + entry.getValue()).reduce((a, b) -> a + ", " + b).orElse(""));
            }
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


    private void runStepsOnEnabled() {
        // TODO If there is config steps todo:
        ServerSetup.setupPaper();
        // runChunky();
    }
    public void runNextStepsAfterWorldInit() {
        if (Underilla.getUnderillaConfig().getString(StringKeys.STEP_UNDERILLA_GENERATION).equals("todo")) {
            runChunky();
        } else if (Underilla.getUnderillaConfig().getString(StringKeys.STEP_UNDERILLA_GENERATION).equals("doing")) {
            restartChunky();
        } else if (Underilla.getUnderillaConfig().getString(StringKeys.STEP_CLEANING_BLOCKS).equals("todo")) {
            runCleanBlocks();
        } else if (Underilla.getUnderillaConfig().getString(StringKeys.STEP_CLEANING_BLOCKS).equals("doing")) {
            restartCleanBlocks();
        } else if (Underilla.getUnderillaConfig().getString(StringKeys.STEP_CLEANING_ENTITIES).equals("todo")) {
            runCleanEntities();
        } else if (Underilla.getUnderillaConfig().getString(StringKeys.STEP_CLEANING_ENTITIES).equals("doing")) {
            restartCleanEntities();
        }
    }
    public void validateTask(StringKeys taskKey, boolean done) {
        getUnderillaConfig().saveNewValue(taskKey, done ? "done" : "failed");
        runNextStepsAfterWorldInit();
    }
    public void validateTask(StringKeys taskKey) { validateTask(taskKey, true); }
    public void setToDoingTask(StringKeys taskKey) { getUnderillaConfig().saveNewValue(taskKey, "doing"); }

    // run tasks ------------------------------------------------------------------------------------------------------
    private void runChunky(boolean restart) {
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
            validateTask(StringKeys.STEP_UNDERILLA_GENERATION);
        });

        boolean worked;
        if (restart) {
            worked = chunky.getApi().continueTask(worldName);
        } else {
            worked = chunky.getApi().startTask(worldName, "rectangle", centerX, centerZ, radiusX, radiusZ, "region");
            setToDoingTask(StringKeys.STEP_UNDERILLA_GENERATION);
        }
        if (worked) {
            info("Started Chunky task for world " + worldName);
        } else {
            warning("Failed to start Chunky task for world " + worldName);
            validateTask(StringKeys.STEP_UNDERILLA_GENERATION, false);
        }
    }
    private void runChunky() { runChunky(false); }
    private void runCleanBlocks(Selector selector) {
        setToDoingTask(StringKeys.STEP_CLEANING_BLOCKS);
        info("Starting clean blocks task");
        cleanBlocksTask = new CleanBlocksTask(2, 3, selector);
        cleanBlocksTask.run();
    }
    private void runCleanBlocks() { runCleanBlocks(Underilla.getUnderillaConfig().getSelector()); }
    private void runCleanEntities(Selector selector) {
        setToDoingTask(StringKeys.STEP_CLEANING_ENTITIES);
        info("Starting clean entities task");
        cleanEntitiesTask = new CleanEntitiesTask(3, 3);
        cleanEntitiesTask.run();
    }
    private void runCleanEntities() { runCleanEntities(Underilla.getUnderillaConfig().getSelector()); }

    // stop tasks -----------------------------------------------------------------------------------------------------
    private void stopTasks() {
        if (cleanBlocksTask != null && Underilla.getUnderillaConfig().getString(StringKeys.STEP_CLEANING_BLOCKS).equals("doing")) {
            Selector selector = cleanBlocksTask.stop();
            selector.saveIn("cleanBlocksTask");
        }
        if (cleanEntitiesTask != null && Underilla.getUnderillaConfig().getString(StringKeys.STEP_CLEANING_ENTITIES).equals("doing")) {
            Selector selector = cleanEntitiesTask.stop();
            selector.saveIn("cleanEntitiesTask");
        }
    }

    // restart tasks --------------------------------------------------------------------------------------------------
    private void restartChunky() {
        info("Restarting Chunky task");
        runChunky(true);
    }
    private void restartCleanBlocks() {
        info("Restarting clean blocks task");
        try {
            runCleanBlocks(Selector.loadFrom("cleanBlocksTask"));
        } catch (Exception e) {
            Underilla.warning("Tasks can't be restarted from last state. Restarting from the beginning.");
            runCleanBlocks();
        }
    }
    private void restartCleanEntities() {
        info("Restarting clean entities task");
        try {
            runCleanEntities(Selector.loadFrom("cleanEntitiesTask"));
        } catch (Exception e) {
            Underilla.warning("Tasks can't be restarted from last state. Restarting from the beginning.");
            runCleanEntities();
        }
    }
}
