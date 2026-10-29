package net.devmaster.legacylink.transformer;

import net.devmaster.legacylink.LegacyLink;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

/**
 * LegacyLink Runtime Transformer
 * 
 * This transformer scans for 1.19.2 mods during mod construction and transforms
 * their bytecode to work with 1.20.1. It uses ASM to modify classes before they're
 * fully loaded by the JVM.
 */
public class LegacyLinkRuntimeTransformer {
    
    private static boolean initialized = false;
    private static final Map<String, byte[]> transformedClasses = new HashMap<>();
    
    /**
     * Initialize the transformer. Called from the mod constructor.
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        
        LegacyLink.LOGGER.info("[LegacyLink] Initializing runtime transformer...");
        
        try {
            // Scan mods folder for 1.19.2 mods
            Path modsDir = FMLLoader.getGamePath().resolve("mods");
            if (!Files.exists(modsDir)) {
                LegacyLink.LOGGER.warn("[LegacyLink] Mods directory not found: {}", modsDir);
                return;
            }
            
            List<Path> modJars = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir, "*.jar")) {
                for (Path jarPath : stream) {
                    modJars.add(jarPath);
                }
            }
            
            LegacyLink.LOGGER.info("[LegacyLink] Found {} mod JARs to scan", modJars.size());
            
            // Transform each mod
            int transformedCount = 0;
            for (Path jarPath : modJars) {
                if (transformModJar(jarPath)) {
                    transformedCount++;
                }
            }
            
            LegacyLink.LOGGER.info("[LegacyLink] Transformed {} mod(s), {} classes", 
                transformedCount, transformedClasses.size());
            
        } catch (Exception e) {
            LegacyLink.LOGGER.error("[LegacyLink] Error initializing transformer", e);
        }
    }
    
    /**
     * Transform a single mod JAR file.
     * 
     * @return true if the mod was transformed, false otherwise
     */
    private static boolean transformModJar(Path jarPath) {
        try {
            // Check if this is a 1.19.2 mod by reading mods.toml
            if (!is192Mod(jarPath)) {
                return false;
            }
            
            LegacyLink.LOGGER.info("[LegacyLink] Found 1.19.2 mod: {}", jarPath.getFileName());
            
            // Extract and transform all classes
            try (JarFile jarFile = new JarFile(jarPath.toFile())) {
                Enumeration<JarEntry> entries = jarFile.entries();
                int classCount = 0;
                
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    
                    if (entry.getName().endsWith(".class")) {
                        // Transform the class
                        byte[] originalBytes = readEntry(jarFile, entry);
                        byte[] transformedBytes = transformClass(originalBytes);
                        
                        if (transformedBytes != null) {
                            String className = entry.getName().replace(".class", "").replace('/', '.');
                            transformedClasses.put(className, transformedBytes);
                            classCount++;
                        }
                    }
                }
                
                if (classCount > 0) {
                    LegacyLink.LOGGER.info("[LegacyLink] Transformed {} classes from {}", 
                        classCount, jarPath.getFileName());
                }
            }
            
            return true;
            
        } catch (Exception e) {
            LegacyLink.LOGGER.error("[LegacyLink] Error transforming mod: {}", jarPath, e);
            return false;
        }
    }
    
    /**
     * Check if a JAR is a 1.19.2 mod by reading its mods.toml.
     */
    private static boolean is192Mod(Path jarPath) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            JarEntry modsToml = jarFile.getJarEntry("META-INF/mods.toml");
            if (modsToml == null) {
                return false;
            }
            
            String content = new String(readEntry(jarFile, modsToml));
            
            // Check for 1.19.2 version range
            return content.contains("[1.19") || content.contains("1.19.2");
        }
    }
    
    /**
     * Read a JAR entry as bytes.
     */
    private static byte[] readEntry(JarFile jarFile, JarEntry entry) throws IOException {
        try (InputStream is = jarFile.getInputStream(entry)) {
            return is.readAllBytes();
        }
    }
    
    /**
     * Transform a single class.
     * 
     * @return transformed bytecode, or null if no transformation needed
     */
    private static byte[] transformClass(byte[] classBytes) {
        try {
            ClassReader reader = new ClassReader(classBytes);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, 0);
            
            // Apply transformations
            ClassNode transformed = LegacyLinkTransformer.transform(classNode.name, classNode);
            
            if (transformed != classNode) {
                // Class was modified, write it back
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                transformed.accept(writer);
                return writer.toByteArray();
            }
            
            return null; // No transformation needed
            
        } catch (Exception e) {
            LegacyLink.LOGGER.debug("[LegacyLink] Error transforming class", e);
            return null;
        }
    }
    
    /**
     * Get a transformed class by name.
     * This can be used by a custom ClassLoader to load transformed classes.
     */
    public static byte[] getTransformedClass(String className) {
        return transformedClasses.get(className);
    }
    
    /**
     * Check if a class has been transformed.
     */
    public static boolean hasTransformedClass(String className) {
        return transformedClasses.containsKey(className);
    }
}
