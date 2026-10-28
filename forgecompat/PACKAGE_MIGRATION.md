# Package Migration: net.minecraftforge.compat → net.devmaster.forgecompat

## What Changed

All Java source files have been moved from `net.minecraftforge.compat` to `net.devmaster.forgecompat`.

## Files Updated

### Java Source Files (6 files)
- `ForgeCompat.java` - Main mod class
- `RuntimeAdapter.java` - Event handlers
- `translate/TranslationRules.java` - All translation rules
- `translate/MaterialData.java` - Material data holder
- `translate/CompatHelpers.java` - Helper methods
- `transformer/ForgeCompatTransformer.java` - Bytecode transformer

### Configuration Files
- `gradle.properties` - Updated `mod_group_id=net.devmaster.forgecompat`
- `META-INF/mods.toml` - Updated CoreMod entrypoint path
- `forgecompat_coremod.js` - Updated Java.type() reference

## New Package Structure

```
forgecompat/src/main/java/net/devmaster/forgecompat/
├── ForgeCompat.java
├── RuntimeAdapter.java
├── translate/
│   ├── TranslationRules.java
│   ├── MaterialData.java
│   └── CompatHelpers.java
└── transformer/
    └── ForgeCompatTransformer.java
```

## Key Changes in Code

1. **Package declarations**: All files now use `package net.devmaster.forgecompat.*`

2. **Import statements**: Updated to reference new package paths

3. **Internal class paths**: Updated in ForgeCompatTransformer.java:
   - `MATERIAL_DATA_INTERNAL = "net/devmaster/forgecompat/translate/MaterialData"`
   - `COMPAT_HELPERS_INTERNAL = "net/devmaster/forgecompat/translate/CompatHelpers"`

4. **CoreMod JS**: Updated Java.type() reference:
   ```javascript
   var Transformer = Java.type('net.devmaster.forgecompat.transformer.ForgeCompatTransformer');
   ```

5. **Filter logic**: Updated shouldTransform() to exclude new package:
   ```java
   if (className.startsWith("net/devmaster/forgecompat/")) return false;
   ```

## mods.toml CoreMod Declaration

```toml
[[coremods]]
id = "forgecompat"
entrypoint = "net/devmaster/forgecompat/transformer/ForgeCompatCoreMod.js"
```

Note: The entrypoint path should match where you place the JS file. Currently it's at:
`src/main/resources/forgecompat_coremod.js`

You may need to adjust the entrypoint path in mods.toml to match your resource structure.

## Testing Checklist

- [ ] All Java files compile with new package
- [ ] CoreMod JS loads correctly
- [ ] Transformer is invoked for target mod classes
- [ ] Material translation works (Properties.of(Material.WOOD) → Properties.of().ignitedByLava())
- [ ] Field→method translation works (entity.level → entity.level())
- [ ] Method renames work (getLevel() → serverLevel())
- [ ] Capability remaps work (CapabilityItemHandler → ForgeCapabilities)

## Next Steps

1. Place the mod in a Forge 1.20.1 environment
2. Test with a simple 1.19.2 mod
3. Check logs for `[ForgeCompat]` messages
4. Verify blocks/items register correctly
5. Test with Homespun mod
