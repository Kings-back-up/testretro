package net.devmaster.legacylink.shim;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * Helper class for CreativeModeTab icon handling.
 * 
 * When the transformer rewrites a 1.19.2 CreativeModeTab(String) constructor
 * to use the 1.20.1 Builder pattern, this helper:
 * 1. Creates the Builder with the correct title
 * 2. Binds the icon supplier after construction so it can call makeIcon()
 */
public class CreativeModeTabIconHelper {
    
    // Map of pending icon suppliers - keyed by the CreativeModeTab instance
    private static final Map<CreativeModeTab, Supplier<ItemStack>> pendingIcons = new WeakHashMap<>();
    
    /**
     * Build a CreativeModeTab.Builder from a tab name.
     * Called by the transformer before the super() call.
     */
    public static CreativeModeTab.Builder buildBuilder(String tabName) {
        return CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + tabName));
    }
    
    /**
     * Bind the icon supplier to a CreativeModeTab instance.
     * Called by the transformer immediately after super() returns.
     * 
     * The icon supplier will call the makeIcon() method on the tab instance.
     */
    public static void bindPendingIcon(Object tabInstance) {
        if (tabInstance instanceof CreativeModeTab tab) {
            // The makeIcon() method will be called by the icon supplier
            // We need to use reflection or a callback to invoke it
            pendingIcons.put(tab, () -> {
                try {
                    // Try to call makeIcon() on the tab instance
                    java.lang.reflect.Method makeIconMethod = tab.getClass().getMethod("makeIcon");
                    return (ItemStack) makeIconMethod.invoke(tab);
                } catch (Exception e) {
                    // If makeIcon() doesn't exist or fails, return empty
                    return ItemStack.EMPTY;
                }
            });
        }
    }
    
    /**
     * Get the icon supplier for a tab.
     * This is called by the CreativeModeTab when it needs to display its icon.
     */
    public static Supplier<ItemStack> getIconSupplier(CreativeModeTab tab) {
        return pendingIcons.getOrDefault(tab, () -> ItemStack.EMPTY);
    }
}
