package net.devmaster.legacylink.cache;

import net.devmaster.legacylink.LegacyLink;
import net.devmaster.legacylink.detector.ModDetector;
import net.devmaster.legacylink.transformer.LegacyLinkTransformationService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Hybrid loader that combines pre-translation (cache) with runtime transformation (fallback).
 * 
 * Loading strategy:
 * 1. Try to load from cache (fast path)
 * 2. If not cached, pre-translate and cache (slow path, first time only)
 * 3. If pre-translation fails, enable runtime transformation as fallback
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
     * Process all mods in the mods directory.
     * This is called during mod loading to prepare translations.
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
                // Pre-translation failed - enable runtime transformation as fallback
                LegacyLink.LOGGER.warn("[LegacyLink] Pre-translation failed, using runtime fallback: {}", 
                    modJar.getFileName(), e);
                
                // Enable runtime transformation for this mod
                LegacyLinkTransformationService.getInstance().enableRuntimeFallback(modJar);
                
                // Still register the original mod (runtime transformer will handle it)
                registerTranslatedMod(modJar, modJar);
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
