// package com.jkantrell.mc.underilla.spigot.generation;

// import org.bukkit.craftbukkit.CraftWorld;
// import org.bukkit.event.EventHandler;
// import org.bukkit.event.Listener;
// import org.bukkit.event.world.WorldInitEvent;
// import com.jkantrell.mc.underilla.spigot.Underilla;
// import net.minecraft.server.level.ServerLevel;
// import net.minecraft.world.level.chunk.ChunkGenerator;

// public class WorldInitListener implements Listener {
// @EventHandler
// public void onWorldInit(WorldInitEvent event) {
// Underilla.getInstance().getLogger().info("Preparing to take over the world: " + event.getWorld().getName());
// CraftWorld craftWorld = (CraftWorld) event.getWorld();
// ServerLevel serverWorld = craftWorld.getHandle();

// // ConfigPack pack = bukkitChunkGeneratorWrapper.getPack();

// ChunkGenerator vanilla = serverWorld.getChunkSource().getGenerator();
// // NMSBiomeProvider provider = new NMSBiomeProvider(pack.getBiomeProvider(), craftWorld.getSeed());

// serverWorld.getChunkSource().chunkMap.generator = new NMSChunkGeneratorDelegate(vanilla);
// // Underilla.getInstance().getDefaultWorldGenerator(event.getWorld().getName(), "WorldInitListenerID")
// // serverWorld.getChunkSource().chunkMap.generator
// System.out.println("vanilla generator: " + vanilla);

// Underilla.getInstance().getLogger().info("Successfully injected into world.");
// }
// }
