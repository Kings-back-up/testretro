package net.minecraftforge.compat.shim.registries;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Shim for the 1.19.2 ForgeRegistries class.
 * 
 * In 1.19.2, ForgeRegistries.BLOCKS was an IForgeRegistry<Block>.
 * In 1.20.1, ForgeRegistries.BLOCKS is a DeferredRegister.Blocks factory.
 * 
 * For the compatibility layer, we provide a marker class that the transformer
 * recognizes and routes to the correct 1.20.1 registration method.
 */
public class ShimForgeRegistry<T> {
    
    public enum RegistryType {
        BLOCK, ITEM, FLUID, ENTITY_TYPE, BLOCK_ENTITY_TYPE,
        MOB_EFFECT, SOUND_EVENT, RECIPE_SERIALIZER, MENU, OTHER
    }
    
    private final RegistryType type;
    private final ResourceKey<? extends Registry<T>> resourceKey;
    
    private ShimForgeRegistry(RegistryType type, ResourceKey<? extends Registry<T>> resourceKey) {
        this.type = type;
        this.resourceKey = resourceKey;
    }
    
    // ---- 1.19.2 ForgeRegistries constants ----
    
    public static final ShimForgeRegistry<Block> BLOCKS = 
        new ShimForgeRegistry<>(RegistryType.BLOCK, BuiltInRegistries.BLOCK.key());
    
    public static final ShimForgeRegistry<Item> ITEMS = 
        new ShimForgeRegistry<>(RegistryType.ITEM, BuiltInRegistries.ITEM.key());
    
    public static final ShimForgeRegistry<Fluid> FLUIDS = 
        new ShimForgeRegistry<>(RegistryType.FLUID, BuiltInRegistries.FLUID.key());
    
    public static final ShimForgeRegistry<EntityType<?>> ENTITY_TYPES = 
        new ShimForgeRegistry<>(RegistryType.ENTITY_TYPE, BuiltInRegistries.ENTITY_TYPE.key());
    
    public static final ShimForgeRegistry<BlockEntityType<?>> BLOCK_ENTITY_TYPES = 
        new ShimForgeRegistry<>(RegistryType.BLOCK_ENTITY_TYPE, BuiltInRegistries.BLOCK_ENTITY_TYPE.key());
    
    // ---- Accessor methods ----
    
    public RegistryType getRegistryType() {
        return type;
    }
    
    public ResourceKey<? extends Registry<T>> getResourceKey() {
        return resourceKey;
    }
    
    /**
     * 1.19.2 had getValue(ResourceLocation) on IForgeRegistry.
     * We delegate to the vanilla registry.
     */
    @SuppressWarnings("unchecked")
    public T getValue(net.minecraft.resources.ResourceLocation location) {
        Registry<T> registry = (Registry<T>) switch (type) {
            case BLOCK -> BuiltInRegistries.BLOCK;
            case ITEM -> BuiltInRegistries.ITEM;
            case FLUID -> BuiltInRegistries.FLUID;
            case ENTITY_TYPE -> BuiltInRegistries.ENTITY_TYPE;
            case BLOCK_ENTITY_TYPE -> BuiltInRegistries.BLOCK_ENTITY_TYPE;
            default -> throw new UnsupportedOperationException("getValue not supported for " + type);
        };
        return registry.get(location);
    }
    
    /**
     * 1.19.2 had getKey(T) on IForgeRegistry.
     */
    @SuppressWarnings("unchecked")
    public net.minecraft.resources.ResourceLocation getKey(T value) {
        Registry<T> registry = (Registry<T>) switch (type) {
            case BLOCK -> BuiltInRegistries.BLOCK;
            case ITEM -> BuiltInRegistries.ITEM;
            case FLUID -> BuiltInRegistries.FLUID;
            case ENTITY_TYPE -> BuiltInRegistries.ENTITY_TYPE;
            case BLOCK_ENTITY_TYPE -> BuiltInRegistries.BLOCK_ENTITY_TYPE;
            default -> throw new UnsupportedOperationException("getKey not supported for " + type);
        };
        return registry.getKey(value);
    }
}
