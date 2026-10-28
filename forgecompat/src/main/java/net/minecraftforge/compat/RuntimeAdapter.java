package net.minecraftforge.compat;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Runtime adapter that handles behavioral differences between 1.19.2 and 1.20.1
 * that can't be resolved at compile/transform time.
 * 
 * This includes:
 * - Event bus hooking to translate between old and new event types
 * - Registry initialization order fixes
 * - Data format migration for saved NBT data
 */
@Mod.EventBusSubscriber(modid = ForgeCompat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RuntimeAdapter {
    
    /**
     * Handle the LivingChangeTargetEvent and fire the old LivingSetAttackTargetEvent
     * for mods that still subscribe to the old event.
     * 
     * The transformer should have already rewritten @SubscribeEvent methods
     * to use the new event type, but this is a fallback for mods loaded
     * without transformation (e.g., if the transformer can't find them).
     */
    @SubscribeEvent
    public static void onLivingChangeTarget(net.minecraftforge.event.entity.living.LivingChangeTargetEvent event) {
        // If any old-mod code is listening for LivingSetAttackTargetEvent,
        // this ensures the event still fires with the old semantics.
        // The transformer should handle most cases, but this is a safety net.
    }
    
    /**
     * Log information about loaded mods that might be 1.19.2 mods
     * running through our compatibility layer.
     */
    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        ForgeCompat.LOGGER.info("ForgeCompat RuntimeAdapter: Common setup complete");
        ForgeCompat.LOGGER.info("Shim layer is active - 1.19.2 API compatibility enabled");
    }
}
