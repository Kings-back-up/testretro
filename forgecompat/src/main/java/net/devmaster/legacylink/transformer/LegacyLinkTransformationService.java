package net.devmaster.legacylink.transformer;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import net.devmaster.legacylink.LegacyLink;
import net.devmaster.legacylink.cache.JarTranslator;
import net.devmaster.legacylink.detector.ModDetector;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * LegacyLink Transformation Service
 * 
 * This service patches 1.19.2 mods BEFORE Forge validates dependencies.
 * By doing this in beginScanning(), we can modify the JARs before Forge
 * reads their mods.toml files, eliminating the need for a restart.
 */
public class LegacyLinkTransformationService implements ITransformationService {
    
    private ModDetector detector;
    
    @Override
    public String name() {
        return "legacylink";
    }
    
    @Override
    public void initialize(IEnvironment environment) {
        LegacyLink.LOGGER.info("[LegacyLink] Transformation service initializing");
        detector = ModDetector.getInstance();
    }
    
    @Override
    public void onLoad(IEnvironment env, Set<String> otherTargets) {
        LegacyLink.LOGGER.info("[LegacyLink] Transformation service loaded");
    }
    
    /**
     * This is called BEFORE Forge scans and validates mods.
     * We patch all 1.19.2 mods here so Forge sees them as compatible.
     */
    @Override
    public void onBeginScan(IEnvironment env, Set<String> otherTargets) {
        LegacyLink.LOGGER.info("[LegacyLink] Beginning scan - patching 1.19.2 mods...");
        
        try {
            Path modsDir = FMLLoader.getGamePath().resolve("mods");
            
            if (!Files.exists(modsDir)) {
                LegacyLink.LOGGER.warn("[LegacyLink] Mods directory not found");
                return;
            }
            
            int patchedCount = 0;
            
            try (var stream = Files.list(modsDir)) {
                List<Path> jars = stream
                    .filter(p -> p.toString().endsWith(".jar"))
                    .filter(p -> !p.getFileName().toString().contains("legacylink"))
                    .toList();
                
                for (Path jarPath : jars) {
                    try {
                        if (detector.needsTranslation(jarPath)) {
                            LegacyLink.LOGGER.info("[LegacyLink] Patching mod: {}", jarPath.getFileName());
                            
                            // Patch the JAR in place
                            JarTranslator.patchJarInPlace(jarPath);
                            patchedCount++;
                            
                            LegacyLink.LOGGER.info("[LegacyLink] Successfully patched: {}", jarPath.getFileName());
                        }
                    } catch (Exception e) {
                        LegacyLink.LOGGER.error("[LegacyLink] Failed to patch mod: {}", jarPath.getFileName(), e);
                    }
                }
            }
            
            LegacyLink.LOGGER.info("[LegacyLink] Patched {} mod(s) - no restart required!", patchedCount);
            
        } catch (Exception e) {
            LegacyLink.LOGGER.error("[LegacyLink] Error during onBeginScan", e);
        }
    }
    
    @Override
    public List<ITransformer<?>> transformers() {
        // Return empty list - we don't need class transformers
        // All the work is done in onBeginScan()
        return Collections.emptyList();
    }
}
