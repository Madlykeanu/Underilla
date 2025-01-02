package com.jkantrell.mc.underilla.spigot.cleaning;

import java.util.EnumMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;
import com.jkantrell.mc.underilla.spigot.Underilla;
import com.jkantrell.mc.underilla.spigot.io.UnderillaConfig.SetEntityTypeKeys;
import com.jkantrell.mc.underilla.spigot.io.UnderillaConfig.StringKeys;
import net.minecraft.world.level.LevelReader;

public class CleanEntitiesTask extends FollowableProgressTask {
    private LevelReader levelReader;
    public CleanEntitiesTask(int taskID, int tasksCount) {
        super(taskID, tasksCount);
        levelReader = ((CraftWorld) Bukkit.getWorld(selector.getWorldUUID())).getHandle();
    }

    public void run() {
        final long startTime = System.currentTimeMillis();
        final Map<EntityType, Long> removedEntity = new EnumMap<>(EntityType.class);
        final Map<EntityType, Long> finalEntity = new EnumMap<>(EntityType.class);
        new BukkitRunnable() {
            private long processedBlocks = 0;
            @Override
            public void run() {
                long execTime = System.currentTimeMillis();
                if (selector == null || selector.progress() >= 1) {
                    printProgress(processedBlocks, startTime);
                    Underilla.info("Cleaning entities task " + taskID + " finished in " + (System.currentTimeMillis() - startTime) + "ms");
                    Underilla.info("Removed entities: " + removedEntity);
                    Underilla.info("Final entities: " + finalEntity);
                    cancel();
                    Underilla.getInstance().validateTask(StringKeys.STEP_CLEANING_ENTITIES);
                    return;
                }

                while (execTime + 45 > System.currentTimeMillis() && selector.hasNextBlock()) {
                    Chunk currentChunk = selector.nextChunk();
                    for (Entity entity : currentChunk.getEntities()) {
                        if (Underilla.getUnderillaConfig().isEntityTypeInSet(SetEntityTypeKeys.CLEAN_ENTITY_TO_REMOVE, entity.getType())) {
                            entity.remove();
                            removedEntity.put(entity.getType(), removedEntity.getOrDefault(entity.getType(), 0l) + 1);
                        } else {
                            finalEntity.put(entity.getType(), finalEntity.getOrDefault(entity.getType(), 0l) + 1);
                        }
                    }
                }

            }

        }.runTaskTimer(Underilla.getInstance(), 0, 1);
    }


}
