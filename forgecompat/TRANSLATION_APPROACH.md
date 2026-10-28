# ForgeCompat — Pure Bytecode Translator

## The Approach

You were right. Instead of re-implementing the old 1.19.2 API as shim classes, this project **translates** the old bytecode directly to new bytecode at class load time.

**Before (shim approach):**
```
Mod code: Properties.of(Material.WOOD)
  → transformer redirects to: shim.Material.WOOD
  → shim.Material.WOOD.applyTo(Properties.of())
  → 1.20.1 Properties.of().ignitedByLava()
```
Three hops. Maintaining a whole fake Material class.

**After (translation approach):**
```
Mod code: Properties.of(Material.WOOD)
  → transformer rewrites directly to: Properties.of().ignitedByLava()
```
One hop. No shim Material class. The transformer inlines the flags.

## What Changed

### Removed (shim classes):
- `shim/material/Material.java` — re-implementation of Material
- `shim/material/MaterialColor.java` — wrapper for MapColor
- `shim/registries/RegistryObject.java` — wrapper for DeferredHolder
- `shim/registries/ShimDeferredRegister.java` — wrapper for DeferredRegister
- `shim/registries/ShimForgeRegistry.java` — wrapper for ForgeRegistries
- `shim/entity/EntityCompat.java` — field→method helpers
- `shim/events/LivingSetAttackTargetEvent.java` — event shim
- `shim/capabilities/CapabilityCompat.java` — capability wrapper
- `shim/network/ShimSimpleChannel.java` — network wrapper

### Added (translation):
- `translate/TranslationRules.java` — ALL translation rules in one file
- `translate/MaterialData.java` — minimal data bag (NOT a re-implementation)
- `translate/CompatHelpers.java` — tiny helpers for non-inlineable cases

### Rewritten:
- `transformer/ForgeCompatTransformer.java` — now does pure translation

## How It Works

### 1. Material Translation

**Case 1: Compile-time constant** (most common)
```java
// 1.19.2 mod bytecode:
GETSTATIC Material.WOOD
INVOKESTATIC Properties.of(Material)Properties

// Transformer rewrites to:
INVOKESTATIC Properties.of()Properties
INVOKEVIRTUAL Properties.ignitedByLava()Properties
```
The transformer sees `GETSTATIC Material.WOOD`, looks up WOOD's flags in `TranslationRules.MATERIAL_FLAGS`, and inlines them. No Material class exists at runtime.

**Case 2: Variable Material** (rare)
```java
// 1.19.2 mod bytecode:
Material m = getMaterial();
Properties p = Properties.of(m);

// Transformer rewrites to:
MaterialData m = getMaterial();
Properties p = CompatHelpers.of(m);
```
When the Material is in a variable, the transformer can't inline flags it doesn't know at transform time. It rewrites to `CompatHelpers.of(MaterialData)` where `MaterialData` is just a data bag of flag values.

### 2. Field → Method Translation

```java
// 1.19.2: entity.level (field)
GETFIELD net/minecraft/world/entity/Entity.level : Level

// Transformer rewrites to:
INVOKEVIRTUAL net/minecraft/world/entity/Entity.level() : Level
```

### 3. Method Renames

```java
// 1.19.2:
INVOKEVIRTUAL ServerPlayer.getLevel() : ServerLevel

// Transformer rewrites to:
INVOKEVIRTUAL ServerPlayer.serverLevel() : ServerLevel
```

### 4. Capability Remaps

```java
// 1.19.2:
GETSTATIC CapabilityItemHandler.ITEM_HANDLER_CAPABILITY

// Transformer rewrites to:
GETSTATIC ForgeCapabilities.ITEM_HANDLER
```

### 5. Registry Factory Pattern

```java
// 1.19.2:
GETSTATIC ForgeRegistries.BLOCKS
LDC "modid"
INVOKESTATIC DeferredRegister.create(IForgeRegistry, String)DeferredRegister

// Transformer rewrites to:
LDC "modid"
INVOKESTATIC DeferredRegister.createBlocks(String)DeferredRegister$Blocks
```

