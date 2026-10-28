package net.devmaster.forgecompat.translate;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines ALL translation rules for converting 1.19.2 Forge bytecode to 1.20.1.
 * 
 * This is the single source of truth for "what old thing maps to what new thing."
 * The transformer reads these rules and applies them.
 * 
 * There are several categories of translation:
 * 
 * 1. CLASS_RENAMES - Old class names that became new class names
 * 2. METHOD_RENAMES - Methods that were renamed between versions
 * 3. FIELD_TO_METHODS - Public fields that became getter methods
 * 4. MATERIAL_FLAGS - What property flags each Material constant maps to
 * 5. COMPLEX_PATTERNS - Multi-instruction patterns that need special handling
 */
public class TranslationRules {
    
    // ================================================================
    // 1. CLASS RENAMES
    //    Old internal name → New internal name
    //    Applied everywhere: type references, descriptors, signatures
    // ================================================================
    
    public static final Map<String, String> CLASS_RENAMES = new HashMap<>();
    static {
        // MaterialColor was renamed to MapColor
        CLASS_RENAMES.put(
            "net/minecraft/world/level/material/MaterialColor",
            "net/minecraft/world/level/material/MapColor"
        );
        
        // That's actually it for simple class renames.
        // Material itself was REMOVED, not renamed — it needs special handling below.
        // RegistryObject was REPLACED by DeferredHolder — also special handling.
    }
    
    // ================================================================
    // 2. METHOD RENAMES
    //    owner + old name + old descriptor → new name + new descriptor
    //    The owner and descriptor help disambiguate overloads.
    // ================================================================
    
    public record MethodRename(String owner, String oldName, String oldDesc,
                               String newName, String newDesc) {}
    
    public static final MethodRename[] METHOD_RENAMES = {
        // Entity methods
        new MethodRename(
            "net/minecraft/server/level/ServerPlayer",
            "getLevel", "()Lnet/minecraft/server/level/ServerLevel;",
            "serverLevel", "()Lnet/minecraft/server/level/ServerLevel;"
        ),
        new MethodRename(
            "net/minecraft/server/level/ServerPlayer",
            "setLevel", "(Lnet/minecraft/server/level/ServerLevel;)V",
            "setServerLevel", "(Lnet/minecraft/server/level/ServerLevel;)V"
        ),
        new MethodRename(
            "net/minecraft/world/entity/Entity",
            "outOfWorld", "()Z",
            "onBelowWorld", "()Z"
        ),
        new MethodRename(
            "net/minecraft/world/entity/Entity",
            "checkOutOfWorld", "()V",
            "checkBelowWorld", "()V"
        ),
        
        // ItemStack
        new MethodRename(
            "net/minecraft/world/item/ItemStack",
            "sameItem", "(Lnet/minecraft/world/item/ItemStack;)Z",
            "isSameItem", "(Lnet/minecraft/world/item/ItemStack;)Z"
        ),
        new MethodRename(
            "net/minecraft/world/item/ItemStack",
            "tagMatches", "(Lnet/minecraft/world/item/ItemStack;)Z",
            "isSameItemSameTags", "(Lnet/minecraft/world/item/ItemStack;)Z"
        ),
        
        // Damage
        new MethodRename(
            "net/minecraft/world/damagesource/DamageSources",
            "outOfWorld", "()Lnet/minecraft/world/damagesource/DamageSource;",
            "fellOutOfWorld", "()Lnet/minecraft/world/damagesource/DamageSource;"
        ),
        
        // Block properties
        new MethodRename(
            "net/minecraft/world/level/block/state/BlockBehaviour$Properties",
            "color", "(Lnet/minecraft/world/level/material/MapColor;)Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;",
            "mapColor", "(Lnet/minecraft/world/level/material/MapColor;)Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;"
        ),
        
        // Direction
        new MethodRename(
            "net/minecraft/core/Direction",
            "fromNormal", "(III)Lnet/minecraft/core/Direction;",
            "fromDelta", "(III)Lnet/minecraft/core/Direction;"
        ),
        
        // Player reach
        new MethodRename(
            "net/minecraftforge/common/extensions/IForgePlayer",
            "getAttackRange", "()F",
            "getEntityReach", "()F"
        ),
        new MethodRename(
            "net/minecraftforge/common/extensions/IForgePlayer",
            "getReachDistance", "()F",
            "getBlockReach", "()F"
        ),
        
        // Events
        new MethodRename(
            "net/minecraftforge/event/entity/living/LivingChangeTargetEvent",
            "getTarget", "()Lnet/minecraft/world/entity/LivingEntity;",
            "getNewTarget", "()Lnet/minecraft/world/entity/LivingEntity;"
        ),
        
        // Loot
        new MethodRename(
            "net/minecraft/world/level/storage/loot/LootContext$Builder",
            null, null,  // handled as complex pattern
            null, null
        ),
    };
    
