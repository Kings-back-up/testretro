# ForgeCompat — Pure Bytecode Translator

Translates 1.19.2 Forge mod bytecode directly to 1.20.1 at class load time.

**No shim classes. No fake API. No wrappers.** The transformer reads the old bytecode and rewrites it to call the new 1.20.1 APIs directly.

## How it works

```
1.19.2 mod bytecode          ForgeCompatTransformer           1.20.1 runtime
─────────────────            ────────────────────             ──────────────
GETSTATIC Material.WOOD  →   inlines property flags       →   Properties.of().ignitedByLava()
GETFIELD entity.level    →   rewrites to method call      →   INVOKEVIRTUAL entity.level()
INVOKEVIRTUAL getLevel() →   renames method               →   INVOKEVIRTUAL serverLevel()
GETSTATIC CapabilityItemHandler.X →  remaps owner+field   →   GETSTATIC ForgeCapabilities.X
```

The transformer pattern-matches old bytecode sequences and replaces them with new ones. For example:

```
// What the 1.19.2 mod's bytecode looks like:
GETSTATIC net/minecraft/world/level/material/Material.WOOD
INVOKESTATIC Properties.of(Material)Properties

// What the transformer rewrites it to:
INVOKESTATIC Properties.of()Properties
INVOKEVIRTUAL Properties.ignitedByLava()Properties
```

No `Material` class exists at runtime. The flags are inlined directly.

## Architecture

```
forgecompat/src/main/java/net/minecraftforge/compat/
├── ForgeCompat.java              # @Mod entry point
├── RuntimeAdapter.java           # Minimal runtime hooks (logging, etc.)
├── translate/
│   ├── TranslationRules.java     # ALL translation rules (the data)
│   ├── MaterialData.java         # Minimal data holder for variable Materials
│   └── CompatHelpers.java        # Tiny helpers for cases that can't be inlined
└── transformer/
    └── ForgeCompatTransformer.java  # The actual bytecode rewriter
```

### What's in each file:

- **TranslationRules.java** — The single source of truth. Contains:
  - `CLASS_RENAMES` — MaterialColor → MapColor, etc.
  - `METHOD_RENAMES` — getLevel() → serverLevel(), etc.
  - `FIELD_TO_METHODS` — entity.level → entity.level()
  - `MATERIAL_FLAGS` — What flags each Material constant maps to
  - `CAPABILITY_REMAPS` — CapabilityItemHandler.X → ForgeCapabilities.X
  - `EVENT_RENAMES` — LivingSetAttackTargetEvent → LivingChangeTargetEvent
  - `REGISTRY_FACTORY_REMAPS` — DeferredRegister.create(ForgeRegistries.BLOCKS, id) → createBlocks(id)

- **ForgeCompatTransformer.java** — Reads TranslationRules and applies them:
  - Pattern-matches `Properties.of(Material.X)` and inlines the flags
  - Rewrites `GETFIELD entity.level` to `INVOKEVIRTUAL entity.level()`
  - Renames method calls per METHOD_RENAMES
  - Remaps capability field access
  - Handles the DeferredRegister factory pattern
  - Falls back to `CompatHelpers.of(material)` only when Material is in a variable

- **MaterialData.java** — NOT a re-implementation of Material. Just a data bag with the flag values. Only used at runtime when a mod stores a Material in a variable (the transformer can't inline flags it doesn't know at transform time).

- **CompatHelpers.java** — Tiny static methods for the rare cases where pure bytecode rewriting isn't practical. Most translations happen inline; these are the exception.

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

## Adding new translations

Just add entries to `TranslationRules.java`. For example, to add a new method rename:

```java
new MethodRename(
    "net/minecraft/world/some/Class",
    "oldMethodName", "(Lsome/param;)ReturnType",
    "newMethodName", "(Lsome/param;)ReturnType"
),
```

For a new field→method conversion:

```java
new FieldToMethod(
    "net/minecraft/world/entity/Entity",
    "fieldName", "Lfield/Type;",
    "methodName", "()Lfield/Type;"
),
```

For a new Material constant:

```java
MATERIAL_FLAGS.put("NEW_MATERIAL", new MaterialFlags(
    true,   // ignitedByLava
    false,  // liquid
    false,  // noCollission
    false,  // replaceable
    false,  // requiresCorrectTool
    MaterialFlags.PUSH_NORMAL  // pushReaction
));
```

## License

MIT
