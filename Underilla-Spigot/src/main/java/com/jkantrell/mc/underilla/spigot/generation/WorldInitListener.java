package com.jkantrell.mc.underilla.spigot.generation;

import java.lang.reflect.Field;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import com.jkantrell.mc.underilla.spigot.Underilla;
import com.jkantrell.mc.underilla.spigot.impl.BukkitWorldReader;
import com.jkantrell.mc.underilla.spigot.impl.CustomBiomeSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import sun.misc.Unsafe;

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
        Underilla.getInstance().getLogger().info("Preparing to take over the world: " + event.getWorld().getName());
        CraftWorld craftWorld = (CraftWorld) event.getWorld();
        ServerLevel serverLevel = craftWorld.getHandle();

        // ConfigPack pack = bukkitChunkGeneratorWrapper.getPack();

        ChunkGenerator vanilla = serverLevel.getChunkSource().getGenerator();
        BiomeSource vanillaBiomeSource = vanilla.getBiomeSource();
        customBiomeSource = new CustomBiomeSource(vanillaBiomeSource, worldSurfaceReader, worldCavesReader);

        // Before 1.21 this was working.
        // serverLevel.getChunkSource().chunkMap.generator = new NMSExtendedChunkGenerator(vanilla, customBiomeSource);

        // After 1.21 we need to use reflection to set the custom biome source.
        // Next steps is based on :
        // https://github.com/VolmitSoftware/Iris/blob/master/nms/v1_21_R1/src/main/java/com/volmit/iris/core/nms/v1_21_R1/NMSBinding.java#L491
        try {
            var chunkMap = serverLevel.getChunkSource().chunkMap;
            var worldGenContextField = getField(chunkMap.getClass(), WorldGenContext.class);
            worldGenContextField.setAccessible(true);
            var worldGenContext = (WorldGenContext) worldGenContextField.get(chunkMap);
            Class<?> clazz = worldGenContext.generator().getClass();
            Field biomeSource = getField(clazz, BiomeSource.class);
            biomeSource.setAccessible(true);
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);
            unsafe.putObject(biomeSource.get(worldGenContext.generator()), unsafe.objectFieldOffset(biomeSource), customBiomeSource);
            biomeSource.set(worldGenContext.generator(), customBiomeSource);
        } catch (Exception e) {
            Underilla.getInstance().getLogger().warning("Failed to set custom biome source");
            e.printStackTrace();
        }
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
