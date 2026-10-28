package net.minecraftforge.compat.shim.material;

import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Static helper for creating BlockBehaviour.Properties from a Material.
 * 
 * This is the key bridge between the 1.19.2 Material system and 1.20.1 Properties.
 * The transformer rewrites:
 *   BlockBehaviour.Properties.of(Material.WOOD)
 * to:
 *   MaterialCompat.propertiesOf(Material.WOOD)
 * 
 * Which internally calls:
 *   Material.WOOD.applyTo(BlockBehaviour.Properties.of())
 */
public class MaterialCompat {
    
    /**
     * Create BlockBehaviour.Properties from a 1.19.2 Material.
     * This is what the transformer injects to replace Properties.of(Material).
     */
    public static BlockBehaviour.Properties propertiesOf(Material material) {
        return material.applyTo(BlockBehaviour.Properties.of());
    }
    
    /**
     * Alternative: create properties with a Material and MaterialColor.
     * In 1.19.2: Properties.of(Material, MaterialColor)
     * In 1.20.1: Properties.of().mapColor(MapColor)
     */
    public static BlockBehaviour.Properties propertiesOf(Material material, 
            net.minecraft.world.level.material.MapColor color) {
        return material.applyTo(BlockBehaviour.Properties.of().mapColor(color));
    }
    
    /**
     * Copy properties from an existing block (same in both versions).
     */
    public static BlockBehaviour.Properties ofFullCopy(BlockBehaviour.Properties source) {
        return BlockBehaviour.Properties.ofFullCopy(source);
    }
}
