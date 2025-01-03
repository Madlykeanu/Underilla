package com.jkantrell.mc.underilla.spigot.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.AsyncStructureSpawnEvent;
import com.jkantrell.mc.underilla.spigot.Underilla;
import com.jkantrell.mc.underilla.spigot.io.UnderillaConfig.SetStructureKeys;

public class StructureEventListener implements Listener {

    public StructureEventListener() {}

    @EventHandler(ignoreCancelled = true)
    public void onStructureSpawn(AsyncStructureSpawnEvent e) {
        // Location location = e.getWorld().getBlockAt(e.getChunkX() * 16, 0, e.getChunkZ() * 16).getLocation();
        // String biomeKey = NMSBiomeUtils.getBiomeKey(location).toString();
        // Underilla.getInstance().getLogger().info("Structure spawned ? " + !this.blackList.contains(e.getStructure()) + " : "
        // + e.getStructure().getStructureType().getKey().toString() + " on biome " + biomeKey + " at location " + location);
        // if (this.blackList.contains(e.getStructure())) {
        // e.setCancelled(true);
        // }
        // If not in the list of structure to keep, cancel the event.
        if (!Underilla.getUnderillaConfig().isStructureInSet(SetStructureKeys.SURUCTURE_ONLY, e.getStructure())) {
            e.setCancelled(true);
        }
    }

}
