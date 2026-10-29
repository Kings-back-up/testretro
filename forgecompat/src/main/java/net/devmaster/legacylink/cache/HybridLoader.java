package net.devmaster.legacylink.cache;

import net.devmaster.legacylink.LegacyLink;
import net.devmaster.legacylink.detector.ModDetector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Hybrid loader that patches 1.19.2 mods in place to work with 1.20.1.
 * 
 * Strategy:
 * 1. Scan all mods in mods folder
 * 2. Detect 1.19.2 mods that need patching
 * 3. Patch them in place (modify mods.toml to relax version requirements)
 * 4. Ask user to restart
 * 5. On restart, patched mods load normally
 */
public class HybridLoader {
    
    private static HybridLoader instance;
    private final CacheManager cacheManager;
    private final ModDetector detector;
    private final Map<String, Path> translatedMods; // modId -> translated JAR path
    
    private HybridLoader() {
        this.cacheManager = CacheManager.getInstance();
        this.detector = ModDetector.getInstance();
        this.translatedMods = new HashMap<>();
    }
    
    /**
     * Get the singleton HybridLoader instance.
     */
    public static synchronized HybridLoader getInstance() {
        if (instance == null) {
            instance = new HybridLoader();
        }
        return instance;
    }
    
    /**
     * Patch all 1.19.2 mods in place.
     * This modifies the original JARs to relax version requirements.
     * 
     * @param modsDir Path to the mods directory
     * @return true if any mods were patched, false otherwise
     */
    public boolean patchModsInPlace(Path modsDir) {
        LegacyLink.LOGGER.info("[LegacyLink] Scanning for 1.19.2 mods to patch...");
        
        boolean[] patched = {false};
        
        try {
            Files.list(modsDir)
                .filter(path -> path.toString().endsWith(".jar"))
                .filter(path -> !path.getFileName().toString().contains("legacylink"))
                .forEach(modJar -> {
                    try {
                        if (detector.needsTranslation(modJar)) {
                            LegacyLink.LOGGER.info("[LegacyLink] Patching mod in place: {}", modJar.getFileName());
                            
                            // Patch the JAR in place
                            JarTranslator.patchJarInPlace(modJar);
                            
                            patched[0] = true;
                            
                            LegacyLink.LOGGER.info("[LegacyLink] Successfully patched: {}", modJar.getFileName());
                        }
                    } catch (Exception e) {
                        LegacyLink.LOGGER.error("[LegacyLink] Failed to patch mod: {}", modJar.getFileName(), e);
                    }
                });
                
        } catch (IOException e) {
            LegacyLink.LOGGER.error("[LegacyLink] Failed to scan mods directory", e);
        }
        
        if (patched[0]) {
            LegacyLink.LOGGER.info("[LegacyLink] Patched mods in place - restart required");
        }
        
        return patched[0];
    }
    
    /**
     * Process all mods in the mods directory (legacy method, kept for compatibility).
     * 
     * @param modsDir Path to the mods directory
     */
    public void processMods(Path modsDir) {
        LegacyLink.LOGGER.info("[LegacyLink] Processing mods for translation...");
        
        try {
            Files.list(modsDir)
                .filter(path -> path.toString().endsWith(".jar"))
                .filter(path -> !path.getFileName().toString().contains("legacylink"))
                .forEach(this::processMod);
                
        } catch (IOException e) {
            LegacyLink.LOGGER.error("[LegacyLink] Failed to process mods directory", e);
        }
        
        LegacyLink.LOGGER.info("[LegacyLink] Processed {} mods, {} translated", 
            translatedMods.size(), translatedMods.size());
    }
    
    /**
     * Process a single mod JAR.
     * 
     * @param modJar Path to the mod JAR
     */
    private void processMod(Path modJar) {
        try {
            // Check if this mod needs translation
            if (!detector.needsTranslation(modJar)) {
                LegacyLink.LOGGER.debug("[LegacyLink] Mod doesn't need translation: {}", 
                    modJar.getFileName());
                return;
            }
            
            LegacyLink.LOGGER.info("[LegacyLink] Processing mod: {}", modJar.getFileName());
            
            // Try to get from cache
            Path cachedJar = cacheManager.getCachedJar(modJar);
            
            if (cachedJar != null) {
                // Cache hit - use cached version
                LegacyLink.LOGGER.info("[LegacyLink] Using cached translation: {}", 
                    cachedJar.getFileName());
                registerTranslatedMod(modJar, cachedJar);
                return;
            }
            
            // Cache miss - pre-translate
            LegacyLink.LOGGER.info("[LegacyLink] Pre-translating mod: {}", modJar.getFileName());
            
            try {
                Path translatedJar = JarTranslator.translateJar(modJar, cacheManager.getTranslatedDir());
                cacheManager.cacheTranslatedJar(modJar, translatedJar);
                registerTranslatedMod(modJar, translatedJar);
                
                LegacyLink.LOGGER.info("[LegacyLink] Pre-translation successful: {}", 
                    translatedJar.getFileName());
                
            } catch (Exception e) {
                // Pre-translation failed - skip this mod
                LegacyLink.LOGGER.error("[LegacyLink] Pre-translation failed, skipping mod: {}", 
                    modJar.getFileName(), e);
                
                // Don't register the mod - it won't be loaded
            }
            
        } catch (Exception e) {
            LegacyLink.LOGGER.error("[LegacyLink] Failed to process mod: {}", modJar.getFileName(), e);
        }
    }
    
    /**
     * Register a translated mod.
     * 
     * @param originalJar Path to the original mod JAR
     * @param translatedJar Path to the translated mod JAR
     */
    private void registerTranslatedMod(Path originalJar, Path translatedJar) {
        String modId = detector.getModId(originalJar);
        if (modId != null) {
            translatedMods.put(modId, translatedJar);
            LegacyLink.LOGGER.debug("[LegacyLink] Registered translated mod: {} -> {}", 
                modId, translatedJar.getFileName());
        }
    }
    
    /**
     * Get the translated JAR path for a mod.
     * 
     * @param modId The mod ID
     * @return Path to the translated JAR, or null if not found
     */
    public Path getTranslatedJar(String modId) {
        return translatedMods.get(modId);
    }
    
    /**
     * Check if a mod has been translated.
     * 
     * @param modId The mod ID
     * @return true if the mod has been translated
     */
    public boolean isTranslated(String modId) {
        return translatedMods.containsKey(modId);
    }
    
    /**
     * Get the number of translated mods.
     */
    public int getTranslatedCount() {
        return translatedMods.size();
    }
    
    /**
     * Clear the cache and reset the loader.
     */
    public void reset() {
        cacheManager.clearCache();
        translatedMods.clear();
        LegacyLink.LOGGER.info("[LegacyLink] HybridLoader reset");
    }
}
