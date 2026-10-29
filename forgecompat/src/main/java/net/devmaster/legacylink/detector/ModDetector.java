package net.devmaster.legacylink.detector;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import net.devmaster.legacylink.LegacyLink;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects which mods need translation based on their version ranges.
 * 
 * Rules:
 * 1. Translate only if minecraft range excludes 1.20.1 and includes some 1.19.x
 * 2. If range contains 1.20.1, don't touch it
 * 3. For open-ended ranges like [1.19.2,), fall back to pack_format
 * 4. Support user overrides via legacylink.toml
 */
public class ModDetector {
    
    private static final Pattern VERSION_RANGE_PATTERN = Pattern.compile(
        "versionRange\\s*=\\s*\"([^\"]+)\""
    );
    
    private static final Pattern MOD_ID_PATTERN = Pattern.compile(
        "modId\\s*=\\s*\"([^\"]+)\""
    );
    
    private static final Pattern PACK_FORMAT_PATTERN = Pattern.compile(
        "\"pack_format\"\\s*:\\s*(\\d+)"
    );
    
    private static final ArtifactVersion MC_1_20_1 = new DefaultArtifactVersion("1.20.1");
    private static final ArtifactVersion MC_1_19_0 = new DefaultArtifactVersion("1.19.0");
    private static final ArtifactVersion MC_1_20_0 = new DefaultArtifactVersion("1.20.0");
    
    // Pack format thresholds
    private static final int PACK_FORMAT_1_19 = 9;  // 1.19.x
    private static final int PACK_FORMAT_1_20 = 15; // 1.20.x
    
    private Set<String> forceTranslate = new HashSet<>();
    private Set<String> skipTranslate = new HashSet<>();
    
    public ModDetector() {
        loadOverrides();
    }
    
    /**
     * Load user overrides from legacylink.toml
     */
    private void loadOverrides() {
        Path configPath = FMLLoader.getGamePath().resolve("config/legacylink.toml");
        if (!Files.exists(configPath)) {
            return;
        }
        
        try {
            FileConfig config = FileConfig.of(configPath);
            config.load();
            
            List<String> force = config.get("force");
            if (force != null) {
                forceTranslate.addAll(force);
            }
            
            List<String> skip = config.get("skip");
            if (skip != null) {
                skipTranslate.addAll(skip);
            }
            
            config.close();
            
            LegacyLink.LOGGER.info("[LegacyLink] Loaded overrides: force={}, skip={}", 
                forceTranslate, skipTranslate);
                
        } catch (Exception e) {
            LegacyLink.LOGGER.error("[LegacyLink] Error loading legacylink.toml", e);
        }
    }
    
    /**
     * Determine if a mod JAR should be translated.
     * 
     * @param jarPath Path to the mod JAR
     * @return true if the mod should be translated, false otherwise
     */
    public boolean shouldTranslate(Path jarPath) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            // Read mods.toml
            JarEntry modsTomlEntry = jarFile.getJarEntry("META-INF/mods.toml");
            if (modsTomlEntry == null) {
                return false;
            }
            
            String modsTomlContent;
            try (InputStream is = jarFile.getInputStream(modsTomlEntry)) {
                modsTomlContent = new String(is.readAllBytes());
            }
            
            // Extract mod ID
            String modId = extractModId(modsTomlContent);
            if (modId == null) {
                return false;
            }
            
            // Check user overrides first
            if (forceTranslate.contains(modId)) {
                LegacyLink.LOGGER.info("[LegacyLink] Mod {} forced to translate by user override", modId);
                return true;
            }
            if (skipTranslate.contains(modId)) {
                LegacyLink.LOGGER.info("[LegacyLink] Mod {} skipped by user override", modId);
                return false;
            }
            
            // Extract minecraft version range
            String versionRange = extractMinecraftVersionRange(modsTomlContent);
            if (versionRange == null) {
                LegacyLink.LOGGER.warn("[LegacyLink] Mod {} has no minecraft version range", modId);
                return false;
            }
            
