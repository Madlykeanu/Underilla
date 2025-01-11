package com.jkantrell.mc.underilla.spigot.listener;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.AsyncStructureSpawnEvent;
import com.jkantrell.mc.underilla.spigot.Underilla;
import com.jkantrell.mc.underilla.spigot.io.UnderillaConfig.SetStructureKeys;
import com.jkantrell.mc.underilla.spigot.io.UnderillaConfig.StringKeys;
import com.jkantrell.mc.underilla.core.api.HeightMapType;
import com.jkantrell.mc.underilla.core.generation.Generator;
import com.jkantrell.mc.underilla.spigot.impl.BukkitWorldInfo;
import com.jkantrell.mc.underilla.spigot.impl.BukkitWorldReader;

public class StructureEventListener implements Listener {
    private final Map<String, Integer> structureCount;
    private Generator generator;

    public StructureEventListener() { 
        structureCount = new HashMap<>();
        // Create a new generator with the surface world reader
        try {
            BukkitWorldReader worldSurfaceReader = new BukkitWorldReader(Underilla.getUnderillaConfig().getString(StringKeys.SURFACE_WORLD_NAME));
            this.generator = new Generator(worldSurfaceReader);
        } catch (NoSuchFieldException e) {
            Underilla.warning("Failed to initialize generator for structure height checking");
        }
    }

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
            
            String structureName = e.getStructure().getKey().toString();
            
            // For surface structures, check if they're floating
            if (structureName.contains("village") || 
                structureName.contains("outpost") || 
                structureName.contains("ruined_portal") ||
                structureName.contains("mansion") ||
                structureName.contains("swamp_hut")) {
                if (generator != null) {
                    BukkitWorldInfo worldInfo = new BukkitWorldInfo(e.getWorld());
                    int surfaceHeight = generator.getBaseHeight(worldInfo, blockX, blockZ, HeightMapType.WORLD_SURFACE);
                    
                    // If the structure's bottom is more than 3 blocks above the surface, cancel it
                    if (structureY > surfaceHeight + 3) {
                        Underilla.debug(() -> "Cancelled floating " + structureName + 
                            " at Y=" + structureY + " (surface height: " + surfaceHeight + ")");
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            
            Underilla.debug(() -> structureName + 
                " spawned at block: " + blockX + " " + structureY + " " + blockZ + 
                " (chunk: " + e.getChunkX() + " " + e.getChunkZ() + ") in biome " +
                e.getWorld().getBiome(blockX, structureY, blockZ));
                
            structureCount.put(structureName, structureCount.getOrDefault(structureName, 0) + 1);
        } else {
            e.setCancelled(true);
        }
    }
}
