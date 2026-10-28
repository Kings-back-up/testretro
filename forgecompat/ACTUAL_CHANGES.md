# Forge 1.19.2 → 1.20.1: What Actually Changed

After researching the official Forge documentation, here are the **real** differences between Forge 1.19.2 and Forge 1.20.1:

## Registry System: IDENTICAL

**Good news:** The registry API is the same in both versions.

```java
// 1.19.2:
private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
public static final RegistryObject<Block> MY_BLOCK = BLOCKS.register("my_block", () -> new Block(...));

// 1.20.1:
private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
public static final RegistryObject<Block> MY_BLOCK = BLOCKS.register("my_block", () -> new Block(...));
```

**No translation needed.** `DeferredRegister`, `RegistryObject`, `ForgeRegistries` — all identical.

*(Note: The `DeferredBlock`/`DeferredHolder`/`createBlocks()` stuff is from **NeoForge**, not Forge. Don't confuse them!)*

## Block Properties: Material System Removed

**This is the main change.** In 1.19.2, you passed a `Material` to `Properties.of()`. In 1.20.1, `Material` was removed and replaced with individual property flags.

```java
// 1.19.2:
BlockBehaviour.Properties.of(Material.WOOD)
    .strength(2.0F, 3.0F)
    .sound(SoundType.WOOD)
    .color(MaterialColor.WOOD);

// 1.20.1:
BlockBehaviour.Properties.of()
    .strength(2.0F, 3.0F)
    .sound(SoundType.WOOD)
    .mapColor(MapColor.WOOD)
    .ignitedByLava();  // replaces Material.WOOD's flammability
```

**Translation needed:** The transformer must detect `Properties.of(Material.X)` and inline the appropriate flags.

### Material → Property Flags Mapping

| Material | ignitedByLava | liquid | noCollission | replaceable | requiresCorrectTool | pushReaction |
|----------|---------------|--------|--------------|-------------|---------------------|--------------|
| WOOD | ✓ | | | | | NORMAL |
| STONE | | | | | ✓ | NORMAL |
| METAL | | | | | ✓ | NORMAL |
| PLANT | ✓ | | ✓ | | | DESTROY |
| WATER | | ✓ | ✓ | | | NORMAL |
| LAVA | | ✓ | ✓ | | | NORMAL |
| DIRT | | | | | | NORMAL |
| SAND | | | | | | NORMAL |
| AIR | | | ✓ | | | NORMAL |
| DECORATION | | | ✓ | | | DESTROY |
| WOOL | ✓ | | | | | NORMAL |

*(Full mapping in `TranslationRules.MATERIAL_FLAGS`)*

## Entity Fields → Methods

In 1.20.1, some public fields became getter methods:

```java
// 1.19.2:
Level level = entity.level;
boolean grounded = entity.isOnGround;

// 1.20.1:
Level level = entity.level();
boolean grounded = entity.onGround();
```

**Translation needed:** `GETFIELD entity.level` → `INVOKEVIRTUAL entity.level()`

## Method Renames

Several methods were renamed:

| 1.19.2 | 1.20.1 |
|--------|--------|
| `ServerPlayer.getLevel()` | `ServerPlayer.serverLevel()` |
| `ServerPlayer.setLevel()` | `ServerPlayer.setServerLevel()` |
| `Entity.outOfWorld()` | `Entity.onBelowWorld()` |
| `Entity.checkOutOfWorld()` | `Entity.checkBelowWorld()` |
| `ItemStack.sameItem()` | `ItemStack.isSameItem()` |
| `DamageSources.outOfWorld()` | `DamageSources.fellOutOfWorld()` |
| `Properties.color(MapColor)` | `Properties.mapColor(MapColor)` |

**Translation needed:** Method name remapping.

## Class Renames

```java
// 1.19.2:
net.minecraft.world.level.material.MaterialColor

// 1.20.1:
net.minecraft.world.level.material.MapColor
```

**Translation needed:** Class reference remapping throughout descriptors.

## Capabilities

```java
// 1.19.2:
import net.minecraftforge.common.capabilities.CapabilityItemHandler;
Capability<IItemHandler> cap = CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;

// 1.20.1:
import net.minecraftforge.common.ForgeCapabilities;
Capability<IItemHandler> cap = ForgeCapabilities.ITEM_HANDLER;
```

**Translation needed:** `GETSTATIC CapabilityItemHandler.ITEM_HANDLER_CAPABILITY` → `GETSTATIC ForgeCapabilities.ITEM_HANDLER`

## Events

Some events were renamed:

| 1.19.2 | 1.20.1 |
|--------|--------|
| `LivingSetAttackTargetEvent` | `LivingChangeTargetEvent` |
| `SaplingGrowTreeEvent` | `BlockGrowFeatureEvent` |

**Translation needed:** Event class reference remapping.

## What Didn't Change

- **DeferredRegister API** — identical
- **RegistryObject** — identical
- **ForgeRegistries** — identical
- **Event bus system** — identical
- **Network system** — mostly identical (minor changes)
- **Block entity system** — mostly identical
- **Creative tabs** — mostly identical (minor API changes)

## Summary

The main translation work is:
1. **Material system** → inline property flags (biggest change)
2. **Entity fields** → getter methods
3. **Method renames** → update method names
4. **Class renames** → MaterialColor → MapColor
5. **Capability remaps** → ForgeCapabilities
6. **Event renames** → update event class names

Everything else (registries, event bus, network, block entities) is largely unchanged between Forge 1.19.2 and 1.20.1.
