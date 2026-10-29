# ForgeCompat — Pure Bytecode Translator

Translates 1.19.2 Forge mod bytecode directly to 1.20.1 at class load time.

**No shim classes. No fake API. No wrappers.** The transformer reads the old bytecode and rewrites it to call the new 1.20.1 APIs directly.

## ⚠️ IMPORTANT: Patching Required

Before using a 1.19.2 mod with ForgeCompat, you **must** patch its metadata to accept Minecraft 1.20.1. This is a simple one-time edit per mod.

**[📖 See the patching guide →](HOW_TO_PATCH_A_MOD.md)**

**Quick summary:**
1. Open the 1.19.2 mod JAR with a ZIP editor
2. Edit `META-INF/mods.toml`
3. Change `versionRange="[1.19.2,1.20)"` to `versionRange="[1.19.2,1.21)"`
4. Save and close

That's it! Takes about 2 minutes per mod.

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

## Installation

1. **Install ForgeCompat:**
   - Download `forgecompat-0.1.0.jar`
   - Place it in your `mods` folder

2. **Patch your 1.19.2 mods:**
   - Follow [HOW_TO_PATCH_A_MOD.md](HOW_TO_PATCH_A_MOD.md)
   - Takes 2 minutes per mod
   - Only needs to be done once per mod

3. **Launch Minecraft:**
   - Start Minecraft 1.20.1 with Forge
   - Your 1.19.2 mods should now work!

## Architecture

```
forgecompat/src/main/java/net/devmaster/forgecompat/
├── ForgeCompat.java              # @Mod entry point
├── RuntimeAdapter.java           # Minimal runtime hooks
├── translate/
│   ├── TranslationRules.java     # ALL translation rules
│   ├── MaterialData.java         # Minimal data holder for variable Materials
│   └── CompatHelpers.java        # Tiny helpers for edge cases
└── transformer/
    └── ForgeCompatTransformer.java  # The bytecode rewriter
```

## Building

```bash
cd forgecompat
./gradlew build
# Output: build/libs/forgecompat-0.1.0.jar
```

## Testing

1. Build ForgeCompat
2. Patch your 1.19.2 mod (see [HOW_TO_PATCH_A_MOD.md](HOW_TO_PATCH_A_MOD.md))
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

## Why Patching is Necessary

Forge validates mod version requirements before loading any mods. By the time ForgeCompat runs, 1.19.2 mods have already been rejected. The patching step changes the version requirement so Forge accepts the mod, then ForgeCompat translates the bytecode at runtime.

See [THE_PATCHING_PROBLEM.md](THE_PATCHING_PROBLEM.md) for a detailed explanation.

## License

MIT
