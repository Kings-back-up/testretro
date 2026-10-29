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
 * LegacyLink - A hybrid compatibility layer for running 1.19.2 Forge mods on 1.20.1.
 * 
 * This mod uses a hybrid approach combining:
 * 1. Pre-translation to cache (Retromod-style) - for reliability and performance
 * 2. Runtime transformation fallback (our approach) - for edge cases
 * 3. Selective isolation (Retrofit-inspired) - for deep incompatibilities (future)
 * 
 * Usage: Place this mod in the mods folder alongside 1.19.2 target mods.
 * The system will automatically detect, translate, and cache compatible mods.
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
        LOGGER.info("Hybrid approach: cache + runtime fallback");
        LOGGER.info("========================================");
        
        // Initialize hybrid loader
        initializeHybridLoader();
        
        // Register event handlers for runtime adaptation
        modEventBus.addListener(this::onCommonSetup);
        forgeEventBus.register(new RuntimeAdapter());
        
        LOGGER.info("LegacyLink initialized successfully");
    }
    
    /**
     * Initialize the hybrid loader system.
     * This processes all mods in the mods folder and pre-translates them to cache.
     */
    private void initializeHybridLoader() {
        try {
            Path modsDir = FMLLoader.getGamePath().resolve("mods");
            HybridLoader loader = HybridLoader.getInstance();
            
            LOGGER.info("[LegacyLink] Starting hybrid loader...");
            loader.processMods(modsDir);
            
            LOGGER.info("[LegacyLink] Hybrid loader complete. {} mods translated.", 
                loader.getTranslatedCount());
                
        } catch (Exception e) {
            LOGGER.error("[LegacyLink] Failed to initialize hybrid loader", e);
        }
    }
    
    private void onCommonSetup(final net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        LOGGER.info("LegacyLink common setup - hybrid translation layer active");
    }
}
