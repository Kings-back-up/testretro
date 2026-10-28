package net.minecraftforge.compat.shim.registries;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredHolder;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Shim for the 1.19.2 RegistryObject class.
 * 
 * In 1.19.2: RegistryObject<T> implements Supplier<T>
 * In 1.20.1: This was replaced by DeferredHolder<R, T>
 * 
 * This shim wraps a DeferredHolder to provide the old RegistryObject API.
 * The CoreMod transformer rewrites:
 *   RegistryObject<Block> → this class
 *   deferredRegister.register() return type → this class
 */
public class RegistryObject<T> implements Supplier<T> {
    
    private final DeferredHolder<?, T> holder;
    private final ResourceLocation id;
    
    private RegistryObject(DeferredHolder<?, T> holder, ResourceLocation id) {
        this.holder = holder;
        this.id = id;
    }
    
    /**
     * Create a RegistryObject wrapping a DeferredHolder.
     * Used internally by the shim DeferredRegister.
     */
    public static <T> RegistryObject<T> of(DeferredHolder<?, T> holder) {
        return new RegistryObject<>(holder, holder.getId());
    }
    
    /**
     * Create a RegistryObject wrapping a DeferredHolder with explicit ID.
     */
    public static <T> RegistryObject<T> of(DeferredHolder<?, T> holder, ResourceLocation id) {
        return new RegistryObject<>(holder, id);
    }
    
    // ---- 1.19.2 RegistryObject API ----
    
    @Override
    public T get() {
        return holder.get();
    }
    
    /**
     * Get the ResourceLocation ID of this registry entry.
     * 1.19.2 API: RegistryObject#getId()
     */
    public ResourceLocation getId() {
        return id;
    }
    
    /**
     * Get the key (same as getId in 1.19.2).
     */
    public ResourceLocation getKey() {
        return id;
    }
    
    /**
     * Check if this registry object has been populated yet.
     */
    public boolean isPresent() {
        try {
            return holder.get() != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get the value if present, or throw.
     */
    public T asOptional() {
        return get();
    }
    
    /**
     * Stream containing this entry if present.
     */
    public Stream<T> stream() {
        return isPresent() ? Stream.of(get()) : Stream.empty();
    }
    
    /**
     * 1.19.2 had a type-specific cast method.
     */
    @SuppressWarnings("unchecked")
    public <I> RegistryObject<I> cast() {
        return (RegistryObject<I>) this;
    }
    
    /**
     * Get the underlying DeferredHolder (for internal use).
     */
    public DeferredHolder<?, T> getHolder() {
        return holder;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegistryObject<?> that)) return false;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "RegistryObject{" + id + "}";
    }
}
