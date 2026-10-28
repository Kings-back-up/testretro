# Testing Guide

## What to Look For in Logs

When you run Minecraft with ForgeCompat and a 1.19.2 mod, you should see these log messages:

### Startup Messages
```
[ForgeCompat] ========================================
[ForgeCompat] ForgeCompat v0.1.0 initializing
[ForgeCompat] Bridging 1.19.2 Forge mods to 1.20.1
[ForgeCompat] Pure bytecode translation - no shims
[ForgeCompat] ========================================
[ForgeCompat] CoreMod initialized - bytecode translator active
```

### Transformation Messages
```
[ForgeCompat] Found X classes to transform
[ForgeCompat] Transforming class: yourmod/package/ClassName
[ForgeCompat] Material pattern detected: Material.WOOD → inlining property flags
[ForgeCompat] Field→Method: net/minecraft/world/entity/Entity.level → net/minecraft/world/entity/Entity.level()
[ForgeCompat] Method rename: net/minecraft/server/level/ServerPlayer.getLevel() → serverLevel()
[ForgeCompat] Capability remap: CapabilityItemHandler.ITEM_HANDLER_CAPABILITY → ForgeCapabilities.ITEM_HANDLER
[ForgeCompat] Event rename: LivingSetAttackTargetEvent → LivingChangeTargetEvent
[ForgeCompat] Successfully transformed: yourmod/package/ClassName
```

### Error Messages
```
[ForgeCompat] Unknown material: CUSTOM_MATERIAL - falling back to CompatHelpers
[ForgeCompat] Error transforming yourmod/package/ClassName: java.lang.NoSuchMethodError: ...
```

## Common Issues and What They Mean

### 1. No transformation messages at all
**Problem:** The CoreMod isn't loading or the filter is too strict.
**Solution:** Check that the CoreMod is declared in mods.toml and that the JS file exists.

### 2. "Found 0 classes to transform"
**Problem:** The filter is rejecting all classes.
**Solution:** The 1.19.2 mod's package might be filtered out. Check `shouldTransform()` in ForgeCompatTransformer.java.

### 3. "Unknown material: X"
**Problem:** The mod uses a Material constant that's not in our MATERIAL_FLAGS map.
**Solution:** Add the material to TranslationRules.MATERIAL_FLAGS.

### 4. NoSuchMethodError in crash log
**Problem:** A method was renamed but we don't have a translation rule for it.
**Solution:** Add the method rename to TranslationRules.METHOD_RENAMES.

### 5. NoClassDefFoundError in crash log
**Problem:** A class was renamed but we don't have a translation rule for it.
**Solution:** Add the class rename to TranslationRules.CLASS_RENAMES.

### 6. AbstractMethodError in crash log
**Problem:** An interface method signature changed.
**Solution:** This is more complex - might need a new translation pattern.

## Testing Strategy

### Test 1: Minimal Mod
Create or find a simple 1.19.2 mod that:
- Registers one block with `Properties.of(Material.WOOD)`
- Registers one item
- Adds them to a creative tab

**Expected:** Should work with minimal or no changes.

### Test 2: Homespun
Once the minimal mod works, try Homespun.

**Expected:** Will likely fail on first try. Read the crash log and add missing translations.

### Test 3: Iterate
Keep testing and adding translations until it works.

## Debugging Tips

1. **Enable debug logging** in your Minecraft launcher:
   - Add JVM argument: `-Dforge.logging.console.level=debug`
   - This will show more detailed transformation messages

2. **Check the full crash log** in `logs/latest.log` or the crash report

3. **Look for patterns** - if you see the same error multiple times, it's a systematic issue

4. **Test one thing at a time** - don't try to fix everything at once

## What to Send Me

If you hit errors, send me:
1. The relevant log messages (search for `[ForgeCompat]`)
2. The crash report or error message
3. Which 1.19.2 mod you're testing with

I'll help you add the missing translations.
