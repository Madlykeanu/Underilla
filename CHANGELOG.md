## Features / Bug fix
- **Custom biomes are fully supported**. Structures will be generate correctly in custom biomes !
- **Caves biomes can be merge inside the final world** without needing an extra caves_world. And they are merged only under the world surface.
- **Carvers **can be run before world merging or after world merging. It is now possible to have carvers caves but still have a **clean surface without holes**. This option can be configure for each biome.
- **Chunks outside** of the surface world are now generated **empty**.

## Other major or breaking changes
- Complete reorganization of the configuration.
- Underilla now need Paper 1.21.3+. Spigot is no longer supported.
- RELATIVE strategy have been removed. (It was deprecated since months because it create random stone blocks & does not support fluid in caves from surface world)