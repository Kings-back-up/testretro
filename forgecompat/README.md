# ForgeCompat — Pure Bytecode Translator

Translates 1.19.2 Forge mod bytecode directly to 1.20.1 at class load time.

**No shim classes. No fake API. No wrappers.** The transformer reads the old bytecode and rewrites it to call the new 1.20.1 APIs directly.

## ⚠️ IMPORTANT: Patching Required

Before using a 1.19.2 mod with ForgeCompat, you **must** patch its metadata to accept Minecraft 1.20.1. This is a limitation of how Forge loads mods - it validates version requirements before ForgeCompat even runs.

See [WHY_PATCHING_IS_NECESSARY.md](WHY_PATCHING_IS_NECESSARY.md) for detailed explanation and your options.

**Quick start:**
1. Download the patcher (Python script or Java JAR)
2. Place it in your mods folder with your 1.19.2 mods
3. Run it once per mod
4. Launch Minecraft with ForgeCompat

## How It Works

```
1.19.2 mod bytecode          ForgeCompatTransformer           1.20.1 runtime
─────────────────            ────────────────────             ──────────────
GETSTATIC Material.WOOD  →   inlines property flags       →   Properties.of().ignitedByLava()
GETFIELD entity.level    →   rewrites to method call      →   INVOKEVIRTUAL entity.level()
INVOKEVIRTUAL getLevel() →   renames method               →   INVOKEVIRTUAL serverLevel()
GETSTATIC CapabilityItemHandler.X →  remaps owner+field   →   GETSTATIC ForgeCapabilities.X
```

The transformer pattern-matches old bytecode sequences and replaces them with new ones.

## Architecture

```
forgecompat/src/main/java/net/devmaster/forgecompat/
├── ForgeCompat.java              # @Mod entry point
├── RuntimeAdapter.java           # Minimal runtime hooks
├── translate/
│   ├── TranslationRules.java     # ALL translation rules
│   ├── MaterialData.java         # Minimal data holder for variable Materials
│   └── CompatHelpers.java        # Tiny helpers for edge cases
├── transformer/
│   └── ForgeCompatTransformer.java  # The bytecode rewriter
└── tools/
    └── StandalonePatcher.java    # Java-based mod patcher
```

## Building

```bash
cd forgecompat
./gradlew build
# Output: build/libs/forgecompat-0.1.0.jar
```

## Testing

1. Build ForgeCompat
2. Patch your 1.19.2 mod (see patching options below)
3. Place both JARs in your mods folder
4. Launch and check logs for `[ForgeCompat]` messages

## Adding Translations

Add entries to `TranslationRules.java`:

```java
// Method rename
new MethodRename(
    "net/minecraft/world/some/Class",
    "oldMethodName", "(Lparam;)Return",
    "newMethodName", "(Lparam;)Return"
),

// Field → method
new FieldToMethod(
    "net/minecraft/world/entity/Entity",
    "fieldName", "LType;",
    "methodName", "()LType;"
),

// Material flags
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
