package net.devmaster.forgecompat;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Runtime adapter for behavioral differences that can't be resolved
 * at bytecode translation time.
 * 
 * Most translation happens in ForgeCompatTransformer (compile-time).
 * This class handles the few remaining cases:
 * - Logging what's being translated
 * - Any event timing differences
 * - Registry initialization order fixes if needed
 */
@Mod.EventBusSubscriber(modid = ForgeCompat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RuntimeAdapter {
    
    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        ForgeCompat.LOGGER.info("ForgeCompat translator active — rewriting 1.19.2 bytecode to 1.20.1");
    }
}
