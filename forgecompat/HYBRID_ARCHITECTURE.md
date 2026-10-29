# LegacyLink - Hybrid Architecture

## Philosophy

Combine the best of all three approaches:
- **Retromod**: Pre-translation + caching (reliability, debuggability)
- **Our approach**: Runtime transformation (simplicity, no extra files)
- **Retrofit**: Selective isolation (handles deep incompatibilities)

## Hybrid Strategy

### Layer 1: Pre-Translation (Retromod-inspired)
- Translate JARs to cache on first launch
- Use ASM ClassRemapper for clean bytecode transformation
- Cache with hash-based keys
- Load from cache on subsequent launches

### Layer 2: Runtime Fallback (Our approach)
- If pre-translation fails or is incomplete
- Fall back to runtime transformation via ITransformationService
- Handles edge cases that pre-translation misses

### Layer 3: Selective Isolation (Retrofit-inspired)
- For deep incompatibilities (custom classloaders, reflection-heavy mods)
- Isolate specific problematic subsystems
- Bridge between isolated and main environment

## Architecture Flow

```
User puts 1.19.2 mod in mods/
         ↓
    ModDetector scans
         ↓
    Is it already cached?
         ↓
    ┌────┴────┐
   YES        NO
    ↓          ↓
Load from   Pre-translate
cache       to cache
    ↓          ↓
    └────┬────┘
         ↓
    Load translated JAR
         ↓
    Runtime issues?
         ↓
    ┌────┴────┐
   YES        NO
    ↓          ↓
Runtime     Success!
transform
fallback
    ↓
Still failing?
    ↓
Selective
isolation
```

## Implementation Plan

### Phase 1: Cache Layer (Retromod-style)
- CacheManager: manages legacylink-cache/translated/
- JarTranslator: pre-translates JARs using ASM
- MetadataPatcher: patches mods.toml
- Cache key: SHA256(source) + legacylink_version

### Phase 2: Runtime Fallback (Our approach)
- Keep existing ITransformationService
- Use as fallback when cache misses or translation incomplete
- Handles dynamic cases

### Phase 3: Selective Isolation (Retrofit-inspired)
- Detect problematic patterns (reflection, custom classloaders)
- Isolate specific classes/subsystems
- Bridge between isolated and main environment

## File Structure

```
forgecompat/src/main/java/net/devmaster/legacylink/
├── LegacyLink.java                      # Main mod class
├── cache/
│   ├── CacheManager.java                # Cache management
│   ├── CacheKey.java                    # Cache key generation
│   └── JarTranslator.java               # Pre-translation orchestrator
├── transformer/
│   ├── LegacyLinkTransformer.java       # ASM-based transformer (shared)
│   ├── RuntimeTransformer.java          # Runtime fallback
│   └── LegacyLinkTransformationService.java  # ITransformationService
├── detector/
│   └── ModDetector.java                 # Detect mods needing translation
├── patcher/
│   └── MetadataPatcher.java             # mods.toml patcher
├── isolation/
│   ├── IsolationManager.java            # Selective isolation
│   └── BridgeAdapter.java               # Bridge isolated ↔ main
└── mapping/
    ├── TranslationRules.java            # All translation rules
    ├── MaterialFlags.java               # Material → Properties
    └── MethodMappings.java              # Method renames
```

## Key Components

### 1. CacheManager
```java
public class CacheManager {
    private final Path cacheDir;
    
    public Path getCachedJar(Path sourceJar) {
        String cacheKey = generateCacheKey(sourceJar);
        Path cachedJar = cacheDir.resolve(cacheKey + ".jar");
        
        if (Files.exists(cachedJar)) {
            return cachedJar;
        }
        return null;
    }
    
    public Path cacheTranslatedJar(Path sourceJar, Path translatedJar) {
        String cacheKey = generateCacheKey(sourceJar);
        Path cachedJar = cacheDir.resolve(cacheKey + ".jar");
        Files.copy(translatedJar, cachedJar);
        return cachedJar;
    }
}
```

