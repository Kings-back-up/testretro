package net.devmaster.legacylink.cache;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Generates unique cache keys for translated JARs.
 * Cache key = SHA256(source JAR) + LegacyLink version
 */
public class CacheKey {
    
    private static final String LEGACYLINK_VERSION = "0.1.0";
    
    /**
     * Generate a cache key for a source JAR.
     * 
     * @param sourceJar Path to the original mod JAR
     * @return Cache key string
     */
    public static String generate(Path sourceJar) {
        try {
            String hash = sha256(sourceJar);
            return hash + "-v" + LEGACYLINK_VERSION;
        } catch (Exception e) {
            // Fallback to filename + timestamp if hashing fails
            return sourceJar.getFileName().toString().replace(".jar", "") + 
                   "-v" + LEGACYLINK_VERSION + 
                   "-" + System.currentTimeMillis();
        }
    }
    
    /**
     * Compute SHA-256 hash of a file.
     */
    private static String sha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        
        try (InputStream is = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        
        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        
        return sb.toString();
    }
    
    /**
     * Get the LegacyLink version used in cache keys.
     */
    public static String getVersion() {
        return LEGACYLINK_VERSION;
    }
}
