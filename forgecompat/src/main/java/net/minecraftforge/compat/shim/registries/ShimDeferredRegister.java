package net.minecraftforge.compat.shim.registries;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredHolder;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Shim for the 1.19.2 DeferredRegister API.
 * 
 * In 1.19.2:
 *   DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
 *   RegistryObject<Block> MY_BLOCK = BLOCKS.register("my_block", () -> new Block(props));
 * 
 * In 1.20.1:
 *   DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
 *   DeferredBlock<Block> MY_BLOCK = BLOCKS.register("my_block", () -> new Block(props));
 * 
 * This shim provides the old API by wrapping the new DeferredRegister internally.
 * The transformer rewrites:
 *   - DeferredRegister.create(ForgeRegistries.BLOCKS, modid) → ShimDeferredRegister.createBlocks(modid)
 *   - register() return type → RegistryObject wrapping DeferredHolder
 */
public class ShimDeferredRegister<T> {
    
    private final DeferredRegister<?> internal;
    private final String modId;
    
    private ShimDeferredRegister(DeferredRegister<?> internal, String modId) {
        this.internal = internal;
        this.modId = modId;
    }
    
    // ---- 1.19.2 API: DeferredRegister.create(IForgeRegistry, String) ----
    
    /**
     * Old 1.19.2 API: DeferredRegister.create(ForgeRegistries.BLOCKS, modid)
     * The transformer rewrites calls to ForgeRegistries.BLOCKS to pass a marker,
     * and this method handles the dispatch.
     */
    @SuppressWarnings("unchecked")
    public static <B> ShimDeferredRegister<B> create(ShimForgeRegistry<B> registry, String modId) {
        DeferredRegister<?> internal = switch (registry.getRegistryType()) {
            case BLOCK -> DeferredRegister.createBlocks(modId);
            case ITEM -> DeferredRegister.createItems(modId);
            case FLUID -> DeferredRegister.createFluids(modId);
            case ENTITY_TYPE -> DeferredRegister.createEntityType(modId);
            case BLOCK_ENTITY_TYPE -> DeferredRegister.createBlockEntityType(modId);
            case MOB_EFFECT -> DeferredRegister.createMobEffects(modId);
            case SOUND_EVENT -> DeferredRegister.createSoundEvents(modId);
            default -> DeferredRegister.create(
                (ResourceKey<Registry<B>>) registry.getResourceKey(), modId);
        };
        return new ShimDeferredRegister<>(internal, modId);
    }
    
    /**
     * Alternative old API: DeferredRegister.create(ResourceKey, String)
     */
    public static <T> ShimDeferredRegister<T> create(ResourceKey<? extends Registry<T>> key, String modId) {
        DeferredRegister<T> internal = DeferredRegister.create(key, modId);
        return new ShimDeferredRegister<>(internal, modId);
    }
    
    // ---- 1.19.2 API: register(String, Supplier) -> RegistryObject ----
    
    /**
     * Old 1.19.2 API: register(name, supplier) -> RegistryObject<T>
     * In 1.20.1, this returns DeferredHolder<R, T>
     */
    @SuppressWarnings("unchecked")
    public <I extends T> RegistryObject<I> register(String name, Supplier<? extends I> supplier) {
        DeferredHolder<?, ?> holder = ((DeferredRegister<T>) internal).register(name, supplier);
        ResourceLocation id = new ResourceLocation(modId, name);
        return RegistryObject.of((DeferredHolder<?, I>) holder, id);
    }
    
    // ---- Registration ----
    
    /**
     * Register this deferred register to the mod event bus.
     * Same API in both versions.
     */
    public void register(IEventBus bus) {
        internal.register(bus);
    }
    
    /**
     * Get the internal DeferredRegister (for advanced usage).
     */
    public DeferredRegister<?> getInternal() {
        return internal;
    }
    
    public String getModId() {
        return modId;
    }
}
