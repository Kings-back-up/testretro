package net.devmaster.legacylink;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LegacyLink - A compatibility layer for running 1.19.2 Forge mods on 1.20.1.
 * 
 * This mod provides:
 * 1. A CoreMod transformer that rewrites target mod bytecode at load time
 * 2. Runtime adapters for behavioral differences
 * 
 * Usage: Place this mod in the mods folder alongside a 1.19.2 target mod.
 * The transformer will automatically detect and patch supported mods.
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
        LOGGER.info("Pure bytecode translation - no shims");
        LOGGER.info("========================================");
        
        // Initialize the runtime transformer
        net.devmaster.legacylink.transformer.LegacyLinkRuntimeTransformer.initialize();
        
        // Register event handlers for runtime adaptation
        modEventBus.addListener(this::onCommonSetup);
        forgeEventBus.register(new RuntimeAdapter());
        
        LOGGER.info("LegacyLink initialized successfully");
    }
    
    private void onCommonSetup(final net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        LOGGER.info("LegacyLink common setup - translation layer active");
    }
}
