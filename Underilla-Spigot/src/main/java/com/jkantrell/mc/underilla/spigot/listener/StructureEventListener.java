package com.jkantrell.mc.underilla.spigot.listener;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.AsyncStructureSpawnEvent;
import com.jkantrell.mc.underilla.spigot.Underilla;
import com.jkantrell.mc.underilla.spigot.io.UnderillaConfig.SetStructureKeys;

public class StructureEventListener implements Listener {
    private final Map<String, Integer> structureCount;

    public StructureEventListener() { structureCount = new HashMap<>(); }

    public Map<String, Integer> getStructureCount() { return structureCount; }

    @EventHandler(ignoreCancelled = true)
    public void onStructureSpawn(AsyncStructureSpawnEvent e) {
        // If in the list of structure to keep then log & count else cancel the event.
        if (Underilla.getUnderillaConfig().isStructureInSet(SetStructureKeys.SURUCTURE_ONLY, e.getStructure())) {
            // Cast double to int for Y coordinate
            int structureY = (int) e.getBoundingBox().getMinY();
            
            // Convert chunk coordinates to block coordinates (multiply by 16)
            int blockX = e.getChunkX() * 16;
            int blockZ = e.getChunkZ() * 16;
            
            Underilla.debug(() -> e.getStructure().getKey().asString() + 
                " spawned at block: " + blockX + " " + structureY + " " + blockZ + 
                " (chunk: " + e.getChunkX() + " " + e.getChunkZ() + ") in biome " +
                e.getWorld().getBiome(blockX, structureY, blockZ));
                
            String structureName = e.getStructure().getKey().asString();
            structureCount.put(structureName, structureCount.getOrDefault(structureName, 0) + 1);
        } else {
            e.setCancelled(true);
        }
    }

}