## The Files

```
forgecompat/src/main/java/net/minecraftforge/compat/
├── ForgeCompat.java              # @Mod entry point
├── RuntimeAdapter.java           # Minimal runtime hooks (logging)
├── translate/
│   ├── TranslationRules.java     # ALL translation rules (the data)
│   ├── MaterialData.java         # Minimal data bag for variable Materials
│   └── CompatHelpers.java        # Tiny helpers for non-inlineable cases
└── transformer/
    └── ForgeCompatTransformer.java  # The bytecode rewriter (the brain)
```

### TranslationRules.java

This is the single source of truth. Contains:
- `CLASS_RENAMES` — MaterialColor → MapColor
- `METHOD_RENAMES` — getLevel() → serverLevel(), etc.
- `FIELD_TO_METHODS` — entity.level → entity.level()
- `MATERIAL_FLAGS` — What flags each Material constant maps to
- `CAPABILITY_REMAPS` — CapabilityItemHandler.X → ForgeCapabilities.X
- `EVENT_RENAMES` — LivingSetAttackTargetEvent → LivingChangeTargetEvent
- `REGISTRY_FACTORY_REMAPS` — DeferredRegister.create(ForgeRegistries.BLOCKS, id) → createBlocks(id)

To add support for a new API change, just add an entry to this file.

### ForgeCompatTransformer.java

Reads TranslationRules and applies them:
- Pattern-matches `Properties.of(Material.X)` and inlines the flags
- Rewrites `GETFIELD entity.level` to `INVOKEVIRTUAL entity.level()`
- Renames method calls per METHOD_RENAMES
- Remaps capability field access
- Handles the DeferredRegister factory pattern
- Falls back to `CompatHelpers.of(material)` only when Material is in a variable

### MaterialData.java

**NOT a re-implementation of Material.** Just a data bag with flag values. Only used at runtime when a mod stores a Material in a variable (the transformer can't inline flags it doesn't know at transform time).

### CompatHelpers.java

Tiny static methods for the rare cases where pure bytecode rewriting isn't practical. Most translations happen inline; these are the exception.

## Benefits

1. **Less code** — 12 files instead of 19
2. **No fake API** — No re-implementations of Material, RegistryObject, etc.
3. **Zero runtime overhead** — No wrapper indirection
4. **Easier to maintain** — All translation logic in one file (TranslationRules.java)
5. **Better errors** — Errors point to real 1.20.1 APIs, not shim code
6. **More transparent** — You can see exactly what the old code becomes

## Building

```bash
# Needs a Forge 1.20.1 MDK environment
cd forgecompat
./gradlew build
# Output: build/libs/forgecompat-0.1.0.jar
```

## Testing with Homespun

1. Build ForgeCompat
2. Get a 1.19.2 build of Homespun (before its NeoForge port)
3. Put both JARs in a 1.20.1 Forge mods folder
4. Launch and watch the log for `[ForgeCompat]` messages
5. Check that blocks, items, creative tab, fluids, and recipes work

## Adding New Translations

Just add entries to `TranslationRules.java`:

```java
// Method rename:
new MethodRename(
    "net/minecraft/world/some/Class",
    "oldMethodName", "(Lsome/param;)ReturnType",
    "newMethodName", "(Lsome/param;)ReturnType"
),

// Field → method:
new FieldToMethod(
    "net/minecraft/world/entity/Entity",
    "fieldName", "Lfield/Type;",
    "methodName", "()Lfield/Type;"
),

// Material flags:
MATERIAL_FLAGS.put("NEW_MATERIAL", new MaterialFlags(
    true,   // ignitedByLava
    false,  // liquid
    false,  // noCollission
    false,  // replaceable
    false,  // requiresCorrectTool
    MaterialFlags.PUSH_NORMAL
));
```

## License

MIT
