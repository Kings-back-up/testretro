package net.minecraftforge.compat.shim.material;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

/**
 * Shim for the 1.19.2 Material class that was removed in 1.20.
 * 
 * In 1.19.2, blocks were created with:
 *   BlockBehaviour.Properties.of(Material.WOOD)
 * 
 * In 1.20.1, Material was removed and replaced with individual property flags:
 *   BlockBehaviour.Properties.of().ignitedByLava().pushReaction(PushReaction.NORMAL)
 * 
 * This shim provides the old Material API and translates it to the new system.
 * The CoreMod transformer rewrites Properties.of(Material) calls to use this class.
 */
public class Material {
    
    // ---- 1.19.2 Material constants ----
    // Each maps to the equivalent set of 1.20.1 property flags
    
    public static final Material AIR = new Material(
        false, false, false, false, false, false,
        PushReaction.NORMAL, false
    );
    
    public static final Material STRUCTURAL_AIR = new Material(
        false, false, false, false, false, false,
        PushReaction.NORMAL, false
    );
    
    public static final Material PORTAL = new Material(
        false, false, false, false, false, false,
        PushReaction.BLOCK, false
    );
    
    public static final Material CLOTH_DECORATION = new Material(
        true, false, false, false, false, false,
        PushReaction.DESTROY, false
    );
    
    public static final Material PLANT = new Material(
        true, false, false, false, false, false,
        PushReaction.DESTROY, false
    );
    
    public static final Material WATER_PLANT = new Material(
        true, false, false, false, false, false,
        PushReaction.DESTROY, false
    );
    
    public static final Material REPLACEABLE_PLANT = new Material(
        true, false, false, false, false, true,
        PushReaction.DESTROY, false
    );
    
    public static final Material NETHER_PLANT = new Material(
        true, false, false, false, false, false,
        PushReaction.DESTROY, false
    );
    
    public static final Material REPLACEABLE_FIREPROOF_PLANT = new Material(
        false, false, false, false, false, true,
        PushReaction.DESTROY, false
    );
    
    public static final Material DECORATION = new Material(
        false, false, false, false, false, false,
        PushReaction.DESTROY, false
    );
    
    public static final Material WEB = new Material(
        false, false, false, false, false, false,
        PushReaction.DESTROY, false
    );
    
    public static final Material SCULK = new Material(
        false, false, false, false, false, false,
        PushReaction.DESTROY, false
    );
    
    public static final Material BUILDABLE_GLASS = new Material(
        false, false, false, false, false, false,
        PushReaction.NORMAL, false
    );
    
    public static final Material CLAY = new Material(
        false, false, false, false, false, false,
        PushReaction.NORMAL, false
    );
    
    public static final Material DIRT = new Material(
        false, false, false, false, false, false,
        PushReaction.NORMAL, false
    );
    
    public static final Material GRASS = new Material(
        false, false, false, false, false, false,
        PushReaction.NORMAL, false
    );
    
    public static final Material MUD = new Material(
        false, false, false, false, false, false,
        PushReaction.NORMAL, false
    );
    
    // THE IMPORTANT ONE - Most mods use this
    public static final Material WOOD = new Material(
        true,  // flammable (→ ignitedByLava in 1.20.1)
        false, // not liquid
        false, // not solid blocking
        false, // not pushable as piston
        false, // not requires correct tool
        false, // not replaceable
        PushReaction.NORMAL,
        false  // not solid
    );
    
    public static final Material NETHER_WOOD = new Material(
        false, false, false, false, false, false,
        PushReaction.NORMAL, false
    );
    
    public static final Material BAMBOO = new Material(
        true, false, false, false, false, false,
        PushReaction.DESTROY, false
    );
    
    public static final Material WOOL = new Material(
        true, false, false, false, false, false,
        PushReaction.NORMAL, false
    );
    
    public static final Material EXPLOSIVE = new Material(
        true, false, false, false, false, false,
        PushReaction.DESTROY, false
    );
    
    public static final Material GLASS = new Material(
        false, false, false, false, false, false,
        PushReaction.DESTROY, false
    );
    
    public static final Material FABRIC = new Material(
        true, false, false, false, false, false,
        PushReaction.DESTROY, false
    );
    
    public static final Material SAND = new Material(
        false, false, false, false, false, false,
        PushReaction.NORMAL, false
    );
    
    public static final Material SPONGE = new Material(
        false, false, false, false, false, false,
        PushReaction.NORMAL, false
    );
    
    public static final Material SHULKER_SHELL = new Material(
        false, false, false, false, true, false,
        PushReaction.NORMAL, false
    );
    
