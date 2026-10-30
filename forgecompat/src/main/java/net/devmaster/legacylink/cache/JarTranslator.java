package net.devmaster.legacylink.cache;

import net.devmaster.legacylink.LegacyLink;
import net.devmaster.legacylink.transformer.LegacyLinkTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.nio.file.*;
import java.util.jar.*;
import java.util.zip.*;

/**
 * Translates a 1.19.2 mod JAR to 1.20.1 by transforming all classes
 * and patching metadata.
 */
public class JarTranslator {
    
    /**
     * Translate a mod JAR from 1.19.2 to 1.20.1.
     * 
     * @param sourceJar Path to the original 1.19.2 mod JAR
     * @param outputDir Directory to write the translated JAR
     * @return Path to the translated JAR
     */
    public static Path translateJar(Path sourceJar, Path outputDir) throws IOException {
        LegacyLink.LOGGER.info("[LegacyLink] Translating JAR: {}", sourceJar.getFileName());
        
        // Create temp directory for extraction
        Path tempDir = Files.createTempDirectory("legacylink-translate-");
        
        try {
            // Extract JAR
            extractJar(sourceJar, tempDir);
            
            // Transform all classes
            int transformedCount = transformClasses(tempDir);
            LegacyLink.LOGGER.info("[LegacyLink] Transformed {} classes", transformedCount);
            
            // Patch metadata (mods.toml)
            boolean patched = patchMetadata(tempDir);
            if (patched) {
                LegacyLink.LOGGER.info("[LegacyLink] Patched metadata");
            }
            
            // Repackage to output directory
            String outputName = sourceJar.getFileName().toString().replace(".jar", "-translated.jar");
            Path outputJar = outputDir.resolve(outputName);
            repackageJar(tempDir, outputJar);
            
            LegacyLink.LOGGER.info("[LegacyLink] Translation complete: {}", outputJar.getFileName());
            return outputJar;
            
        } finally {
            // Clean up temp directory
            deleteDirectory(tempDir);
        }
    }
    
    /**
     * Patch a 1.19.2 mod JAR in place to relax version requirements.
     * This modifies the original JAR file.
     * 
     * @param jarPath Path to the JAR to patch
     */
    public static void patchJarInPlace(Path jarPath) throws IOException {
        LegacyLink.LOGGER.info("[LegacyLink] Patching JAR in place: {}", jarPath.getFileName());
        
        // Create temp directory for extraction
        Path tempDir = Files.createTempDirectory("legacylink-patch-");
        
        try {
            // Extract JAR
            extractJar(jarPath, tempDir);
            
            // Patch metadata (mods.toml)
            boolean patched = patchMetadata(tempDir);
            
            if (patched) {
                // Repackage back to original location
                repackageJar(tempDir, jarPath);
                LegacyLink.LOGGER.info("[LegacyLink] Successfully patched: {}", jarPath.getFileName());
            } else {
                LegacyLink.LOGGER.warn("[LegacyLink] No metadata to patch in: {}", jarPath.getFileName());
            }
            
        } finally {
            // Clean up temp directory
            deleteDirectory(tempDir);
        }
    }
    
    /**
     * Extract a JAR file to a directory.
     */
    private static void extractJar(Path jarPath, Path outputDir) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            var entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                Path outputPath = outputDir.resolve(entry.getName());
                
                // Security check: prevent zip slip
                if (!outputPath.normalize().startsWith(outputDir.normalize())) {
                    throw new IOException("Bad zip entry: " + entry.getName());
                }
                
