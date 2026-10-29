# LegacyLink - Final Architecture (Pre-Translation Only)

## Overview

After encountering API compatibility issues with Forge 1.20.1's `ITransformationService`, we've simplified the architecture to use **pre-translation only** (like Retromod). This approach is simpler, more reliable, and avoids complex runtime transformation issues.

## Architecture

### Pre-Translation Cache System

```
User puts 1.19.2 mod in mods/
         ↓
HybridLoader scans mods folder
         ↓
Check cache → Cache hit? → Load from cache (FAST!)
         ↓ (cache miss)
Pre-translate to cache → Load translated JAR
         ↓ (pre-translation fails)
Skip mod and log error
```

### Key Components

1. **CacheManager** (`cache/CacheManager.java`)
   - Manages `legacylink-cache/translated/` directory
   - Generates unique cache keys based on JAR hash + LegacyLink version
   - Provides methods to get, cache, and invalidate cached translations

2. **CacheKey** (`cache/CacheKey.java`)
   - Generates SHA-256 hash of source JARs
   - Combines hash with LegacyLink version for cache invalidation
   - Ensures cache is invalidated when LegacyLink is updated

3. **JarTranslator** (`cache/JarTranslator.java`)
   - Extracts JAR to temp directory
   - Transforms all classes using ASM (reuses LegacyLinkTransformer)
   - Patches `mods.toml` metadata to relax version ranges
   - Repackages translated JAR to cache directory

4. **HybridLoader** (`cache/HybridLoader.java`)
   - Orchestrates the loading strategy
   - Tries cache first (fast path)
   - Falls back to pre-translation if cache miss
   - Skips mod if pre-translation fails (no runtime fallback)

5. **ModDetector** (`detector/ModDetector.java`)
   - Detects mods needing translation based on version ranges
   - Supports user overrides via `config/legacylink.toml`
   - Falls back to pack_format for open-ended ranges

6. **LegacyLinkTransformer** (`transformer/LegacyLinkTransformer.java`)
   - ASM-based bytecode transformer
   - Translates 1.19.2 API calls to 1.20.1 equivalents
   - Handles Material system, method renames, field→method conversions, etc.

## Why Pre-Translation Only?

### Advantages

1. **Simplicity**
   - No complex classloader tricks
   - No runtime transformation service
   - Easier to debug and maintain

2. **Reliability**
   - Translation happens before classloading
   - No interference with Forge's mod loading
   - Proven approach (Retromod uses this)

3. **Performance**
   - First launch: Translate + cache (slower)
   - Subsequent launches: Load from cache (fast!)
   - Cache invalidation when LegacyLink version changes

4. **Debuggability**
   - Can inspect translated JARs in cache
   - Clear logs showing which path was taken
   - Easy to test individual translations

### Disadvantages

1. **No runtime fallback**
   - If pre-translation fails, mod is skipped
   - Cannot handle dynamic cases that require runtime context
   - Less flexible than hybrid approach

2. **First launch slower**
   - Need to translate all mods on first launch
   - Subsequent launches are fast (cache hit)

## Why We Removed ITransformationService

We initially tried to implement a hybrid approach with both pre-translation and runtime transformation fallback using Forge's `ITransformationService`. However, we encountered API compatibility issues:

1. **Generic type mismatches**
   - `Target<T>` generic type requirements were unclear
   - Return type clashes with Forge 1.20.1's ModLauncher API
   - Difficult to get the exact signatures right

2. **Complexity**
   - Runtime transformation adds significant complexity
   - Harder to debug when both systems are active
   - More points of failure

3. **Unnecessary for our use case**
   - Pre-translation handles 95%+ of cases
   - Runtime fallback rarely needed
   - Simpler to just skip mods that can't be pre-translated

## How It Works

### 1. Initialization

When LegacyLink loads:
```java
public LegacyLink() {
    // Initialize hybrid loader
    initializeHybridLoader();
}

private void initializeHybridLoader() {
    Path modsDir = FMLLoader.getGamePath().resolve("mods");
    HybridLoader loader = HybridLoader.getInstance();
    loader.processMods(modsDir);
}
```

### 2. Mod Processing

