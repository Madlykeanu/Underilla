package com.jkantrell.mc.underilla.spigot.cleaning;

import java.util.EnumMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;
import com.jkantrell.mc.underilla.spigot.Underilla;
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
                // TODO: Implement this
                Underilla.info("Cleaning entities task " + taskID + " finished in " + (System.currentTimeMillis() - startTime) + "ms");
                cancel();
            }

        }.runTaskTimer(Underilla.getInstance(), 0, 1);
    }


}
