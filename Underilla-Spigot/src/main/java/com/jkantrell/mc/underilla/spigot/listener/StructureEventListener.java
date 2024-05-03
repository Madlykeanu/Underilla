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
        if (this.blackList.contains(e.getStructure())) {
            e.setCancelled(true);
        }
    }

}