            // Parse and analyze the version range
            return analyzeVersionRange(modId, versionRange, jarFile);
            
        } catch (Exception e) {
            LegacyLink.LOGGER.error("[LegacyLink] Error analyzing mod: {}", jarPath, e);
            return false;
        }
    }
    
    /**
     * Extract mod ID from mods.toml content
     */
    private String extractModId(String content) {
        Matcher matcher = MOD_ID_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * Extract minecraft version range from mods.toml content
     */
    private String extractMinecraftVersionRange(String content) {
        // Look for [[dependencies.modid]] section with modId="minecraft"
        String[] lines = content.split("\n");
        boolean inMinecraftDep = false;
        
        for (String line : lines) {
            if (line.contains("[[dependencies.")) {
                inMinecraftDep = false;
            }
            if (line.contains("modId=\"minecraft\"")) {
                inMinecraftDep = true;
            }
            if (inMinecraftDep) {
                Matcher matcher = VERSION_RANGE_PATTERN.matcher(line);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Analyze version range to determine if translation is needed
     */
    private boolean analyzeVersionRange(String modId, String versionRange, JarFile jarFile) {
        try {
            VersionRange range = VersionRange.createFromVersionSpec(versionRange);
            
            // Check if range contains 1.20.1
            if (range.containsVersion(MC_1_20_1)) {
                LegacyLink.LOGGER.info("[LegacyLink] Mod {} supports 1.20.1 natively, skipping", modId);
                return false;
            }
            
            // Check if range includes any 1.19.x
            boolean includes19 = range.containsVersion(MC_1_19_0) || 
                                 range.containsVersion(new DefaultArtifactVersion("1.19.2")) ||
                                 range.containsVersion(new DefaultArtifactVersion("1.19.4"));
            
            if (!includes19) {
                LegacyLink.LOGGER.info("[LegacyLink] Mod {} doesn't target 1.19.x, skipping", modId);
                return false;
            }
            
            // Check if range is open-ended (like [1.19.2,))
            // In this case, fall back to pack_format
            if (isOpenEndedRange(range)) {
                LegacyLink.LOGGER.info("[LegacyLink] Mod {} has open-ended range {}, checking pack_format", 
                    modId, versionRange);
                return checkPackFormat(modId, jarFile);
            }
            
            LegacyLink.LOGGER.info("[LegacyLink] Mod {} targets 1.19.x only, will translate", modId);
            return true;
            
        } catch (Exception e) {
            LegacyLink.LOGGER.error("[LegacyLink] Error parsing version range for {}: {}", modId, versionRange, e);
            return false;
        }
    }
    
    /**
     * Check if a version range is open-ended (no upper bound)
     */
    private boolean isOpenEndedRange(VersionRange range) {
        // Check if the range has no upper bound
        return range.getRecommendedVersion() == null || 
               range.getRestrictions().stream().anyMatch(r -> r.getUpperBound() == null);
    }
    
    /**
     * Check pack_format as fallback for open-ended ranges
     */
    private boolean checkPackFormat(String modId, JarFile jarFile) {
        try {
            // Look for pack.mcmeta
            JarEntry packMcmeta = jarFile.getJarEntry("pack.mcmeta");
            if (packMcmeta == null) {
                LegacyLink.LOGGER.warn("[LegacyLink] Mod {} has no pack.mcmeta, assuming 1.19.x", modId);
                return true;
            }
            
            String packContent;
            try (InputStream is = jarFile.getInputStream(packMcmeta)) {
                packContent = new String(is.readAllBytes());
            }
            
            Matcher matcher = PACK_FORMAT_PATTERN.matcher(packContent);
            if (matcher.find()) {
                int packFormat = Integer.parseInt(matcher.group(1));
                
                if (packFormat >= PACK_FORMAT_1_20) {
                    LegacyLink.LOGGER.info("[LegacyLink] Mod {} has pack_format {} (1.20+), skipping", 
                        modId, packFormat);
                    return false;
                } else if (packFormat >= PACK_FORMAT_1_19) {
                    LegacyLink.LOGGER.info("[LegacyLink] Mod {} has pack_format {} (1.19.x), will translate", 
                        modId, packFormat);
                    return true;
                } else {
                    LegacyLink.LOGGER.warn("[LegacyLink] Mod {} has unknown pack_format {}, assuming 1.19.x", 
                        modId, packFormat);
                    return true;
                }
            }
            
            LegacyLink.LOGGER.warn("[LegacyLink] Mod {} pack.mcmeta has no pack_format, assuming 1.19.x", modId);
            return true;
            
        } catch (Exception e) {
            LegacyLink.LOGGER.error("[LegacyLink] Error checking pack_format for {}", modId, e);
            return true; // Default to translating if we can't determine
        }
    }
}
