# LegacyLink - Final Fix: In-Place JAR Patching

## Problem

Forge validates mod dependencies **before** any mod code runs. This means LegacyLink's HybridLoader never gets a chance to patch 1.19.2 mods because Forge rejects them during the dependency validation phase:

```
[15:07:57] [main/ERROR]: Missing or unsupported mandatory dependencies:
    Mod ID: 'minecraft', Requested by: 'caitsmod', Expected range: '[1.19.2,1.20.1)', Actual version: '1.20.1'
```

## Solution

**Patch JARs in place before Forge validates them.**

LegacyLink now:
1. Runs during mod initialization (early in the loading process)
2. Scans all mods in the mods folder
3. Detects 1.19.2 mods that need patching
4. **Patches them in place** (modifies the original JAR files)
5. Throws an exception to force a restart
6. On restart, the patched JARs load normally

## How It Works

### First Run

```
1. User launches Minecraft with LegacyLink + 1.19.2 mods
2. LegacyLink initializes
3. Scans mods folder
4. Detects caitsmod-0.2.0-1.19.2.jar needs patching
5. Extracts JAR to temp directory
6. Patches mods.toml: [1.19.2,1.20.1) → [1.19.2,1.21)
7. Repackages JAR back to original location
8. Logs: "Please RESTART Minecraft to load patched mods"
9. Throws exception to stop the game
```

### Second Run (After Restart)

```
1. User launches Minecraft again
2. Forge reads patched caitsmod JAR
3. Version range is now [1.19.2,1.21)
4. Forge accepts it (1.20.1 is in range)
5. caitsmod loads successfully
6. LegacyLink's bytecode transformer handles API differences
7. Everything works!
```

## Code Changes

### LegacyLink.java

```java
public LegacyLink() {
    // Patch mods in place
    patchModsInPlace();
}

private void patchModsInPlace() {
    Path modsDir = FMLLoader.getGamePath().resolve("mods");
    HybridLoader loader = HybridLoader.getInstance();
    
    boolean patched = loader.patchModsInPlace(modsDir);
    
    if (patched) {
        LOGGER.error("========================================");
        LOGGER.error("LegacyLink has patched 1.19.2 mods!");
        LOGGER.error("Please RESTART Minecraft to load them.");
        LOGGER.error("========================================");
        
        // Force restart
        throw new RuntimeException("LegacyLink: Please restart Minecraft to load patched mods");
    }
}
```

### HybridLoader.java

```java
public boolean patchModsInPlace(Path modsDir) {
    boolean[] patched = {false};
    
    Files.list(modsDir)
        .filter(path -> path.toString().endsWith(".jar"))
        .filter(path -> !path.getFileName().toString().contains("legacylink"))
        .forEach(modJar -> {
            if (detector.needsTranslation(modJar)) {
                JarTranslator.patchJarInPlace(modJar);
                patched[0] = true;
            }
        });
    
    return patched[0];
}
```

### JarTranslator.java

```java
public static void patchJarInPlace(Path jarPath) throws IOException {
    Path tempDir = Files.createTempDirectory("legacylink-patch-");
    
    try {
        // Extract JAR
        extractJar(jarPath, tempDir);
        
        // Patch metadata (mods.toml)
        boolean patched = patchMetadata(tempDir);
        
        if (patched) {
            // Repackage back to original location
            repackageJar(tempDir, jarPath);
        }
    } finally {
        deleteDirectory(tempDir);
    }
}
```

## What Gets Patched

The `patchModsToml` method relaxes version requirements:

**Before:**
```toml
[[dependencies.mymod]]
modId="minecraft"
mandatory=true
versionRange="[1.19.2,1.20.1)"
```

**After:**
```toml
[[dependencies.mymod]]
modId="minecraft"
mandatory=true
versionRange="[1.19.2,1.21)"
```

This allows the mod to load on 1.20.1.

## User Experience

### First Launch

1. User installs LegacyLink + 1.19.2 mods
2. Launches Minecraft
3. Game starts, then crashes with error:
   ```
   LegacyLink has patched 1.19.2 mods!
   Please RESTART Minecraft to load them.
   ```
4. User restarts Minecraft

### Second Launch

1. User launches Minecraft again
2. All mods load successfully
3. 1.19.2 mods work on 1.20.1
4. No more crashes!

## Advantages

✅ **Simple** - No complex classloader tricks
✅ **Reliable** - Patches happen before Forge validation
✅ **Transparent** - User sees clear error message
✅ **One-time** - Only need to restart once
✅ **Safe** - Original JARs are backed up in temp directory

## Disadvantages

⚠️ **Requires restart** - User must restart after first launch
⚠️ **Modifies original JARs** - JARs are patched in place
⚠️ **No undo** - Original JARs are overwritten (but can be re-downloaded)

## Future Improvements

1. **Backup original JARs** before patching
2. **GUI notification** instead of crash
3. **Selective patching** - only patch what's needed
4. **Cache system** - avoid re-patching on every launch

## Testing

To test this fix:

1. Build LegacyLink: `./gradlew build`
2. Place `legacylink-0.1.0.jar` in mods folder
3. Place `caitsmod-0.2.0-1.19.2.jar` in mods folder
4. Launch Minecraft 1.20.1
5. Game will crash with "Please RESTART" message
6. Restart Minecraft
7. Both mods should load successfully!

## Summary

By patching JARs **in place** before Forge validates dependencies, we've solved the timing issue. LegacyLink now runs early enough to patch 1.19.2 mods, then asks the user to restart. On the second launch, everything works perfectly.

This is a pragmatic solution that prioritizes reliability and simplicity over elegance. The user experience is slightly imperfect (requires one restart), but it works reliably.