    // ================================================================
    // 3. FIELD → METHOD CONVERSIONS
    //    Public fields that became getter methods in 1.20.1
    //    GETFIELD instruction → INVOKEVIRTUAL instruction
    // ================================================================
    
    public record FieldToMethod(String owner, String fieldName, String fieldDesc,
                                String methodName, String methodDesc) {}
    
    public static final FieldToMethod[] FIELD_TO_METHODS = {
        // Entity.level (field) → Entity.level() (method)
        new FieldToMethod(
            "net/minecraft/world/entity/Entity",
            "level", "Lnet/minecraft/world/level/Level;",
            "level", "()Lnet/minecraft/world/level/Level;"
        ),
        
        // Entity.isOnGround (field) → Entity.onGround() (method)
        new FieldToMethod(
            "net/minecraft/world/entity/Entity",
            "isOnGround", "Z",
            "onGround", "()Z"
        ),
    };
    
    // ================================================================
    // 4. MATERIAL → PROPERTY FLAGS
    //    Each 1.19.2 Material constant maps to a set of 1.20.1 property flags.
    //    The transformer uses this to inline the correct flags when it sees
    //    Properties.of(Material.X) in bytecode.
    //
    //    For variable Materials (not compile-time constants), the transformer
    //    rewrites to CompatHelpers.of(material) which does runtime lookup.
    // ================================================================
    
    public record MaterialFlags(
        boolean ignitedByLava,      // was flammable
        boolean liquid,
        boolean noCollission,       // was non-solid
        boolean replaceable,
        boolean requiresCorrectTool,
        int pushReaction            // 0=NORMAL, 1=DESTROY, 2=BLOCK, 3=PUSH_ONLY
    ) {
        public static final int PUSH_NORMAL = 0;
        public static final int PUSH_DESTROY = 1;
        public static final int PUSH_BLOCK = 2;
        public static final int PUSH_ONLY = 3;
    }
    
