package net.devmaster.legacylink.transformer;

import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.Target;
import net.devmaster.legacylink.LegacyLink;
import net.devmaster.legacylink.detector.ModDetector;
import org.objectweb.asm.tree.ClassNode;

import java.nio.file.Path;
import java.util.*;

/**
 * LegacyLink Transformation Service
 * 
 * This service registers the LegacyLinkTransformer with Forge's ModLauncher,
 * allowing bytecode transformation without requiring a CoreMod JS file.
 */
public class LegacyLinkTransformationService implements ITransformationService {
    
    private static LegacyLinkTransformationService instance;
    
    private ModDetector detector;
    private Set<String> modsToTransform = new HashSet<>();
    private Set<Path> runtimeFallbackMods = new HashSet<>();
    
    public LegacyLinkTransformationService() {
        instance = this;
    }
    
    /**
     * Get the singleton instance.
     */
    public static LegacyLinkTransformationService getInstance() {
        return instance;
    }
    
    /**
     * Enable runtime transformation fallback for a specific mod.
     * This is used when pre-translation fails.
     * 
     * @param modJar Path to the mod JAR
     */
    public void enableRuntimeFallback(Path modJar) {
        runtimeFallbackMods.add(modJar);
        LegacyLink.LOGGER.info("[LegacyLink] Runtime fallback enabled for: {}", modJar.getFileName());
    }
    
    /**
     * Check if runtime fallback is enabled for a mod.
     */
    public boolean isRuntimeFallbackEnabled(Path modJar) {
        return runtimeFallbackMods.contains(modJar);
    }
    
    @Override
    public String name() {
        return "legacylink";
    }
    
    @Override
    public void onInitialize(IEnvironment environment) {
        LegacyLink.LOGGER.info("[LegacyLink] Transformation service initializing");
        detector = ModDetector.getInstance();
    }
    
    @Override
    public void onLoad(IEnvironment env, Set<String> otherTargets) {
        LegacyLink.LOGGER.info("[LegacyLink] Transformation service loaded");
        // Scan mods folder to determine which mods need translation
        scanMods();
    }
    
    /**
     * Scan mods folder to determine which mods need translation
     */
    private void scanMods() {
        try {
            Path modsDir = net.minecraftforge.fml.loading.FMLLoader.getGamePath().resolve("mods");
            if (!java.nio.file.Files.exists(modsDir)) {
                LegacyLink.LOGGER.warn("[LegacyLink] Mods directory not found");
                return;
            }
            
            try (var stream = java.nio.file.Files.list(modsDir)) {
                stream.filter(p -> p.toString().endsWith(".jar"))
                      .forEach(jarPath -> {
                          if (detector.shouldTranslate(jarPath)) {
                              String modId = getModIdFromJar(jarPath);
                              if (modId != null) {
                                  modsToTransform.add(modId);
                                  LegacyLink.LOGGER.info("[LegacyLink] Will transform mod: {}", modId);
                              }
                          }
                      });
            }
            
            LegacyLink.LOGGER.info("[LegacyLink] Found {} mods to transform", modsToTransform.size());
            
        } catch (Exception e) {
            LegacyLink.LOGGER.error("[LegacyLink] Error scanning mods", e);
        }
    }
    
    /**
     * Extract mod ID from a JAR file
     */
    private String getModIdFromJar(java.nio.file.Path jarPath) {
        try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(jarPath.toFile())) {
            java.util.jar.JarEntry modsToml = jarFile.getJarEntry("META-INF/mods.toml");
            if (modsToml == null) return null;
            
            String content;
            try (java.io.InputStream is = jarFile.getInputStream(modsToml)) {
                content = new String(is.readAllBytes());
            }
            
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("modId\\s*=\\s*\"([^\"]+)\"");
            java.util.regex.Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            LegacyLink.LOGGER.debug("[LegacyLink] Error reading mod ID from {}", jarPath, e);
        }
        return null;
    }
    
    @Override
    public List<ITransformer<?>> transformers() {
        LegacyLink.LOGGER.info("[LegacyLink] Registering class transformers");
        
        // Return a transformer that applies LegacyLinkTransformer to classes from mods that need it
        return Collections.singletonList(new ITransformer<ClassNode>() {
            @Override
            public ClassNode transform(ClassNode input, ITransformer.Context context) {
                String className = input.name;
                
                // Check if this class should be transformed
                if (!LegacyLinkTransformer.shouldTransform(className)) {
                    return input;
                }
                
                // Check if this class belongs to a mod that needs transformation
                if (!shouldTransformClass(className)) {
                    return input;
                }
                
                LegacyLink.LOGGER.debug("[LegacyLink] Transforming class via service: {}", className);
                
                try {
                    return LegacyLinkTransformer.transform(className, input);
                } catch (Exception e) {
                    LegacyLink.LOGGER.error("[LegacyLink] Error transforming class: {}", className, e);
                    return input;
                }
            }
            
            @Override
            public Set<Target> targets() {
                // Transform all classes - we'll filter in the transform method
                return Collections.singleton(Target.targetClass(""));
            }
        });
    }
    
    /**
     * Check if a class belongs to a mod that needs transformation
     */
    private boolean shouldTransformClass(String className) {
        // Extract package name (mod ID is usually the first package component)
        int firstSlash = className.indexOf('/');
        if (firstSlash > 0) {
            String packageName = className.substring(0, firstSlash);
            // Check if this package matches any mod we want to transform
            // This is a heuristic - mod packages often match their mod ID
            return modsToTransform.stream().anyMatch(modId -> 
                packageName.contains(modId) || modId.contains(packageName)
            );
        }
        return false;
    }
}