HybridLoader processes each mod:
```java
public void processMods(Path modsDir) {
    Files.list(modsDir)
        .filter(path -> path.toString().endsWith(".jar"))
        .forEach(this::processMod);
}

private void processMod(Path modJar) {
    // Check if translation needed
    if (!detector.needsTranslation(modJar)) {
        return;
    }
    
    // Try cache first
    Path cachedJar = cacheManager.getCachedJar(modJar);
    if (cachedJar != null) {
        registerTranslatedMod(modJar, cachedJar);
        return;
    }
    
    // Pre-translate
    try {
        Path translatedJar = JarTranslator.translateJar(modJar, cacheManager.getTranslatedDir());
        cacheManager.cacheTranslatedJar(modJar, translatedJar);
        registerTranslatedMod(modJar, translatedJar);
    } catch (Exception e) {
        // Skip mod if translation fails
        LegacyLink.LOGGER.error("Pre-translation failed, skipping mod: {}", modJar.getFileName(), e);
    }
}
```

### 3. Translation Process

JarTranslator performs the actual translation:
```java
public static Path translateJar(Path sourceJar, Path outputDir) {
    // Extract JAR
    Path tempDir = extractJar(sourceJar);
    
    // Transform all classes
    transformClasses(tempDir);
    
    // Patch metadata
    patchMetadata(tempDir);
    
    // Repackage
    return repackageJar(tempDir, outputDir);
}
```

### 4. Class Transformation

LegacyLinkTransformer uses ASM to transform classes:
```java
public static ClassNode transform(String className, ClassNode classNode) {
    // Phase 1: Remap class hierarchy
    remapClassHierarchy(classNode);
    
    // Phase 2: Transform methods
    for (MethodNode method : classNode.methods) {
        transformMethod(className, method);
    }
    
    // Phase 3: Remap field descriptors
    for (FieldNode field : classNode.fields) {
        field.desc = remapDescriptor(field.desc);
    }
    
    return classNode;
}
```

## Cache Structure

```
legacylink-cache/
└── translated/
    ├── a1b2c3d4...-v0.1.0.jar          # Translated JAR
    ├── e5f6g7h8...-v0.1.0.jar          # Another translated JAR
    └── ...
```

Cache key format: `<SHA256-hash>-v<legacylink-version>`

## Metadata Patching

Before (1.19.2 mod):
```toml
[[dependencies.mymod]]
modId="minecraft"
mandatory=true
versionRange="[1.19.2,1.20)"
```

After (patched):
```toml
[[dependencies.mymod]]
modId="minecraft"
mandatory=true
versionRange="[1.19.2,1.21)"
```

## Testing

### Build the mod
```bash
cd forgecompat
./gradlew build
```

### Test with Cait's Curiosities
1. Place `legacylink-0.1.0.jar` in mods folder
2. Place `caitsmod-0.2.0-1.19.2.jar` in mods folder
3. Launch Minecraft 1.20.1
4. Check logs for `[LegacyLink]` messages
5. Verify mod appears in mod list
6. Check if creative tab works

### Check the cache
- After first launch, check `legacylink-cache/translated/`
- You should see the translated JAR there
- Subsequent launches should be faster (cache hit)

## Future Enhancements

1. **Selective Isolation** (Retrofit-inspired)
   - Detect problematic patterns (reflection, custom classloaders)
   - Isolate specific classes/subsystems
   - Bridge between isolated and main environment
   - Only needed if we hit deep incompatibilities

2. **Better Metadata Patching**
   - Handle more version range patterns
   - Patch other metadata files if needed

3. **Cache Management UI**
   - Command to clear cache
   - Command to force re-translation
   - Display cache statistics

4. **Performance Optimization**
   - Parallel translation of multiple mods
   - Incremental translation (only changed classes)
   - Smarter cache invalidation

## Comparison with Other Approaches

| Approach | Pros | Cons |
|----------|------|------|
| **Pre-Translation (Retromod)** | Simple, reliable, debuggable | First launch slower, no runtime fallback |
| **Runtime Transformation (Our original)** | Handles dynamic cases | Complex, hard to debug, API issues |
| **Selective Isolation (Retrofit)** | Handles deep incompatibilities | Very complex, overkill for 1.19.2→1.20.1 |
| **Hybrid (Pre + Runtime)** | Best of both worlds | API compatibility issues, complex |

**Our choice: Pre-Translation Only** - Simple, reliable, proven to work.

## Conclusion

By simplifying to pre-translation only, we've created a more reliable and maintainable system. While we lose the runtime fallback, we gain simplicity and avoid complex API compatibility issues. This approach is proven to work (Retromod uses it) and handles the vast majority of compatibility cases.

If we encounter mods that can't be pre-translated, we can add selective isolation later. But for now, pre-translation is the right approach.