    public static final Map<String, MaterialFlags> MATERIAL_FLAGS = new HashMap<>();
    static {
        // The most common ones mods use:
        MATERIAL_FLAGS.put("WOOD", new MaterialFlags(
            true, false, false, false, false, MaterialFlags.PUSH_NORMAL
        ));
        MATERIAL_FLAGS.put("STONE", new MaterialFlags(
            false, false, false, false, true, MaterialFlags.PUSH_NORMAL
        ));
        MATERIAL_FLAGS.put("METAL", new MaterialFlags(
            false, false, false, false, true, MaterialFlags.PUSH_NORMAL
        ));
        MATERIAL_FLAGS.put("DIRT", new MaterialFlags(
            false, false, false, false, false, MaterialFlags.PUSH_NORMAL
        ));
        MATERIAL_FLAGS.put("PLANT", new MaterialFlags(
            true, false, true, false, false, MaterialFlags.PUSH_DESTROY
        ));
        MATERIAL_FLAGS.put("REPLACEABLE_PLANT", new MaterialFlags(
            true, false, true, true, false, MaterialFlags.PUSH_DESTROY
        ));
        MATERIAL_FLAGS.put("WATER", new MaterialFlags(
            false, true, true, false, false, MaterialFlags.PUSH_NORMAL
        ));
        MATERIAL_FLAGS.put("LAVA", new MaterialFlags(
            false, true, true, false, false, MaterialFlags.PUSH_NORMAL
        ));
        MATERIAL_FLAGS.put("SAND", new MaterialFlags(
            false, false, false, false, false, MaterialFlags.PUSH_NORMAL
        ));
        MATERIAL_FLAGS.put("GLASS", new MaterialFlags(
            false, false, false, false, false, MaterialFlags.PUSH_DESTROY
        ));
        MATERIAL_FLAGS.put("WOOL", new MaterialFlags(
            true, false, false, false, false, MaterialFlags.PUSH_NORMAL
        ));
        MATERIAL_FLAGS.put("ICE", new MaterialFlags(
            false, false, false, false, false, MaterialFlags.PUSH_NORMAL
        ));
        MATERIAL_FLAGS.put("CLAY", new MaterialFlags(
            false, false, false, false, false, MaterialFlags.PUSH_NORMAL
        ));
        MATERIAL_FLAGS.put("GRASS", new MaterialFlags(
            false, false, false, false, false, MaterialFlags.PUSH_NORMAL
        ));
        MATERIAL_FLAGS.put("DECORATION", new MaterialFlags(
            false, false, true, false, false, MaterialFlags.PUSH_DESTROY
        ));
        MATERIAL_FLAGS.put("FIRE", new MaterialFlags(
            false, false, true, false, false, MaterialFlags.PUSH_DESTROY
        ));
        MATERIAL_FLAGS.put("AIR", new MaterialFlags(
            false, false, true, false, false, MaterialFlags.PUSH_NORMAL
        ));
        MATERIAL_FLAGS.put("EXPLOSIVE", new MaterialFlags(
            true, false, false, false, false, MaterialFlags.PUSH_DESTROY
        ));
        MATERIAL_FLAGS.put("NETHER_WOOD", new MaterialFlags(
            false, false, false, false, false, MaterialFlags.PUSH_NORMAL
        ));
        MATERIAL_FLAGS.put("BAMBOO", new MaterialFlags(
            true, false, true, false, false, MaterialFlags.PUSH_DESTROY
        ));
        MATERIAL_FLAGS.put("HEAVY_METAL", new MaterialFlags(
            false, false, false, false, true, MaterialFlags.PUSH_NORMAL
        ));
        MATERIAL_FLAGS.put("AMETHYST", new MaterialFlags(
            false, false, false, false, true, MaterialFlags.PUSH_NORMAL
        ));
        MATERIAL_FLAGS.put("MOSS", new MaterialFlags(
            true, false, false, false, false, MaterialFlags.PUSH_NORMAL
        ));
        MATERIAL_FLAGS.put("CAKE", new MaterialFlags(
            false, false, false, false, false, MaterialFlags.PUSH_NORMAL
        ));
        MATERIAL_FLAGS.put("WEB", new MaterialFlags(
            false, false, true, false, false, MaterialFlags.PUSH_DESTROY
        ));
        MATERIAL_FLAGS.put("SPONGE", new MaterialFlags(
            false, false, false, false, false, MaterialFlags.PUSH_NORMAL
        ));
        MATERIAL_FLAGS.put("SCULK", new MaterialFlags(
            false, false, false, false, false, MaterialFlags.PUSH_DESTROY
        ));
        MATERIAL_FLAGS.put("BUILDABLE_GLASS", new MaterialFlags(
            false, false, false, false, false, MaterialFlags.PUSH_NORMAL
        ));
        MATERIAL_FLAGS.put("FABRIC", new MaterialFlags(
            true, false, false, false, false, MaterialFlags.PUSH_DESTROY
        ));
        MATERIAL_FLAGS.put("VEGETABLE", new MaterialFlags(
            true, false, true, false, false, MaterialFlags.PUSH_DESTROY
        ));
        MATERIAL_FLAGS.put("EGG", new MaterialFlags(
            false, false, false, false, false, MaterialFlags.PUSH_NORMAL
        ));
        MATERIAL_FLAGS.put("PORTAL", new MaterialFlags(
            false, false, false, false, false, MaterialFlags.PUSH_BLOCK
        ));
        MATERIAL_FLAGS.put("BARRIER", new MaterialFlags(
            false, false, false, false, false, MaterialFlags.PUSH_BLOCK
        ));
        MATERIAL_FLAGS.put("PISTON", new MaterialFlags(
            false, false, false, false, false, MaterialFlags.PUSH_BLOCK
        ));
        MATERIAL_FLAGS.put("MUD", new MaterialFlags(
            false, false, false, false, false, MaterialFlags.PUSH_NORMAL
        ));
        MATERIAL_FLAGS.put("SNOW", new MaterialFlags(
            false, false, false, false, false, MaterialFlags.PUSH_DESTROY
        ));
        MATERIAL_FLAGS.put("FROGED_ICE", new MaterialFlags(
            false, false, false, false, false, MaterialFlags.PUSH_NORMAL
        ));
        MATERIAL_FLAGS.put("BUBBLE_COLUMN", new MaterialFlags(
            false, true, true, false, false, MaterialFlags.PUSH_ONLY
        ));
        MATERIAL_FLAGS.put("SHULKER_SHELL", new MaterialFlags(
            false, false, false, false, true, MaterialFlags.PUSH_NORMAL
        ));
        MATERIAL_FLAGS.put("STRUCTURAL_AIR", new MaterialFlags(
            false, false, true, false, false, MaterialFlags.PUSH_NORMAL
        ));
        MATERIAL_FLAGS.put("WATER_PLANT", new MaterialFlags(
            true, false, true, false, false, MaterialFlags.PUSH_DESTROY
        ));
        MATERIAL_FLAGS.put("NETHER_PLANT", new MaterialFlags(
            true, false, true, false, false, MaterialFlags.PUSH_DESTROY
        ));
        MATERIAL_FLAGS.put("REPLACEABLE_FIREPROOF_PLANT", new MaterialFlags(
            false, false, true, true, false, MaterialFlags.PUSH_DESTROY
        ));
        MATERIAL_FLAGS.put("CLOTH_DECORATION", new MaterialFlags(
            true, false, true, false, false, MaterialFlags.PUSH_DESTROY
        ));
    }
    