    // THE OTHER IMPORTANT ONE
    public static final Material STONE = new Material(
        false, // not flammable
        false, // not liquid
        false, // not solid blocking override
        false, // not pushable override
        true,  // requires correct tool
        false, // not replaceable
        PushReaction.NORMAL,
        true   // is solid
    );
    
    public static final Material METAL = new Material(
        false, false, false, false, true, false,
        PushReaction.NORMAL, true
    );
    
    public static final Material SNOW = new Material(
        false, false, false, false, false, false,
        PushReaction.DESTROY, false
    );
    
    public static final Material HEAVY_METAL = new Material(
        false, false, false, false, true, false,
        PushReaction.NORMAL, true
    );
    
    public static final Material ICE = new Material(
        false, false, false, false, false, false,
        PushReaction.NORMAL, true
    );
    
    public static final Material LAVA = new Material(
        false, true, false, false, false, false,
        PushReaction.NORMAL, false
    );
    
    public static final Material WATER = new Material(
        false, true, false, false, false, false,
        PushReaction.NORMAL, false
    );
    
    public static final Material FIRE = new Material(
        false, false, false, false, false, false,
        PushReaction.DESTROY, false
    );
    
    public static final Material BUBBLE_COLUMN = new Material(
        false, true, false, false, false, false,
        PushReaction.PUSH_ONLY, false
    );
    
    public static final Material CAKE = new Material(
        false, false, false, false, false, false,
        PushReaction.NORMAL, false
    );
    
    public static final Material AMETHYST = new Material(
        false, false, false, false, true, false,
        PushReaction.NORMAL, true
    );
    
    public static final Material MOSS = new Material(
        true, false, false, false, false, false,
        PushReaction.NORMAL, false
    );
    
    public static final Material VEGETABLE = new Material(
        true, false, false, false, false, false,
        PushReaction.DESTROY, false
    );
    
    public static final Material EGG = new Material(
        false, false, false, false, false, false,
        PushReaction.NORMAL, false
    );
    
    public static final Material FROGED_ICE = new Material(
        false, false, false, false, false, false,
        PushReaction.NORMAL, true
    );
    
    public static final Material PISTON = new Material(
        false, false, false, false, false, false,
        PushReaction.BLOCK, false
    );
    
    public static final Material BARRIER = new Material(
        false, false, false, false, false, false,
        PushReaction.BLOCK, false
    );
    
    // ---- Instance fields ----
    private final boolean flammable;
    private final boolean liquid;
    private final boolean solidBlocking;
    private final boolean pushable;
    private final boolean requiresCorrectTool;
    private final boolean replaceable;
    private final PushReaction pushReaction;
    private final boolean solid;
    
    private Material(boolean flammable, boolean liquid, boolean solidBlocking,
                     boolean pushable, boolean requiresCorrectTool, boolean replaceable,
                     PushReaction pushReaction, boolean solid) {
        this.flammable = flammable;
        this.liquid = liquid;
        this.solidBlocking = solidBlocking;
        this.pushable = pushable;
        this.requiresCorrectTool = requiresCorrectTool;
        this.replaceable = replaceable;
        this.pushReaction = pushReaction;
        this.solid = solid;
    }
    
    // ---- 1.19.2 Material API methods ----
    
    public boolean isFlammable() {
        return flammable;
    }
    
    public boolean isLiquid() {
        return liquid;
    }
    
    public boolean isSolid() {
        return solid;
    }
    
    public boolean blocksMotion() {
        return !liquid;
    }
    
    public boolean canBurn() {
        return flammable;
    }
    
    public boolean isReplaceable() {
        return replaceable;
    }
    
    public boolean isSolidBlocking() {
        return solidBlocking || solid;
    }
    
    public PushReaction getPushReaction() {
        return pushReaction;
    }
    
    public boolean requiresCorrectToolForDrops() {
        return requiresCorrectTool;
    }
    
    /**
     * KEY METHOD: Apply this Material's properties to a 1.20.1 BlockBehaviour.Properties.
     * This is what the transformer rewrites Properties.of(Material) to.
     * 
     * BEFORE (1.19.2): BlockBehaviour.Properties.of(Material.WOOD)
     * AFTER (transformed): Material.WOOD.applyTo(BlockBehaviour.Properties.of())
     */
    public BlockBehaviour.Properties applyTo(BlockBehaviour.Properties props) {
        if (flammable) {
            props = props.ignitedByLava();
        }
        if (liquid) {
            props = props.liquid();
        }
        props = props.pushReaction(pushReaction);
        if (replaceable) {
            props = props.replaceable();
        }
        if (requiresCorrectTool) {
            props = props.requiresCorrectToolForDrops();
        }
        if (!solid) {
            props = props.noCollission();
        }
        return props;
    }
}
