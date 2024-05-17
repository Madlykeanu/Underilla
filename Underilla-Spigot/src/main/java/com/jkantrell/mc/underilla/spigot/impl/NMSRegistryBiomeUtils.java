package com.jkantrell.mc.underilla.spigot.impl;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;

public class NMSRegistryBiomeUtils {
    public static Registry<Biome> getBiomeRegistry() {
        return ((CraftServer) Bukkit.getServer()).getServer().registryAccess().registryOrThrow(Registries.BIOME);
    }
}
