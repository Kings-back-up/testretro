# LegacyLink - Final Implementation

## Overview

LegacyLink is a bytecode translator that allows 1.19.2 Forge mods to run on Minecraft 1.20.1 Forge. It uses Forge's `ITransformationService` API to transform mod bytecode at load time, with automatic detection of which mods need translation.

## Key Features

### 1. Automatic Mod Detection

LegacyLink automatically determines which mods need translation based on:

1. **Version Range Analysis**: Checks the mod's declared Minecraft version range in `mods.toml`
   - If the range includes 1.20.1, the mod is skipped (already compatible)
   - If the range only includes 1.19.x, the mod is translated
   - If the range is open-ended (like `[1.19.2,)`), falls back to pack_format check

2. **Pack Format Fallback**: For open-ended ranges, checks `pack.mcmeta`
   - pack_format 9-14: 1.19.x → translate
   - pack_format 15+: 1.20.x → skip

3. **User Overrides**: Supports `config/legacylink.toml` for manual overrides
   - `force = ["modid"]`: Force translation even if mod appears compatible
   - `skip = ["modid"]`: Skip translation even if mod appears to need it

### 2. Correct 1.19.2 Material Translation

The Material system was completely removed in 1.20.1. LegacyLink correctly translates:

**Material Constants** (all 47 constants from 1.19.2):
- AIR, STRUCTURAL_AIR, PORTAL, CLOTH_DECORATION, PLANT, WATER_PLANT
- REPLACEABLE_PLANT, REPLACEABLE_FIREPROOF_PLANT, REPLACEABLE_WATER_PLANT
- WATER, BUBBLE_COLUMN, LAVA, TOP_SNOW, FIRE, DECORATION, WEB, SCULK
- BUILDABLE_GLASS, CLAY, DIRT, GRASS, ICE_SOLID, SAND, SPONGE, SHULKER_SHELL
- WOOD, NETHER_WOOD, BAMBOO_SAPLING, BAMBOO, WOOL, EXPLOSIVE, LEAVES
- GLASS, ICE, CACTUS, STONE, METAL, SNOW, HEAVY_METAL, BARRIER, PISTON
- MOSS, VEGETABLE, EGG, CAKE, AMETHYST, POWDER_SNOW

**Material Properties** correctly mapped:
- `flammable` → `ignitedByLava()`
- `liquid` → `liquid()`
- `!blocksMotion` → `noCollission()`
- `replaceable` → `replaceable()`
- `mapColor` → `mapColor(MapColor.X)`
- `pushReaction` → `pushReaction(PushReaction.X)`

### 3. Correct Method and Field Translations

**Field → Method Conversions**:
- `Entity.level` (field) → `Entity.level()` (method)
- `Entity.onGround` (field) → `Entity.onGround()` (method)

**Method Renames**:
- `ServerPlayer.getLevel()` → `serverLevel()`
- `Entity.outOfWorld()` → `onBelowWorld()`
- `Entity.checkOutOfWorld()` → `checkBelowWorld()`
- `ItemStack.sameItem()` → `isSameItem()`
- `ItemStack.tagMatches(stack1, stack2)` → `isSameItemSameTags(stack1, stack2)` (static)
- `BlockBehaviour.Properties.color()` → `mapColor()`
- `Direction.fromNormal()` → `fromDelta()`
- `IForgePlayer.getAttackRange()` → `getEntityReach()`
- `IForgePlayer.getReachDistance()` → `getBlockReach()`
- `LivingChangeTargetEvent.getTarget()` → `getNewTarget()`

**Capability Remaps**:
- `CapabilityItemHandler.ITEM_HANDLER_CAPABILITY` → `ForgeCapabilities.ITEM_HANDLER`
- `CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY` → `ForgeCapabilities.FLUID_HANDLER`
- `CapabilityEnergy.ENERGY` → `ForgeCapabilities.ENERGY`

**Event Renames**:
- `LivingSetAttackTargetEvent` → `LivingChangeTargetEvent`
- `SaplingGrowTreeEvent` → `BlockGrowFeatureEvent`

**Class Renames**:
- `MaterialColor` → `MapColor`

### 4. CreativeModeTab Handling

1.19.2's `CreativeModeTab(String)` constructor was removed in 1.20.1. LegacyLink:
- Keeps the anonymous class extending the real `CreativeModeTab`
- Rewrites the constructor call to use the Builder pattern
- Uses `CreativeModeTabIconHelper` to bind the icon after construction

## Architecture

```
LegacyLinkTransformationService (ITransformationService)
    ↓
ModDetector (scans mods, determines which need translation)
    ↓
LegacyLinkTransformer (applies bytecode transformations)
    ↓
TranslationRules (defines all translation mappings)
    ↓
CompatHelpers (runtime helpers for complex cases)
```

## Files

### Core Files
- `LegacyLink.java` - Main mod class
- `LegacyLinkTransformationService.java` - ITransformationService implementation
- `LegacyLinkTransformer.java` - Bytecode transformation logic
- `ModDetector.java` - Automatic mod detection
- `TranslationRules.java` - All translation rules
- `MaterialData.java` - Material data holder
- `CompatHelpers.java` - Runtime helpers
- `CreativeModeTabIconHelper.java` - CreativeModeTab icon handling

### Configuration
- `META-INF/services/cpw.mods.modlauncher.api.ITransformationService` - Service registration
- `META-INF/mods.toml` - Mod metadata
- `legacylink.toml` - User override configuration (in config folder)

## Usage

### For Players

1. Install LegacyLink in your mods folder
2. Install your 1.19.2 mods
3. Launch the game - LegacyLink automatically detects and translates compatible mods
4. No manual patching required!

### For Mod Developers

If your mod needs special handling:

1. Create `config/legacylink.toml`:
```toml
# Force translation for your mod
force = ["yourmodid"]

# Or skip translation if needed
skip = ["yourmodid"]
```

2. Check the logs for `[LegacyLink]` messages to see what's being transformed

## Testing

Check logs for:
- `[LegacyLink] Transformation service initializing`
- `[LegacyLink] Will transform mod: <modid>`
- `[LegacyLink] Transforming class via service: <classname>`
- `[LegacyLink] Material pattern detected: Material.WOOD → inlining property flags`

## Known Limitations

1. **Complex API Changes**: Some mods may use APIs that changed in ways not covered by the translation rules
2. **Reflection**: Mods that use reflection to access 1.19.2 APIs may not work
3. **Mixins**: Mods that mixin to 1.19.2-specific code may need manual porting
4. **Open-ended Ranges**: Mods with ranges like `[1.19.2,)` rely on pack_format detection, which may not be accurate

## Future Improvements

- Add more translation rules for additional API changes
- Implement better pack_format detection
- Add support for more complex bytecode patterns
- Create a GUI for managing overrides
- Add transformation statistics and reporting

## License

MIT
