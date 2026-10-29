package net.devmaster.legacylink.shim;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import java.util.function.Supplier;

/**
 * Compatibility shim for CreativeModeTab constructor changes.
 * 
 * In 1.19.2: new CreativeModeTab("tab_name") { makeIcon() { ... } }
 * In 1.20.1: CreativeModeTab.builder().title(...).icon(...).build()
 * 
 * This class provides a wrapper that accepts the old constructor pattern.
 */
public abstract class CreativeModeTabCompat {
    
    protected final String tabName;
    private CreativeModeTab internalTab;
    
    /**
     * Old 1.19.2 constructor signature.
     * Subclasses call this with their tab name.
     */
    protected CreativeModeTabCompat(String tabName) {
        this.tabName = tabName;
        this.internalTab = buildTab();
    }
    
    /**
     * Build the actual CreativeModeTab using the new 1.20.1 API.
     * This is called from the constructor.
     */
    private CreativeModeTab buildTab() {
        return CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + tabName))
            .icon(this::makeIcon)
            .build();
    }
    
    /**
     * Old 1.19.2 method for creating the tab icon.
     * Subclasses override this to provide their icon.
     */
    public abstract ItemStack makeIcon();
    
    /**
     * Get the internal CreativeModeTab instance.
     * Use this when registering the tab.
     */
    public CreativeModeTab getTab() {
        return internalTab;
    }
    
    /**
     * Implicit conversion helper - allows using this where CreativeModeTab is expected.
     */
    public static CreativeModeTab toTab(CreativeModeTabCompat compat) {
        return compat.getTab();
    }
}
