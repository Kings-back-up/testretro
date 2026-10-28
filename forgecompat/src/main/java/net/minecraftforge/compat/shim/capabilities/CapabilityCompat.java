package net.minecraftforge.compat.shim.capabilities;

import net.minecraftforge.common.ForgeCapabilities;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * Shim for the 1.19.2 capability constants.
 * 
 * In 1.19.2:
 *   CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
 *   CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
 *   CapabilityEnergy.ENERGY
 * 
 * In 1.20.1, these were consolidated into ForgeCapabilities:
 *   ForgeCapabilities.ITEM_HANDLER
 *   ForgeCapabilities.FLUID_HANDLER
 *   ForgeCapabilities.ENERGY
 * 
 * The transformer rewrites references from the old classes to these constants.
 */
public class CapabilityCompat {
    
    // ---- Item Handler ----
    
    /**
     * 1.19.2: CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
     * 1.20.1: ForgeCapabilities.ITEM_HANDLER
     */
    public static final Capability<IItemHandler> ITEM_HANDLER_CAPABILITY = 
        ForgeCapabilities.ITEM_HANDLER;
    
    // ---- Fluid Handler ----
    
    /**
     * 1.19.2: CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
     * 1.20.1: ForgeCapabilities.FLUID_HANDLER
     */
    public static final Capability<IFluidHandler> FLUID_HANDLER_CAPABILITY = 
        ForgeCapabilities.FLUID_HANDLER;
    
    /**
     * 1.19.2: CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY
     * 1.20.1: ForgeCapabilities.FLUID_HANDLER_ITEM
     */
    // Note: FLUID_HANDLER_ITEM may not exist in 1.20.1, handle gracefully
    // public static final Capability<IFluidHandlerItem> FLUID_HANDLER_ITEM_CAPABILITY = 
    //     ForgeCapabilities.FLUID_HANDLER_ITEM;
    
    // ---- Energy ----
    
    /**
     * 1.19.2: CapabilityEnergy.ENERGY
     * 1.20.1: ForgeCapabilities.ENERGY
     */
    public static final Capability<IEnergyStorage> ENERGY = 
        ForgeCapabilities.ENERGY;
}
