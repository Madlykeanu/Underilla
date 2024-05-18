package com.jkantrell.mc.underilla.spigot.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Material;
import com.jkantrell.mc.underilla.core.api.Biome;
import com.jkantrell.mc.underilla.core.api.Block;
import com.jkantrell.mc.underilla.core.reader.ChunkReader;
import com.jkantrell.mc.underilla.core.reader.TagInterpreter;
import com.jkantrell.mca.Chunk;
import com.jkantrell.nbt.tag.CompoundTag;
import com.jkantrell.nbt.tag.StringTag;

public class BukkitChunkReader extends ChunkReader {
    private Map<String, org.bukkit.block.Biome> customBiomes = new HashMap<>();

    // CONSTRUCTORS
    public BukkitChunkReader(Chunk chunk) { super(chunk); }


    // IMPLEMENTATION
    @Override
    public Optional<Block> blockFromTag(CompoundTag tag) {
        Material m = Optional.ofNullable(tag).map(t -> t.getString("Name")).map(Material::matchMaterial).orElse(null);
        if (m == null) {
            return Optional.empty();
        }

        CompoundTag properties = tag != null ? tag.getCompoundTag("Properties") : null;
        BukkitBlock block;
        if (properties == null) {
            block = new BukkitBlock(m.createBlockData());
            return Optional.of(block);
        }

        // IllegalArgumentException might be thrown if block data is not compatible with current version of Minecraft
        // In such case, return plain block with no data
        try {
            String dataString = TagInterpreter.COMPOUND.interpretBlockDataString(properties);
            block = new BukkitBlock(m.createBlockData(dataString));
        } catch (IllegalArgumentException e) {
            block = new BukkitBlock(m.createBlockData());
        }

        return Optional.of(block);
    }
    @Override
    public Optional<Biome> biomeFromTag(StringTag tag) {
        return Optional.of(new BukkitBiome(tag.getValue()));
        // String[] raw = tag.getValue().split(":");
        // String name = raw.length > 1 ? raw[1] : raw[0];
        // if (customBiomes.containsKey(name)) {
        // Biome biome = new BukkitBiome(tag.getValue());
        // return Optional.of(biome);
        // }
        // try {
        // org.bukkit.block.Biome nativeBiome = org.bukkit.block.Biome.valueOf(name.toUpperCase());
        // Biome biome = new BukkitBiome(nativeBiome);
        // return Optional.of(biome);
        // } catch (IllegalArgumentException e) {
        // Bukkit.getLogger().warning("Could not resolve biome '" + name + "' try to create a custom biome from it");
        // org.bukkit.block.Biome customBiome = customBiomes.computeIfAbsent(name, b -> org.bukkit.block.Biome.PLAINS);
        // // CustomBiome customBiome = customBiomes.computeIfAbsent(name, CustomBiome::new);
        // Biome biome = new BukkitBiome(customBiome);
        // return Optional.of(biome);
        // }
    }
    // public class CustomBiome implements Biome {
    // private final String name;
    // public CustomBiome(String name) { this.name = name; }
    // @Override
    // public String getName() { return name; }
    // }
}