### 2. JarTranslator
```java
public class JarTranslator {
    public Path translateJar(Path sourceJar, Path outputDir) {
        // Extract JAR
        Path tempDir = extractJar(sourceJar);
        
        // Transform all classes
        transformClasses(tempDir);
        
        // Patch metadata
        MetadataPatcher.patch(tempDir);
        
        // Repackage
        return repackageJar(tempDir, outputDir);
    }
    
    private void transformClasses(Path tempDir) {
        Files.walk(tempDir)
            .filter(p -> p.toString().endsWith(".class"))
            .forEach(this::transformClass);
    }
    
    private void transformClass(Path classFile) {
        byte[] original = Files.readAllBytes(classFile);
        byte[] transformed = LegacyLinkTransformer.transform(original);
        Files.write(classFile, transformed);
    }
}
```

### 3. Hybrid Loading Strategy
```java
public class HybridLoader {
    public void loadMod(Path modJar) {
        // Try cache first
        Path cached = cacheManager.getCachedJar(modJar);
        
        if (cached != null) {
            LOGGER.info("Loading from cache: {}", cached);
            loadFromCache(cached);
            return;
        }
        
        // Pre-translate
        LOGGER.info("Pre-translating: {}", modJar);
        Path translated = jarTranslator.translateJar(modJar, cacheDir);
        cacheManager.cacheTranslatedJar(modJar, translated);
        
        // Load translated
        loadFromCache(translated);
    }
    
    private void loadFromCache(Path cachedJar) {
        try {
            // Try loading from cache
            loadModFromJar(cachedJar);
        } catch (Exception e) {
            // Fallback to runtime transformation
            LOGGER.warn("Cache load failed, using runtime fallback", e);
            runtimeTransformer.enableForMod(cachedJar);
            loadModFromJar(cachedJar);
        }
    }
}
```

### 4. Selective Isolation
```java
public class IsolationManager {
    public boolean needsIsolation(Path modJar) {
        // Detect problematic patterns
        return hasCustomClassloader(modJar) || 
               hasHeavyReflection(modJar) ||
               hasNativeCode(modJar);
    }
    
    public void isolate(Path modJar) {
        // Create isolated environment for problematic parts
        IsolatedEnvironment env = new IsolatedEnvironment(modJar);
        bridgeAdapter.connect(env, mainEnvironment);
    }
}
```

## Benefits of Hybrid Approach

### Reliability (Retromod)
- Pre-translation happens before classloading
- Can inspect and debug translated JARs
- Cache means consistent behavior

### Simplicity (Our approach)
- Runtime fallback handles edge cases
- No need to pre-translate everything perfectly
- Graceful degradation

### Compatibility (Retrofit)
- Selective isolation for deep incompatibilities
- Can handle mods that can't be fully translated
- Bridge between old and new code

## Migration Strategy

### Step 1: Implement Cache Layer
- Add CacheManager
- Add JarTranslator
- Keep existing runtime transformer

### Step 2: Hybrid Loading
- Try cache first
- Fall back to runtime transformation
- Log which path was taken

### Step 3: Add Selective Isolation
- Detect problematic patterns
- Isolate when needed
- Bridge between environments

## Testing Strategy

1. **Test cache layer**: Verify pre-translation works
2. **Test fallback**: Verify runtime transformation still works
3. **Test isolation**: Verify problematic mods can be isolated
4. **Test hybrid**: Verify all three layers work together

## Example Scenarios

### Scenario 1: Simple mod (most common)
- Pre-translate to cache ✓
- Load from cache ✓
- No issues

### Scenario 2: Mod with edge cases
- Pre-translate to cache (partial)
- Load from cache
- Runtime transformation handles remaining issues
- Works!

### Scenario 3: Deep incompatibility
- Pre-translate to cache (partial)
- Load from cache
- Runtime transformation handles some issues
- Selective isolation handles deep issues
- Works!

## Performance

- **First launch**: Translate + cache (slower)
- **Subsequent launches**: Load from cache (fast)
- **Cache invalidation**: When LegacyLink version changes
- **Cache size**: ~same as original mod JAR

## Debugging

- Can inspect translated JARs in cache
- Logs show which path was taken (cache/runtime/isolation)
- Can disable cache for testing
- Can force re-translation

## Next Steps

1. Implement CacheManager
2. Implement JarTranslator (reuse existing transformer logic)
3. Implement MetadataPatcher
4. Update ModDetector to use cache
5. Keep runtime transformer as fallback
6. Test with Cait's Curiosities
7. Add selective isolation if needed

This hybrid approach gives us the best of all worlds:
- Retromod's reliability and debuggability
- Our simplicity and graceful degradation
- Retrofit's compatibility with deep incompatibilities