    // ================================================================
    // 5. CAPABILITY FIELD REMAPS
    //    Old capability class + field → new ForgeCapabilities field
    // ================================================================
    
    public record CapabilityRemap(String oldOwner, String oldField,
                                  String newOwner, String newField) {}
    
    public static final CapabilityRemap[] CAPABILITY_REMAPS = {
        new CapabilityRemap(
            "net/minecraftforge/common/capabilities/CapabilityItemHandler",
            "ITEM_HANDLER_CAPABILITY",
            "net/minecraftforge/common/ForgeCapabilities",
            "ITEM_HANDLER"
        ),
        new CapabilityRemap(
            "net/minecraftforge/common/capabilities/CapabilityFluidHandler",
            "FLUID_HANDLER_CAPABILITY",
            "net/minecraftforge/common/ForgeCapabilities",
            "FLUID_HANDLER"
        ),
        new CapabilityRemap(
            "net/minecraftforge/common/capabilities/CapabilityEnergy",
            "ENERGY",
            "net/minecraftforge/common/ForgeCapabilities",
            "ENERGY"
        ),
    };
    
    // ================================================================
    // 6. EVENT RENAMES
    //    Old event class → new event class
    //    Some events also have method renames within them.
    // ================================================================
    
    public static final Map<String, String> EVENT_RENAMES = new HashMap<>();
    static {
        EVENT_RENAMES.put(
            "net/minecraftforge/event/entity/living/LivingSetAttackTargetEvent",
            "net/minecraftforge/event/entity/living/LivingChangeTargetEvent"
        );
        EVENT_RENAMES.put(
            "net/minecraftforge/event/entity/player/SaplingGrowTreeEvent",
            "net/minecraftforge/event/level/BlockGrowFeatureEvent"
        );
    }
    
    // ================================================================
    // NOTE: Registry API is IDENTICAL between Forge 1.19.2 and 1.20.1.
    // DeferredRegister, RegistryObject, ForgeRegistries — all the same.
    // The only registry-related change is Properties.of(Material) → Properties.of().mapColor(...)
    // which is handled by the MATERIAL_FLAGS system above.
    //
    // (The DeferredBlock/DeferredHolder/createBlocks() stuff is NeoForge, not Forge.
    // Don't confuse them!)
    // ================================================================
}
