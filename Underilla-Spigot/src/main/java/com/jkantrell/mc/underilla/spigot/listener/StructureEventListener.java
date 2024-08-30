package com.jkantrell.mc.underilla.spigot.listener;

import java.util.List;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.AsyncStructureSpawnEvent;
import org.bukkit.generator.structure.Structure;

public class StructureEventListener implements Listener {

    private List<Structure> blackList;

    public StructureEventListener(List<Structure> blackList) { this.blackList = blackList; }

    @EventHandler
    public void onStructureSpawn(AsyncStructureSpawnEvent e) {
        // Location location = e.getWorld().getBlockAt(e.getChunkX() * 16, 0, e.getChunkZ() * 16).getLocation();
        // String biomeKey = NMSBiomeUtils.getBiomeKey(location).toString();
        // Underilla.getInstance().getLogger().info("Structure spawned ? " + !this.blackList.contains(e.getStructure()) + " : "
        //         + e.getStructure().getStructureType().getKey().toString() + " on biome " + biomeKey + " at location " + location);
        if (this.blackList.contains(e.getStructure())) {
            e.setCancelled(true);
        }
    }

}
