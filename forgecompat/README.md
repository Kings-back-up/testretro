# ForgeCompat - Minecraft Forge Compatibility Layer

A compatibility layer that enables Minecraft Forge 1.19.2 mods to run on Minecraft Forge 1.20.1.

## How It Works

ForgeCompat uses a three-layer approach:

1. **API Shim Layer** - Java classes that re-implement the old 1.19.2 API surface
2. **Bytecode Transformer** - A CoreMod that rewrites target mod bytecode at load time
3. **Runtime Adapter** - Event bus hooks for behavioral differences

## What Gets Transformed

| 1.19.2 API | 1.20.1 API | Strategy |
|---|---|---|
| `Material.WOOD` | `Properties.of().ignitedByLava()` | Shim + Transform |
| `RegistryObject<T>` | `DeferredHolder<R,T>` | Shim wrapper |
| `entity.level` (field) | `entity.level()` (method) | ASM Transform |
| `entity.isOnGround` (field) | `entity.onGround()` (method) | ASM Transform |
| `ServerPlayer.getLevel()` | `ServerPlayer.serverLevel()` | Method remap |
| `LivingSetAttackTargetEvent` | `LivingChangeTargetEvent` | Shim subclass |
| `CapabilityItemHandler.*` | `ForgeCapabilities.ITEM_HANDLER` | Field remap |
| `Properties.of(Material)` | `MaterialCompat.propertiesOf(Material)` | Static helper |
| `SimpleChannel.registerMessage()` | `messageBuilder().consumerMainThread()` | Shim wrapper |

## Project Structure

```
forgecompat/
├── build.gradle                    # ForgeGradle build config
├── gradle.properties               # Version properties
├── settings.gradle                 # Gradle settings
└── src/main/
    ├── java/net/minecraftforge/compat/
    │   ├── ForgeCompat.java        # Main @Mod class
    │   ├── RuntimeAdapter.java     # Event bus runtime hooks
    │   ├── shim/
    │   │   ├── material/
    │   │   │   ├── Material.java           # Old Material class
    │   │   │   ├── MaterialColor.java      # MaterialColor → MapColor
    │   │   │   └── MaterialCompat.java     # Static helper for Properties.of(Material)
    │   │   ├── registries/
    │   │   │   ├── RegistryObject.java     # Wraps DeferredHolder
    │   │   │   ├── ShimDeferredRegister.java # Wraps new DeferredRegister
    │   │   │   └── ShimForgeRegistry.java  # Wraps ForgeRegistries
    │   │   ├── entity/
    │   │   │   └── EntityCompat.java       # Field → method helpers
    │   │   ├── events/
    │   │   │   └── LivingSetAttackTargetEvent.java
    │   │   ├── network/
    │   │   │   └── ShimSimpleChannel.java  # Old network API
    │   │   └── capabilities/
    │   │       └── CapabilityCompat.java   # Old capability constants
    │   └── transformer/
    │       └── ForgeCompatTransformer.java # ASM bytecode transformer
    └── resources/
        ├── META-INF/
        │   └── mods.toml           # Mod metadata + CoreMod declaration
        └── forgecompat_coremod.js  # CoreMod JS bridge to Java transformer
```

## Building

```bash
# Requires Java 17+
./gradlew build
```

The output JAR goes in `build/libs/forgecompat-0.1.0.jar`.

## Usage

1. Build ForgeCompat
2. Place `forgecompat-0.1.0.jar` in your `mods/` folder
3. Place your 1.19.2 mod JAR in the same `mods/` folder
4. Run Minecraft 1.20.1 with Forge 47.x

The CoreMod transformer will automatically detect and patch the target mod's classes.

## Testing with Homespun

The first test target is [Homespun](https://github.com/PrincessStrelka/Homespun) by PrincessStrelka.

To test:
1. Build the 1.19.2 version of Homespun (or use its source)
2. Place both JARs in a 1.20.1 Forge mods folder
3. Check the log for `[ForgeCompat] Transformed class` messages
4. Verify blocks, items, fluids, and recipes work

## Limitations

- Cannot shim direct access to removed vanilla internals
- Heavily mixin-dependent mods may need additional work
- World generation changes may not be fully compatible
- Some rendering changes require manual intervention

## License

MIT
