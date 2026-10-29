package net.devmaster.legacylink;

import net.devmaster.legacylink.cache.HybridLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * LegacyLink - A compatibility layer for running 1.19.2 Forge mods on 1.20.1.
 * 
 * This mod patches 1.19.2 mods in place to make them compatible with 1.20.1.
 * On first run, it will patch all 1.19.2 mods and ask you to restart.
 * On subsequent runs, the patched mods will load normally.
 */
@Mod(LegacyLink.MOD_ID)
public class LegacyLink {
    public static final String MOD_ID = "legacylink";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    public LegacyLink() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
        
        LOGGER.info("========================================");
        LOGGER.info("LegacyLink v0.1.0 initializing");
        LOGGER.info("Bridging 1.19.2 Forge mods to 1.20.1");
        LOGGER.info("========================================");
        
        // Patch mods in place
        patchModsInPlace();
        
        // Register event handlers
        modEventBus.addListener(this::onCommonSetup);
        forgeEventBus.register(new RuntimeAdapter());
        
        LOGGER.info("LegacyLink initialized successfully");
    }
    
    /**
     * Patch all 1.19.2 mods in place.
     * This modifies the original JARs to relax version requirements.
     */
    private void patchModsInPlace() {
        try {
            Path modsDir = FMLLoader.getGamePath().resolve("mods");
            HybridLoader loader = HybridLoader.getInstance();
            
            LOGGER.info("[LegacyLink] Scanning mods folder...");
            boolean patched = loader.patchModsInPlace(modsDir);
            
            if (patched) {
                LOGGER.error("========================================");
                LOGGER.error("LegacyLink has patched 1.19.2 mods!");
                LOGGER.error("Please RESTART Minecraft to load them.");
                LOGGER.error("========================================");
                
                // Throw an exception to stop the game and force restart
                throw new RuntimeException("LegacyLink: Please restart Minecraft to load patched mods");
            } else {
                LOGGER.info("[LegacyLink] No mods needed patching");
            }
                
        } catch (RuntimeException e) {
            // Re-throw our restart exception
            throw e;
        } catch (Exception e) {
            LOGGER.error("[LegacyLink] Failed to patch mods", e);
        }
    }
    
    private void onCommonSetup(final net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        LOGGER.info("LegacyLink common setup - translation layer active");
    }
}
