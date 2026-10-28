package net.devmaster.forgecompat.translate;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.devmaster.forgecompat.translate.TranslationRules.MaterialFlags;

/**
 * Tiny static helpers for translation patterns that can't be expressed as
 * simple bytecode rewrites.
 * 
 * IMPORTANT: This is NOT a re-implementation of the old API.
 * These are translation utilities that the transformer calls when it can't
 * inline the translation directly (e.g., when a Material is in a variable
 * rather than a constant).
 * 
 * Most translations happen directly in bytecode. These helpers are the
 * exception for complex cases.
 */
public class CompatHelpers {
    
    /**
     * Translate a MaterialData to BlockBehaviour.Properties.
     * 
     * Used when the transformer sees:
     *   Material m = ...; // variable, not constant
     *   Properties p = Properties.of(m);
     * 
     * And rewrites to:
     *   Properties p = CompatHelpers.of(m);
     * 
     * For compile-time constants (Properties.of(Material.WOOD)),
     * the transformer inlines the flags directly — this method is NOT called.
     */
    public static BlockBehaviour.Properties of(MaterialData material) {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of();
        
        if (material.ignitedByLava) {
            props = props.ignitedByLava();
        }
        if (material.liquid) {
            props = props.liquid();
        }
        if (material.noCollission) {
            props = props.noCollission();
        }
        if (material.replaceable) {
            props = props.replaceable();
        }
        if (material.requiresCorrectTool) {
            props = props.requiresCorrectToolForDrops();
        }
        props = props.pushReaction(switch (material.pushReaction) {
            case MaterialFlags.PUSH_DESTROY -> PushReaction.DESTROY;
            case MaterialFlags.PUSH_BLOCK -> PushReaction.BLOCK;
            case MaterialFlags.PUSH_ONLY -> PushReaction.PUSH_ONLY;
            default -> PushReaction.NORMAL;
        });
        
        return props;
    }
    
    /**
     * Check if a material is flammable (was Material.isFlammable() in 1.19.2).
     * Used when the transformer sees: material.isFlammable()
     * and rewrites to: CompatHelpers.isFlammable(material)
     */
    public static boolean isFlammable(MaterialData material) {
        return material.ignitedByLava;
    }
    
    /**
     * Check if a material is liquid.
     */
    public static boolean isLiquid(MaterialData material) {
        return material.liquid;
    }
    
    /**
     * Check if a material is solid blocking.
     */
    public static boolean isSolidBlocking(MaterialData material) {
        return !material.noCollission && !material.liquid;
    }
    
    /**
     * Check if a material can burn.
     */
    public static boolean canBurn(MaterialData material) {
        return material.ignitedByLava;
    }
    
    /**
     * Check if a material is replaceable.
     */
    public static boolean isReplaceable(MaterialData material) {
        return material.replaceable;
    }
}
