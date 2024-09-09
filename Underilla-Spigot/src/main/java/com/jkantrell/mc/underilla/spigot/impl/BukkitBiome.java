package com.jkantrell.mc.underilla.spigot.impl;

import com.jkantrell.mc.underilla.core.api.Biome;
import net.minecraft.core.Holder;

public class BukkitBiome implements Biome {

    public static final BukkitBiome DEFAULT = new BukkitBiome("minecraft:plains");

    // FIELDS
    private String name;


    // // CONSTRUCTORS
    public BukkitBiome(String name) { this.name = NMSBiomeUtils.normalizeBiomeName(name); }


    // GETTERS
    public org.bukkit.block.Biome getBiome() { return NMSBiomeUtils.getBukkitBiome(this.name); }
    public net.minecraft.world.level.biome.Biome getBiomeNMS() { return NMSBiomeUtils.getBiome(this.name); }
    public Holder<net.minecraft.world.level.biome.Biome> getBiomeHolder() {
        return NMSBiomeUtils.getBiomeRegistry().wrapAsHolder(getBiomeNMS());
    }


    // IMPLEMENTATIONS
    @Override
    public String getName() { return name; }
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        if (!(o instanceof BukkitBiome bukkitBiome)) {
            return false;
        }
        return this.name.equals(bukkitBiome.name);
    }

    @Override
    public String toString() { return "BukkitBiome{" + name + '}'; }
}
