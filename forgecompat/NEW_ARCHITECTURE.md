# LegacyLink - New Architecture (Based on Retromod)

## Overview

After analyzing Retromod and Retrofit, we're adopting Retromod's proven approach:
- **Pre-translate JARs** to a cache folder instead of runtime transformation
- **Use ASM's ClassRemapper** with mapping tables
- **Patch metadata** (mods.toml) to relax version ranges
- **Cache translations** with hash-based keys
- **Load from cache** on subsequent launches

## Why This Is Better

### Old Approach (Runtime Transformation)
❌ Complex classloader tricks
❌ Transformation happens during mod loading (too late for some things)
❌ Hard to debug - transformations happen in memory
❌ No way to inspect translated code
❌ CoreMod/ITransformationService loading issues

### New Approach (Pre-Translation)
✅ Translate once, cache forever
✅ Can inspect translated JARs
✅ Easier to debug - see exactly what changed
✅ More reliable - no classloader magic
✅ Proven to work (Retromod uses this)

## Architecture

```
1. User puts 1.19.2 mod in mods/
2. LegacyLink detects it needs translation
3. Extract JAR to temp folder
4. Transform bytecode using ASM ClassRemapper
5. Patch mods.toml (relax version ranges)
6. Repackage to legacylink-cache/translated/
7. Load translated JAR instead of original
8. Cache key = hash(source JAR) + LegacyLink version
```

## Key Components

### 1. JarTranslator
- Extracts JAR
- Transforms all classes using ASM
- Patches metadata
- Repackages to cache

### 2. LegacyLinkTransformer (ASM-based)
- Uses ASM's ClassRemapper
- Mapping tables for 1.19.2 → 1.20.1 changes
- Handles Material, methods, fields, classes

### 3. CacheManager
- Manages legacylink-cache/translated/
- Hash-based cache keys
- Validates cache freshness

### 4. ModDetector
- Scans mods folder
- Determines which mods need translation
- Checks version ranges

### 5. MetadataPatcher
- Updates mods.toml
- Relaxes version ranges
- Adds displayTest for side-only mods

## File Structure

```
forgecompat/src/main/java/net/devmaster/legacylink/
├── LegacyLink.java                    # Main mod class
├── cache/
│   ├── CacheManager.java              # Cache management
│   └── CacheKey.java                  # Cache key generation
├── detector/
│   └── ModDetector.java               # Detect mods needing translation
├── translator/
│   ├── JarTranslator.java             # Main translation orchestrator
│   ├── LegacyLinkTransformer.java     # ASM-based bytecode transformer
│   └── MetadataPatcher.java           # mods.toml patcher
└── mapping/
    ├── TranslationRules.java          # All translation rules
    ├── MaterialFlags.java             # Material → Properties mapping
    └── MethodMappings.java            # Method rename mappings
```

## Translation Process

### Step 1: Detection
```java
// Scan mods folder
for (Path jar : modsFolder) {
    if (needsTranslation(jar)) {
        translateJar(jar);
    }
}
```

### Step 2: Translation
```java
// Extract JAR
Path tempDir = extractJar(sourceJar);

// Transform all classes
transformClasses(tempDir);

// Patch metadata
patchModsToml(tempDir);

// Repackage to cache
Path cachedJar = repackageJar(tempDir, cacheDir);
```

### Step 3: Loading
```java
// Load from cache instead of original
Path translatedJar = cacheManager.getTranslatedJar(originalJar);
loadMod(translatedJar);
```

## ASM Transformation

Using ASM's ClassRemapper:
```java
ClassReader reader = new ClassReader(classBytes);
ClassNode node = new ClassNode();
reader.accept(node, 0);

// Apply transformations
Remapper remapper = new LegacyLinkRemapper();
ClassRemapper classRemapper = new ClassRemapper(node, remapper);
node.accept(classRemapper);

// Write back
ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
node.accept(writer);
byte[] transformed = writer.toByteArray();
```

## Mapping Tables

### Material → Properties
```java
Material.WOOD → Properties.of().ignitedByLava()
Material.STONE → Properties.of().requiresCorrectToolForDrops()
```

### Method Renames
```java
Entity.getLevel() → Entity.level()
ServerPlayer.getLevel() → ServerPlayer.serverLevel()
```

### Field → Method
```java
Entity.level (field) → Entity.level() (method)
Entity.onGround (field) → Entity.onGround() (method)
```

### Class Renames
```java
MaterialColor → MapColor
```

## Metadata Patching

### Before (1.19.2 mod):
```toml
[[dependencies.mymod]]
modId="minecraft"
mandatory=true
versionRange="[1.19.2,1.20)"
```

### After (patched):
```toml
[[dependencies.mymod]]
modId="minecraft"
mandatory=true
versionRange="[1.19.2,1.21)"  # Relaxed to include 1.20.1
```

## Caching Strategy

Cache key = `SHA256(source.jar) + legacylink_version + loader_version`

```
legacylink-cache/
└── translated/
    ├── mymod-1.0.0-legacylink.jar
    ├── mymod-1.0.0-legacylink.jar.cache  # Sidecar with cache key
    └── anothermod-2.0.0-legacylink.jar
```

## Benefits

1. **Reliability**: Proven approach (Retromod uses it)
2. **Debuggability**: Can inspect translated JARs
3. **Performance**: Translate once, load many times
4. **Simplicity**: No complex classloader tricks
5. **Compatibility**: Works with all launchers

## Next Steps

1. Implement CacheManager
2. Implement JarTranslator with ASM
3. Implement MetadataPatcher
4. Implement ModDetector
5. Test with Cait's Curiosities
6. Iterate on mapping tables

## References

- Retromod: https://github.com/Bownlux/Retromod
- Retrofit: https://github.com/Astryxx/Retrofit (different approach, but good reference)
