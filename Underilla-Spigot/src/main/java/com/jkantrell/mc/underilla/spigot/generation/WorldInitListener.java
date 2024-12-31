package com.jkantrell.mc.underilla.spigot.generation;

import java.lang.reflect.Field;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import com.jkantrell.mc.underilla.spigot.Underilla;
import com.jkantrell.mc.underilla.spigot.impl.BukkitWorldReader;
import com.jkantrell.mc.underilla.spigot.impl.CustomBiomeSource;
import com.jkantrell.mc.underilla.spigot.io.UnderillaConfig.BooleanKeys;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.status.WorldGenContext;

public class WorldInitListener implements Listener {
    private final BukkitWorldReader worldSurfaceReader;
    private final BukkitWorldReader worldCavesReader;
    private CustomBiomeSource customBiomeSource;

    public WorldInitListener(BukkitWorldReader worldSurfaceReader, BukkitWorldReader worldCavesReader) {
        this.worldSurfaceReader = worldSurfaceReader;
        this.worldCavesReader = worldCavesReader;
    }

    public CustomBiomeSource getCustomBiomeSource() { return customBiomeSource; }

    @EventHandler
    public void onWorldInit(WorldInitEvent event) {
        if (!Underilla.getUnderillaConfig().getBoolean(BooleanKeys.CUSTOM_BIOME_ENABLED)) {
            Underilla.getInstance().getLogger()
                    .info("Custom biome is disabled, no need to take over the world: " + event.getWorld().getName());
            return;
        }

        Underilla.getInstance().getLogger()
                .info("Preparing to take over the world: " + event.getWorld().getName() + " to use custom biome source");
        CraftWorld craftWorld = (CraftWorld) event.getWorld();
        ServerLevel serverLevel = craftWorld.getHandle();

        // ConfigPack pack = bukkitChunkGeneratorWrapper.getPack();

        ChunkGenerator vanilla = serverLevel.getChunkSource().getGenerator();
        BiomeSource vanillaBiomeSource = vanilla.getBiomeSource();
        customBiomeSource = new CustomBiomeSource(vanillaBiomeSource, worldSurfaceReader, worldCavesReader);
        // ChunkGenerator underillaChunkGenerator = new NMSExtendedChunkGenerator(vanilla, customBiomeSource);

        // Before 1.21 this was working.
        // serverLevel.getChunkSource().chunkMap.generator = new NMSExtendedChunkGenerator(vanilla, customBiomeSource);

        // After 1.21 we need to use reflection to set the custom biome source.
        // Next steps is based on :
        // https://github.com/VolmitSoftware/Iris/blob/master/nms/v1_21_R1/src/main/java/com/volmit/iris/core/nms/v1_21_R1/NMSBinding.java#L491
        try {
            // Edit biome source that will be generated at generation chunk step.
            ChunkMap chunkMap = serverLevel.getChunkSource().chunkMap;
            Field worldGenContextField = getField(chunkMap.getClass(), WorldGenContext.class);
            worldGenContextField.setAccessible(true);
            WorldGenContext worldGenContext = (WorldGenContext) worldGenContextField.get(chunkMap);
            Class<?> clazz = worldGenContext.generator().getClass();
            Field biomeSourceField = getField(clazz, BiomeSource.class);
            biomeSourceField.setAccessible(true);
            sun.misc.Unsafe unsafe = getUnsafe();
            unsafe.putObject(biomeSourceField.get(worldGenContext.generator()), unsafe.objectFieldOffset(biomeSourceField),
                    customBiomeSource);
            biomeSourceField.set(worldGenContext.generator(), customBiomeSource);

            // // Edit biome source that is used by StructureCheck (& StructureManager.structureCheck).
            // Field structureCheckField = getField(serverLevel.getClass(), StructureCheck.class);
            // // set public
            // structureCheckField.setAccessible(true);
            // StructureCheck structureCheck = (StructureCheck) structureCheckField.get(serverLevel);
            // Field biomeSourceStructureCheck = getField(structureCheck.getClass(), BiomeSource.class);
            // // public
            // biomeSourceStructureCheck.setAccessible(true);

            // // Edit value even if it's final.
            // unsafe = getUnsafe();
            // unsafe.putObject(biomeSourceStructureCheck.get(structureCheck), unsafe.objectFieldOffset(biomeSourceStructureCheck),
            // customBiomeSource);
            // biomeSourceStructureCheck.set(structureCheck, customBiomeSource);

            // Field structureManagerField = getField(serverLevel.getClass(), net.minecraft.world.level.StructureManager.class);
            // structureManagerField.setAccessible(true);
            // Field structureCheckInStructureManager = getField(structureManagerField.getType(), StructureCheck.class);
            // structureCheckInStructureManager.setAccessible(true);
            // unsafe = getUnsafe();
            // unsafe.putObject(structureCheckInStructureManager.get(structureManagerField.get(serverLevel)),
            // unsafe.objectFieldOffset(structureCheckInStructureManager), structureCheck);
            // structureCheckInStructureManager.set(structureManagerField.get(serverLevel), structureCheck);


        } catch (Exception e) {
            Underilla.getInstance().getLogger().warning("Failed to set custom biome source");
            e.printStackTrace();
        }


    }

    // Helper method to get the Unsafe instance
    private static sun.misc.Unsafe getUnsafe() throws Exception {
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (sun.misc.Unsafe) unsafeField.get(null);
    }

    private static Field getField(Class<?> clazz, Class<?> fieldType) throws NoSuchFieldException {
        try {
            for (Field f : clazz.getDeclaredFields()) {
                if (f.getType().equals(fieldType))
                    return f;
            }
            throw new NoSuchFieldException(fieldType.getName());
        } catch (NoSuchFieldException var4) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass == null) {
                throw var4;
            } else {
                return getField(superClass, fieldType);
            }
        }
    }

}
