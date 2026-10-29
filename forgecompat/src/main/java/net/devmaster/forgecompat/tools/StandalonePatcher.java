package net.devmaster.forgecompat.tools;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;
import java.util.zip.*;

/**
 * Standalone mod patcher that can be run from the command line.
 * 
 * Usage: java -jar forgecompat-patcher.jar [mods_directory]
 * 
 * This patches 1.19.2 mod JARs to accept Minecraft 1.20.1,
 * allowing them to work with ForgeCompat.
 */
public class StandalonePatcher {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("ForgeCompat Mod Patcher");
        System.out.println("========================================");
        System.out.println();
        System.out.println("This tool patches 1.19.2 mods to work with");
        System.out.println("ForgeCompat on Minecraft 1.20.1");
        System.out.println();
        
        // Get mods directory
        Path modsDir;
        if (args.length > 0) {
            modsDir = Paths.get(args[0]);
        } else {
            // Default to current directory
            modsDir = Paths.get(".");
        }
        
        if (!Files.exists(modsDir)) {
            System.err.println("Error: Directory not found: " + modsDir.toAbsolutePath());
            System.exit(1);
        }
        
        System.out.println("Scanning for 1.19.2 mods in: " + modsDir.toAbsolutePath());
        System.out.println();
        
        try {
            // Find all JAR files
            List<Path> jarFiles = new ArrayList<>();
            Files.list(modsDir)
                .filter(path -> path.toString().endsWith(".jar"))
                .filter(path -> !path.getFileName().toString().contains("forgecompat"))
                .forEach(jarFiles::add);
            
            if (jarFiles.isEmpty()) {
                System.out.println("No JAR files found in directory.");
                System.out.println();
                System.out.println("Make sure you're running this from your mods folder,");
                System.out.println("or specify the path: java -jar forgecompat-patcher.jar /path/to/mods");
                System.exit(0);
            }
            
            System.out.println("Found " + jarFiles.size() + " JAR file(s)");
            System.out.println();
            
            // Patch each JAR
            int patchedCount = 0;
            for (Path jarFile : jarFiles) {
                if (patchJar(jarFile)) {
                    patchedCount++;
                }
            }
            
            System.out.println();
            System.out.println("========================================");
            System.out.println("Patched " + patchedCount + " mod(s)");
            System.out.println("========================================");
            
            if (patchedCount > 0) {
                System.out.println();
                System.out.println("Success! You can now launch Minecraft 1.20.1");
                System.out.println("with ForgeCompat and your patched mods.");
            } else {
                System.out.println();
                System.out.println("No 1.19.2 mods found to patch.");
                System.out.println("All mods in this folder are already compatible.");
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static boolean patchJar(Path jarPath) {
        try {
            // Check if it's a valid ZIP/JAR
            if (!isZipFile(jarPath)) {
                return false;
            }
            
            try (JarFile jarFile = new JarFile(jarPath.toFile())) {
                // Look for mods.toml
                JarEntry modsTomlEntry = null;
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().endsWith("mods.toml")) {
                        modsTomlEntry = entry;
                        break;
                    }
                }
                
                if (modsTomlEntry == null) {
                    return false; // Not a Forge mod
                }
                
                // Read mods.toml content
                String content;
                try (InputStream is = jarFile.getInputStream(modsTomlEntry);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    content = sb.toString();
                }
                
                // Check if it's a 1.19.2 mod
                if (!content.contains("1.19.2") && !content.contains("[1.19")) {
                    return false;
                }
                
                // Patch the content
                String patchedContent = patchMinecraftVersionRange(content);
                
                if (patchedContent.equals(content)) {
                    System.out.println("  [SKIP] " + jarPath.getFileName() + " - no changes needed");
                    return false;
                }
                
                // Write the patched JAR
                writePatchedJar(jarPath, modsTomlEntry.getName(), patchedContent);
                
                System.out.println("  [PATCHED] " + jarPath.getFileName());
                return true;
                
            }
        } catch (Exception e) {
            System.err.println("  [ERROR] " + jarPath.getFileName() + ": " + e.getMessage());
            return false;
        }
    }
    
    private static boolean isZipFile(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            byte[] signature = new byte[4];
            if (is.read(signature) != 4) return false;
            return signature[0] == 0x50 && signature[1] == 0x4B && 
                   signature[2] == 0x03 && signature[3] == 0x04;
        } catch (Exception e) {
            return false;
        }
    }
    
    private static String patchMinecraftVersionRange(String content) {
        String patched = content;
        
        // Pattern 1: versionRange="[1.19.2,1.20)" → "[1.19.2,1.21)"
        patched = patched.replaceAll(
            "versionRange\\s*=\\s*\"\\[1\\.19\\.2,1\\.20\\)\"",
            "versionRange=\"[1.19.2,1.21)\""
        );
        
        // Pattern 2: versionRange="[1.19,1.20)" → "[1.19,1.21)"
        patched = patched.replaceAll(
            "versionRange\\s*=\\s*\"\\[1\\.19,1\\.20\\)\"",
            "versionRange=\"[1.19,1.21)\""
        );
        
        // Pattern 3: versionRange="1.19.2" → "[1.19.2,1.21)"
        patched = patched.replaceAll(
            "versionRange\\s*=\\s*\"1\\.19\\.2\"",
            "versionRange=\"[1.19.2,1.21)\""
        );
        
        // Pattern 4: versionRange="[1.19.2]" → "[1.19.2,1.21)"
        patched = patched.replaceAll(
            "versionRange\\s*=\\s*\"\\[1\\.19\\.2\\]\"",
            "versionRange=\"[1.19.2,1.21)\""
        );
        
        return patched;
    }
    
    private static void writePatchedJar(Path originalJar, String modsTomlPath, String newContent) throws IOException {
        Path tempJar = originalJar.resolveSibling(originalJar.getFileName() + ".tmp");
        
        try (JarFile jarFile = new JarFile(originalJar.toFile());
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(tempJar.toFile()))) {
            
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                
                if (entry.getName().equals(modsTomlPath)) {
                    // Write patched mods.toml
                    JarEntry newEntry = new JarEntry(entry.getName());
                    jos.putNextEntry(newEntry);
                    jos.write(newContent.getBytes());
                    jos.closeEntry();
                } else {
                    // Copy other entries as-is
                    jos.putNextEntry(new JarEntry(entry.getName()));
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = is.read(buffer)) > 0) {
                            jos.write(buffer, 0, len);
                        }
                    }
                    jos.closeEntry();
                }
            }
        }
        
        // Replace original with patched
        Files.delete(originalJar);
        Files.move(tempJar, originalJar);
    }
}
