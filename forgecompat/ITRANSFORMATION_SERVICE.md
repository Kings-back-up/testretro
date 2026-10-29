# LegacyLink - ITransformationService Implementation

## Overview

LegacyLink now uses Forge's `ITransformationService` API to register bytecode transformers, instead of relying on CoreMod JavaScript files. This approach is more reliable because:

1. **Earlier loading**: TransformationService is loaded during ModLauncher initialization, earlier than CoreMod
2. **More stable API**: Uses Java interfaces instead of JavaScript, avoiding JS engine compatibility issues
3. **Better error handling**: Java code is easier to debug and catch exceptions

## Architecture Changes

### Before (CoreMod approach)
```
mods.toml → coremod.js → LegacyLinkTransformer
```

### Now (ITransformationService approach)
```
META-INF/services/cpw.mods.modlauncher.api.ITransformationService 
    → LegacyLinkTransformationService 
    → LegacyLinkTransformer
```

## File Structure

### Core Files
- `LegacyLinkTransformer.java` - Bytecode transformation logic (unchanged)
- `LegacyLinkTransformationService.java` - Register transformer service (new)
- `META-INF/services/cpw.mods.modlauncher.api.ITransformationService` - Service registration file (new)

### Helper Files
- `CreativeModeTabIconHelper.java` - Handle CreativeModeTab icons (new)
- Deleted `legacylink_coremod.js`
- Deleted `CreativeModeTabCompat.java`
- Deleted `LegacyLinkRuntimeTransformer.java`

## CreativeModeTab Handling

### Problem
The `CreativeModeTab(String)` constructor from 1.19.2 was removed in 1.20.1, only `CreativeModeTab(Builder)` constructor remains.

### Solution
Don't rename the superclass (keep anonymous class extending the real `CreativeModeTab`), instead rewrite the constructor call:

**Before transformation:**
```java
new CreativeModeTab("my_tab") {
    @Override
    public ItemStack makeIcon() {
        return new ItemStack(MY_ITEM.get());
    }
}
```

**After transformation (bytecode level):**
```
1. Call CreativeModeTabIconHelper.buildBuilder("my_tab") to get Builder
2. Call super(Builder) instead of super(String)
3. Call CreativeModeTabIconHelper.bindPendingIcon(this) to bind icon
```

### How CreativeModeTabIconHelper Works
1. `buildBuilder(String)` - Creates a Builder with the correct title
2. `bindPendingIcon(Object)` - Binds the icon provider after construction, uses reflection to call `makeIcon()` method

## Configuration Changes

### mods.toml
Removed the `[[coremods]]` section, no longer need CoreMod declaration.

### build.gradle
Removed `FMLCorePlugin` and `FMLCorePluginContainsFMLMod` attributes from JAR manifest.

## Testing

Rebuild the mod:
```bash
./gradlew clean build
```

Check logs for:
- `[LegacyLink] Transformation service initializing`
- `[LegacyLink] Transformation service loaded`
- `[LegacyLink] Registering class transformers`
- `[LegacyLink] Transforming class via service: ...`

## Advantages

1. **No JavaScript needed** - Pure Java implementation, more stable
2. **Earlier loading** - Starts transforming before all mod classes are loaded
3. **Better integration** - Uses Forge's officially recommended extension point
4. **Easier debugging** - Java code is easier to debug than JavaScript

## Future Improvements

- Add more transformation rules to handle other API changes
- Implement smarter CreativeModeTab icon handling (avoid reflection)
- Add transformation statistics and reporting features