                if (entry.isDirectory()) {
                    Files.createDirectories(outputPath);
                } else {
                    Files.createDirectories(outputPath.getParent());
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        Files.copy(is, outputPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }
    
    /**
     * Transform all .class files in a directory.
     * 
     * @return Number of classes transformed
     */
    private static int transformClasses(Path dir) throws IOException {
        int[] count = {0};
        
        Files.walk(dir)
            .filter(path -> path.toString().endsWith(".class"))
            .forEach(classFile -> {
                try {
                    if (transformClass(classFile)) {
                        count[0]++;
                    }
                } catch (Exception e) {
                    LegacyLink.LOGGER.error("[LegacyLink] Failed to transform class: {}", classFile, e);
                }
            });
        
        return count[0];
    }
    
    /**
     * Transform a single class file.
     * 
     * @return true if the class was transformed, false otherwise
     */
    private static boolean transformClass(Path classFile) throws IOException {
        // Read class bytes
        byte[] originalBytes = Files.readAllBytes(classFile);
        
        // Parse with ASM
        ClassReader reader = new ClassReader(originalBytes);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);
        
        // Check if this class should be transformed
        if (!LegacyLinkTransformer.shouldTransform(classNode.name)) {
            return false;
        }
        
        // Transform the class
        ClassNode transformedNode = LegacyLinkTransformer.transform(classNode.name, classNode);
        
        // Check if anything changed
        if (transformedNode == classNode) {
            return false; // No transformation needed
        }
        
        // Write back
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        transformedNode.accept(writer);
        byte[] transformedBytes = writer.toByteArray();
        
        // Write transformed bytes
        Files.write(classFile, transformedBytes);
        
        return true;
    }
    
    /**
     * Patch metadata files (mods.toml) to relax version requirements.
     * 
     * @return true if metadata was patched, false otherwise
     */
    private static boolean patchMetadata(Path dir) throws IOException {
        Path modsToml = dir.resolve("META-INF").resolve("mods.toml");
        
        if (!Files.exists(modsToml)) {
            LegacyLink.LOGGER.debug("[LegacyLink] No mods.toml found, skipping metadata patch");
            return false;
        }
        
        String content = Files.readString(modsToml);
        String patched = patchModsToml(content);
        
        if (!patched.equals(content)) {
            Files.writeString(modsToml, patched);
            LegacyLink.LOGGER.info("[LegacyLink] Patched mods.toml");
            return true;
        }
        
        return false;
    }
    
    /**
     * Patch mods.toml content to relax Minecraft version requirements.
     */
    private static String patchModsToml(String content) {
        // Replace version ranges that exclude 1.20.1
        // Pattern: versionRange="[1.19.2,1.20)" -> versionRange="[1.19.2,1.21)"
        String patched = content;
        
        // Handle various version range patterns
        patched = patched.replaceAll(
            "versionRange\\s*=\\s*\"\\[1\\.19[^,]*,\\s*1\\.20\\)\"",
            "versionRange=\"[1.19,1.21)\""
        );
        
        patched = patched.replaceAll(
            "versionRange\\s*=\\s*\"\\[1\\.19[^,]*,\\s*1\\.20\\]\"",
            "versionRange=\"[1.19,1.21)\""
        );
        
        patched = patched.replaceAll(
            "versionRange\\s*=\\s*\"1\\.19\\.2\"",
            "versionRange=\"[1.19.2,1.21)\""
        );
        
        patched = patched.replaceAll(
            "versionRange\\s*=\\s*\"\\[1\\.19\\.2\\]\"",
            "versionRange=\"[1.19.2,1.21)\""
        );
        
        return patched;
    }
    
    /**
     * Repackage a directory back into a JAR file.
     */
    private static void repackageJar(Path sourceDir, Path outputJar) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(
                new BufferedOutputStream(Files.newOutputStream(outputJar)))) {
            
            Files.walk(sourceDir)
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    try {
                        String entryName = sourceDir.relativize(file).toString()
                            .replace(File.separator, "/");
                        
                        JarEntry entry = new JarEntry(entryName);
                        jos.putNextEntry(entry);
                        
                        Files.copy(file, jos);
                        jos.closeEntry();
                        
                    } catch (IOException e) {
                        LegacyLink.LOGGER.error("[LegacyLink] Failed to add file to JAR: {}", file, e);
                    }
                });
        }
    }
    
    /**
     * Recursively delete a directory.
     */
    private static void deleteDirectory(Path dir) {
        try {
            if (Files.exists(dir)) {
                Files.walk(dir)
                    .sorted((a, b) -> b.compareTo(a)) // Reverse order (files first, then dirs)
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // Ignore
                        }
                    });
            }
        } catch (IOException e) {
            LegacyLink.LOGGER.error("[LegacyLink] Failed to delete directory: {}", dir, e);
        }
    }
}
