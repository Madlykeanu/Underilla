[download]: https://img.shields.io/github/downloads/HydrolienF/Underilla/total
[downloadLink]: https://hangar.papermc.io/Hydrolien/Underilla
[discord-shield]: https://img.shields.io/discord/728592434577014825?label=discord
[discord-invite]: https://discord.gg/RPNbtRSFqG


[ ![download][] ][downloadLink]
[ ![discord-shield][] ][discord-invite]

# Underilla
Underilla is a Paper based plugin for Minecraft Servers to 'merge' existing custom Minecraft word surfaces and vanilla undergrounds. It works by allowing the vanilla generation engine create chunks as normal, then intercepting the generator and forcing the surface of the original world, which works as a reference. In oder worlds, Underilla generates a brand-new world with vanilla undergrounds, but cloning the surface of an already existing world.

It's original purpose is adding vanilla caves to custom [WorldPainter](https://www.worldpainter.net/) worlds, but it would perfectly work for any pre-generated world.

![Underilla](https://github.com/HydrolienF/Underilla/assets/71718798/5d4c0812-443e-42db-90cf-a138f11ec6c9)


## Main features
- 4 merging strategies:
    - **None:** No actual vanilla underground noise is generated. `generate_noodle_caves` setting can still be on, to generate noodle caves.
    - **Absolute:** A Y coordinate value divides the original world surface and vanilla underground.
    - **Surface:** Mix the original world surface and vanilla underground at a variable y that depends of original & vanilla world surface. It have the best racio generated world quality & performance.
    - **Relative:** **This strategie still need improvement, for now you should use Surface**. The reference world's surface will be dynamically carved into vanilla underground; which means there's no actual height-based division.
- Custom caves also supported. If using Relative merge strategy, every non-solid block and surroundings will be preserved, thus, if the reference world has itself an underground system, it'll be transferred over to the merged world.
- Heightmap fixed. Underilla re-calculates heightmaps when merging chunks, getting rid of floating and buried structures. Vanilla villagers and other structures are placed at the right height.
- Biome overwrite. Biomes from the reference world will be transferred and overwrite biomes from de vanilla seed being used. Cave biomes underground will be preserved.

## Getting started
### Perquisites

- Java 21.
- A pre-generated world to use as a reference (Such as a WorldPainter world).
- A [Paper](https://papermc.io/software/paper) (or forks) Minecraft Server of version [1.21 - 1.21.1]. It might work with upper version, but only 1.21.1 have been tested. Use old release for [1.19 - 1.20.6] compatibility.

### Single player or non-Bukkit
Underilla is currently only implemented as a Paper plugin, so it runs only on Paper (or fork) servers. If you have a Vanilla, Forge or non Bukkit-based server; or looking for a single player experience; you may [use a local Paper server](https://papermc.io/software/paper) to pre-generate a fully-merged world and then copy the resulting world folder to your actual `saves` folder.

### Installation

1. Set up your Spigot (or fork) server.
2. Download Underilla's `.jar`.
3. Place Underilla's `.jar` file into the `./plugins` directory of your server. Create the folder if it doesn't exist already.
4. Into the `./plugins` folder, create a new folder called `Underilla` and place a `config.yml` file in it. You may get the file from [this repo](Underilla-Spigot/src/main/resources/config.yml).
5. Open the `bukkit.yml` file in your server's root and add the following lines on top:
   ```
   worlds:
     world:
       generator: Underilla
   ```
   This will tell Spigot to use Underilla's chunk generator.
6. In your server's root, create a new folder called `world_base`.
7. From the folder of your reference world, copy the `region` folder, and paste it into the `world_base` folder you just created.
8. If existing, delete the `world` folder fom your server's root.
9. (Optional) Open the `server.properties` file in your server's root, and tweak the `level-seed` property. This has a direct impact on the generated underground.
10. Run the server.
    You'll notice Underilla generating merged chunks during world creation.

**Important:** Make sure your server's main world is still set to `world`. Aside from this plugin, the server itself doesn't need to "know" about the reference world.

### Pregenerate
Underilla is significantly slower than the vanilla generator, as it doesn't relly on noise generation but on reading the reference world's region `nbt` files and analyzing its patterns to 'clone' its surface to a vanilla world. So, if your world is intended for heavy duty in a big server. It's recommended to pre-generate the whole reference world area with a chunk generator plugin, such as [Chunky](https://hangar.papermc.io/pop4959/Chunky). I'm planning adding a build-in pre-generation system in the future.
To increase generation speed you should edit `worker-threads` in your `config/paper-global.yml` to match your number of CPU cores, else paper won't use all CPU cores aviables. Using your number of core instead of default value usually double speed generation.

### Performances
Huge map generation can takes hours or even days, here is some stats about performance to help you choose your configuration settings.
All tests have been done on paper 1.20.4 on a 1000*1000 map generation of the same world painter generated world with default settings except strategy. We can't garanty that your computer will be as fast as mine, but it should be enoth to imagine how much time your world will need.
- Minecraft Vanilla generator (No Underilla) 1:36
- None strategy 3:25 (2.13 times longer than Vanilla generation)
- Absolute stategy 4:34 (2.85 times longer than Vanilla generation)
- Surface srategy 4:32 (2.83 times longer than Vanilla generation)
- Relative strategy 11:07 (6.94 times longer than Vanilla generation)

For a 50000 * 30000 world, it would take 40 hours to generate with Minecraft vanilla generator, 113 hours in surface strategie and 279 hours in relative.

### How to get the best caves as possibles
This steps are longer than the instalation steps, but will allow you to avoid all the current limitation of the plugins witch are :
- Chests & Spawners data aren't transfered from cave world.
- Caves biomes are generated after the other biome.
- Default Minecraft population will generate unwanted features.
If you are strugeling with world generation, you can ask for help on the Discord: https://discord.gg/RPNbtRSFqG

1. Download the last paper version [here](https://papermc.io/downloads/paper).
2. Create a new repository for your server and move the paper .jar file incide.
3. Start paper with `java -jar paper-*replace by paper last version*.jar -nogui`
4. Server stops because of eula, open eula.txt, set `eula=true` and restart the server.
5. You know have a vanilla ready to work server. But we still want to do some config change to make it faster for world generation. Stop the server.
6. Increase the number of thread that will be used for parallel chunk generation by setting `worker-threads: x` in `config/paper-global.yml`. x = physical CPU cores - 1. Paper default value use half your physical CPU cores, witch is great in most case, but for generation it's faster to use almost all your CPU cores.
7. Set a seed for your world by editing `servers.properties` `level-seed=x`. x = a random number. If you know it, use the same seed than your woldpainter world. This steps will make the world caves generate always the same way. It will be usefull to merge caves biomes into a new world witch have exacly the same cave shapes.
8. Download [Chunky](https://hangar.papermc.io/pop4959/Chunky) and place it in your `plugins/` directory.
9. Start your server.
10. Get your custom map min and max X & Z coordinate.
11. Select the area with Chunky from the server console `chunky corners minX minZ maxX maxZ`. (Replace min & max by the actual values.)
12. Start the world generation with `chunky start`.
13. Wait until world generation is over. World generation might takes hours or even days if your world is huge. You can do the 2 nexts steps while waiting for the world generation to be over.
15. If you haven't export your Underilla world yet, export it with no water or lava underground, no ores, no caves and no underground special stone (diorite, gravel). We will let Minecraft generation take care of the underground. This might take hours, you can do the next steps while waiting.
16. Create an empty datapack. Then add the vanilla biome files into your datapack and edit them to remove the unwanted features. You probably want to remove all trees if you have some in your custom world. If you don't, you will have vanilla tree & custom world tree on the final world. It is recommand to set the depth to 320 everywher to be sur that there will be caves where you have custom world moutains. See [UnderillaBaseDataPack].
17. All previous steps need to be done here. Rename your `world` directory to `world_caves`. We will use this vanilla world to have caves biome in our final world.
18. Rename your exported custom world to `world_base/`
19. Download [Underilla latest release](https://github.com/HydrolienF/Underilla/releases) & place it in your `plugins/` directory.
20. Open the `bukkit.yml` file in your server's root and add the following lines on top:
    ```
    worlds:
        world:
            generator: Underilla
    ```
    This will tell Paper to use Underilla's chunk generator.
21. Copy the [Underilla config](Underilla-Spigot/src/main/resources/config.yml) into `plugins/Underilla/config.yml`.
22. Edit Underilla config by setting `transfer_biomes_from_caves_world: true`.
23. Underilla config: Remove some transfered_caves_world_biomes if you don't want all of them.
24. Underilla config: Add ignored_block_for_surface_calculation if needed.
25. Underilla config: Add blacklisted structures if needed.
26. It's know time to merge your custom world surface and a vanilla world underground. Start Chunky again with the 2 same commands. Underilla world generation might takes hours or even days if your world is huge. *The magic happends now, Underilla generate default caves then add your custom world surface. Then mixt your custom world biomes & the caves biomes from `world_caves` onto the final world. Then add structures witch will be placed according to the new surface shape & the new biome mixt. Then add the features (tree, flower, ores, grass, gravel etc) according to your datapack. Then spawn mobs according to the new biome mixt.*
27. Once the underilla generation is done, you should do a save of `world`, then check if you like it.
28. You can now safely remove Underilla from the plugin and remove `world_caves` & `world_base`
29. You can also remove Underilla from the `bukkit.yml` generator. You should replace it with [VoidWorldGenerator](https://github.com/HydrolienF/VoidWorldGenerator) to avoid any vanilla biome generation out of the Underilla world.


## Known issues

- Underilla's generation disables Minecraft's chunk blender, which means there will be sharp old-school chunk borders at the edge of the reference world's chunks. This may be tackled by trimming your custom world chunks around the edges to generate blended chunks ahead of time.
- Due to Spigot's generation API, outside the reference world's area, heightmaps are broken, which has an impact on structures. You may work around this by pre-generating the whole reference world area, and then disabling Underilla.
- **Relative strategy only:** Little underground lava and water pockets will translate to odd floating blobs in the final world if they overlap with large caves. Avoid such generation patterns.

## WorldPainter considerations
If you're going to plug your custom WorldPainter world into Underilla, consider before exporting:
- Disable caves, caverns, and chasms altogether, allow Underilla to take over that step. This is due to biome placement, every underground non-solid block in the reference_world drags its biome over along with it, this interferes with proper underground vanilla biomes.
- Always disable the `Allow Minecraft to populate the entire terrain` option. Rather use the `vanilla_population` option in Underilla's `config.yml` file.
- Don't user the resource layer. Underilla will have the vanilla generator take care of that for you.
- The Populate layer has no effect. Weather all or none of the terrain will be populated based on the above point.
- If you have custom cave/tunnels layers and want to preserve them during the merge, you'd want to use the Relative merge strategy

## Custom biome
Cave generation on custom biomes is now working. Features (ores, flowers etc) will be placed according to the custom surface world biome but structures won't.
If you have a custom world with custom biomes, you should enable custom biome it in the config.

## Feature fiter
If you want to remove some of the game features, for example the `monster_room` you can create a datapack where you have customine witch feature can spawn in each biome. Underilla will generate feature according to your cusomized biome.
It can also be used to add feature to some biome. For example a quartz_ore feature if your nether is disabled you you still want your builder to have quartz.

## Build
Create a working jar with `./gradlew buildDependents`

## TODO
- Build-in pre-generation system.
- Allow to generate the 2nd world on the fly.
