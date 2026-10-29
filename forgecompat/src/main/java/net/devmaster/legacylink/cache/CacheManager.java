package net.devmaster.legacylink.cache;

import net.devmaster.legacylink.LegacyLink;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

/**
 * Manages the translation cache directory.
 * Cache structure: legacylink-cache/translated/<cache-key>.jar
 */
public class CacheManager {
    
    private static CacheManager instance;
    private final Path cacheDir;
    private final Path translatedDir;
    
    private CacheManager() {
        // Use game directory for cache
        Path gameDir = FMLPaths.GAMEDIR.get();
        this.cacheDir = gameDir.resolve("legacylink-cache");
        this.translatedDir = cacheDir.resolve("translated");
        
        // Create directories if they don't exist
        try {
            Files.createDirectories(cacheDir);
            Files.createDirectories(translatedDir);
            LegacyLink.LOGGER.info("[LegacyLink] Cache directory: {}", cacheDir);
        } catch (IOException e) {
            LegacyLink.LOGGER.error("[LegacyLink] Failed to create cache directory", e);
        }
    }
    
    /**
     * Get the singleton CacheManager instance.
     */
    public static synchronized CacheManager getInstance() {
        if (instance == null) {
            instance = new CacheManager();
        }
        return instance;
    }
    
    /**
     * Get the cached translated JAR for a source JAR, if it exists.
     * 
     * @param sourceJar Path to the original mod JAR
     * @return Path to cached JAR, or null if not cached
     */
    public Path getCachedJar(Path sourceJar) {
        String cacheKey = CacheKey.generate(sourceJar);
        Path cachedJar = translatedDir.resolve(cacheKey + ".jar");
        
        if (Files.exists(cachedJar)) {
            LegacyLink.LOGGER.info("[LegacyLink] Cache hit: {}", sourceJar.getFileName());
            return cachedJar;
        }
        
        LegacyLink.LOGGER.debug("[LegacyLink] Cache miss: {}", sourceJar.getFileName());
        return null;
    }
    
    /**
     * Cache a translated JAR.
     * 
     * @param sourceJar Path to the original mod JAR
     * @param translatedJar Path to the translated JAR
     * @return Path to the cached JAR
     */
    public Path cacheTranslatedJar(Path sourceJar, Path translatedJar) throws IOException {
        String cacheKey = CacheKey.generate(sourceJar);
        Path cachedJar = translatedDir.resolve(cacheKey + ".jar");
        
        // Copy translated JAR to cache
        Files.copy(translatedJar, cachedJar, StandardCopyOption.REPLACE_EXISTING);
        
        LegacyLink.LOGGER.info("[LegacyLink] Cached: {} -> {}", 
            sourceJar.getFileName(), cachedJar.getFileName());
        
        return cachedJar;
    }
    
    /**
     * Invalidate the cache for a specific source JAR.
     * 
     * @param sourceJar Path to the original mod JAR
     */
    public void invalidateCache(Path sourceJar) {
        String cacheKey = CacheKey.generate(sourceJar);
        Path cachedJar = translatedDir.resolve(cacheKey + ".jar");
        
        try {
            if (Files.exists(cachedJar)) {
                Files.delete(cachedJar);
                LegacyLink.LOGGER.info("[LegacyLink] Cache invalidated: {}", sourceJar.getFileName());
            }
        } catch (IOException e) {
            LegacyLink.LOGGER.error("[LegacyLink] Failed to invalidate cache", e);
        }
    }
    
    /**
     * Clear the entire cache.
     */
    public void clearCache() {
        try (Stream<Path> files = Files.list(translatedDir)) {
            files.forEach(file -> {
                try {
                    Files.delete(file);
                } catch (IOException e) {
                    LegacyLink.LOGGER.error("[LegacyLink] Failed to delete cached file: {}", file, e);
                }
            });
            LegacyLink.LOGGER.info("[LegacyLink] Cache cleared");
        } catch (IOException e) {
            LegacyLink.LOGGER.error("[LegacyLink] Failed to clear cache", e);
        }
    }
    
    /**
     * Get the cache directory path.
     */
    public Path getCacheDir() {
        return cacheDir;
    }
    
    /**
     * Get the translated JARs directory path.
     */
    public Path getTranslatedDir() {
        return translatedDir;
    }
    
    /**
     * Get the total size of the cache in bytes.
     */
    public long getCacheSize() {
        try (Stream<Path> files = Files.list(translatedDir)) {
            return files.mapToLong(path -> {
                try {
                    return Files.size(path);
                } catch (IOException e) {
                    return 0;
                }
            }).sum();
        } catch (IOException e) {
            return 0;
        }
    }
    
    /**
     * Get the number of cached JARs.
     */
    public long getCachedJarCount() {
        try (Stream<Path> files = Files.list(translatedDir)) {
            return files.filter(p -> p.toString().endsWith(".jar")).count();
        } catch (IOException e) {
            return 0;
        }
    }
}
