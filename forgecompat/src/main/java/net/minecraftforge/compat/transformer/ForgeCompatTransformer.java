package net.minecraftforge.compat.transformer;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.util.*;

/**
 * The core bytecode transformer for ForgeCompat.
 * 
 * This class is called by the CoreMod JavaScript bridge to transform
 * target mod classes as they're loaded. It rewrites:
 * 
 * 1. Class references (Material → shim.Material, MaterialColor → MapColor)
 * 2. Field access → method calls (entity.level → entity.level())
 * 3. Method signatures (Properties.of(Material) → Material.applyTo(Properties.of()))
 * 4. Event class references (LivingSetAttackTargetEvent → shim version)
 * 5. Capability references (CapabilityItemHandler → ForgeCapabilities)
 * 
 * The transformer targets classes in the target mod's package namespace.
 * It's configured via the CoreMod JavaScript file.
 */
public class ForgeCompatTransformer {
    
    // ---- Class Reference Mappings ----
    // Maps old 1.19.2 class internal names to their 1.20.1 shim equivalents
    
    private static final Map<String, String> CLASS_REMAPS = new HashMap<>();
    static {
        // Material system
        CLASS_REMAPS.put(
            "net/minecraft/world/level/material/Material",
            "net/minecraftforge/compat/shim/material/Material"
        );
        CLASS_REMAPS.put(
            "net/minecraft/world/level/material/MaterialColor",
            "net/minecraftforge/compat/shim/material/MaterialColor"
        );
        
        // Registry system
        CLASS_REMAPS.put(
            "net/minecraftforge/registries/RegistryObject",
            "net/minecraftforge/compat/shim/registries/RegistryObject"
        );
        CLASS_REMAPS.put(
            "net/minecraftforge/registries/ForgeRegistries",
            "net/minecraftforge/compat/shim/registries/ShimForgeRegistry"
        );
        
        // Event system
        CLASS_REMAPS.put(
            "net/minecraftforge/event/entity/living/LivingSetAttackTargetEvent",
            "net/minecraftforge/compat/shim/events/LivingSetAttackTargetEvent"
        );
        
        // Capability system
        CLASS_REMAPS.put(
            "net/minecraftforge/common/capabilities/CapabilityItemHandler",
            "net/minecraftforge/compat/shim/capabilities/CapabilityCompat"
        );
        CLASS_REMAPS.put(
            "net/minecraftforge/common/capabilities/CapabilityFluidHandler",
            "net/minecraftforge/compat/shim/capabilities/CapabilityCompat"
        );
        CLASS_REMAPS.put(
            "net/minecraftforge/common/capabilities/CapabilityEnergy",
            "net/minecraftforge/compat/shim/capabilities/CapabilityCompat"
        );
    }
    
    // ---- Field-to-Method Transformations ----
    // Maps GETFIELD instructions that need to become INVOKEVIRTUAL
    
    private record FieldTransform(String owner, String fieldName, String fieldDesc,
                                  String methodName, String methodDesc) {}
    
    private static final List<FieldTransform> FIELD_TRANSFORMS = new ArrayList<>();
    static {
        // entity.level (field) → entity.level() (method)
        FIELD_TRANSFORMS.add(new FieldTransform(
            "net/minecraft/world/entity/Entity",
            "level",
            "Lnet/minecraft/world/level/Level;",
            "level",
            "()Lnet/minecraft/world/level/Level;"
        ));
        
        // entity.isOnGround (field) → entity.onGround() (method)
        FIELD_TRANSFORMS.add(new FieldTransform(
            "net/minecraft/world/entity/Entity",
            "isOnGround",
            "Z",
            "onGround",
            "()Z"
        ));
    }
    
    // ---- Method Reference Transformations ----
    // Maps old method calls to new method calls
    
    private record MethodTransform(String owner, String oldName, String oldDesc,
                                   String newName, String newDesc) {}
    
    private static final List<MethodTransform> METHOD_TRANSFORMS = new ArrayList<>();
    static {
        // ServerPlayer.getLevel() → ServerPlayer.serverLevel()
        METHOD_TRANSFORMS.add(new MethodTransform(
            "net/minecraft/server/level/ServerPlayer",
            "getLevel",
            "()Lnet/minecraft/server/level/ServerLevel;",
            "serverLevel",
            "()Lnet/minecraft/server/level/ServerLevel;"
        ));
        
        // Entity.outOfWorld() → Entity.onBelowWorld()
        METHOD_TRANSFORMS.add(new MethodTransform(
            "net/minecraft/world/entity/Entity",
            "outOfWorld",
            "()Z",
            "onBelowWorld",
            "()Z"
        ));
        
        // Entity.checkOutOfWorld() → Entity.checkBelowWorld()
        METHOD_TRANSFORMS.add(new MethodTransform(
            "net/minecraft/world/entity/Entity",
            "checkOutOfWorld",
            "()V",
            "checkBelowWorld",
            "()V"
        ));
        
        // DamageSources.outOfWorld() → DamageSources.fellOutOfWorld()
        METHOD_TRANSFORMS.add(new MethodTransform(
            "net/minecraft/world/damagesource/DamageSources",
            "outOfWorld",
            "()Lnet/minecraft/world/damagesource/DamageSource;",
            "fellOutOfWorld",
            "()Lnet/minecraft/world/damagesource/DamageSource;"
        ));
        
        // ItemStack.sameItem() → ItemStack.isSameItem()
        METHOD_TRANSFORMS.add(new MethodTransform(
            "net/minecraft/world/item/ItemStack",
            "sameItem",
            "(Lnet/minecraft/world/item/ItemStack;)Z",
            "isSameItem",
            "(Lnet/minecraft/world/item/ItemStack;)Z"
        ));
    }
    
    // ---- Capability Field Transforms ----
    
    private record CapabilityTransform(String owner, String oldField, String newField, String newOwner) {}
    
    private static final List<CapabilityTransform> CAPABILITY_TRANSFORMS = new ArrayList<>();
    static {
        CAPABILITY_TRANSFORMS.add(new CapabilityTransform(
            "net/minecraftforge/common/capabilities/CapabilityItemHandler",
            "ITEM_HANDLER_CAPABILITY",
            "ITEM_HANDLER_CAPABILITY",
            "net/minecraftforge/compat/shim/capabilities/CapabilityCompat"
        ));
        CAPABILITY_TRANSFORMS.add(new CapabilityTransform(
            "net/minecraftforge/common/capabilities/CapabilityFluidHandler",
            "FLUID_HANDLER_CAPABILITY",
            "FLUID_HANDLER_CAPABILITY",
            "net/minecraftforge/compat/shim/capabilities/CapabilityCompat"
        ));
        CAPABILITY_TRANSFORMS.add(new CapabilityTransform(
            "net/minecraftforge/common/capabilities/CapabilityEnergy",
            "ENERGY",
            "ENERGY",
            "net/minecraftforge/compat/shim/capabilities/CapabilityCompat"
        ));
    }
    
    // ---- Main Transform Method ----
    
    /**
     * Transform a class node. Called by the CoreMod JavaScript bridge.
     * 
     * @param className The internal class name (e.g., "strelka/homespun/block/ModBlocks")
     * @param classNode The ASM ClassNode to transform
     * @return The transformed ClassNode
     */
    public static ClassNode transform(String className, ClassNode classNode) {
        boolean modified = false;
        
        // Phase 1: Remap class references in the class hierarchy
        modified |= remapClassHierarchy(classNode);
        
        // Phase 2: Transform methods
        for (MethodNode method : classNode.methods) {
            modified |= transformMethod(className, method);
        }
        
        // Phase 3: Transform fields
        for (FieldNode field : classNode.fields) {
            modified |= remapFieldDescriptor(field);
        }
        
        if (modified) {
            net.minecraftforge.compat.ForgeCompat.LOGGER.debug(
                "ForgeCompat: Transformed class {}", className);
        }
        
        return classNode;
    }
    
    // ---- Phase 1: Class Hierarchy Remapping ----
    
    private static boolean remapClassHierarchy(ClassNode classNode) {
        boolean modified = false;
        
        // Remap superclass
        String remappedSuper = CLASS_REMAPS.get(classNode.superName);
        if (remappedSuper != null) {
            classNode.superName = remappedSuper;
            modified = true;
        }
        
        // Remap interfaces
        for (int i = 0; i < classNode.interfaces.size(); i++) {
            String iface = classNode.interfaces.get(i);
            String remapped = CLASS_REMAPS.get(iface);
            if (remapped != null) {
                classNode.interfaces.set(i, remapped);
                modified = true;
            }
        }
        
        return modified;
    }
    
    // ---- Phase 2: Method Transformation ----
    
    private static boolean transformMethod(String className, MethodNode method) {
        boolean modified = false;
        
        // Remap method descriptor
        String newDesc = remapDescriptor(method.desc);
        if (!newDesc.equals(method.desc)) {
            method.desc = newDesc;
            modified = true;
        }
        
        // Transform instructions
        AbstractInsnNode insn = method.instructions.getFirst();
        while (insn != null) {
            AbstractInsnNode next = insn.getNext();
            
            switch (insn.getType()) {
                case AbstractInsnNode.FIELD_INSN:
                    modified |= transformFieldInsn(method, (FieldInsnNode) insn);
                    break;
                case AbstractInsnNode.METHOD_INSN:
                    modified |= transformMethodInsn(method, (MethodInsnNode) insn);
                    break;
                case AbstractInsnNode.TYPE_INSN:
                    modified |= transformTypeInsn((TypeInsnNode) insn);
                    break;
            }
            
            insn = next;
        }
        
        // Remap local variable descriptors
        if (method.localVariables != null) {
            for (LocalVariableNode lv : method.localVariables) {
                String newLvDesc = remapDescriptor(lv.desc);
                if (!newLvDesc.equals(lv.desc)) {
                    lv.desc = newLvDesc;
                    modified = true;
                }
            }
        }
        
        // Remap try-catch block types
        if (method.tryCatchBlocks != null) {
            for (TryCatchBlockNode tcb : method.tryCatchBlocks) {
                if (tcb.type != null) {
                    String remapped = CLASS_REMAPS.get(tcb.type);
                    if (remapped != null) {
                        tcb.type = remapped;
                        modified = true;
                    }
                }
            }
        }
        
        return modified;
    }
    
    // ---- Field Instruction Transformation ----
    
    private static boolean transformFieldInsn(MethodNode method, FieldInsnNode insn) {
        // Check for field-to-method transformations (entity.level → entity.level())
        for (FieldTransform ft : FIELD_TRANSFORMS) {
            if (insn.getOpcode() == Opcodes.GETFIELD 
                && insn.owner.equals(ft.owner) 
                && insn.name.equals(ft.fieldName)
                && insn.desc.equals(ft.fieldDesc)) {
                
                // Replace GETFIELD with INVOKEVIRTUAL
                InsnList replacement = new InsnList();
                replacement.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    ft.owner,
                    ft.methodName,
                    ft.methodDesc,
                    false
                ));
                method.instructions.insert(insn, replacement);
                method.instructions.remove(insn);
                return true;
            }
        }
        
        // Check for capability field transforms
        for (CapabilityTransform ct : CAPABILITY_TRANSFORMS) {
            if (insn.getOpcode() == Opcodes.GETSTATIC
                && insn.owner.equals(ct.owner)
                && insn.name.equals(ct.oldField)) {
                
                insn.owner = ct.newOwner;
                insn.name = ct.newField;
                return true;
            }
        }
        
        // Check for general class remaps in field owner
        String remappedOwner = CLASS_REMAPS.get(insn.owner);
        if (remappedOwner != null) {
            insn.owner = remappedOwner;
            return true;
        }
        
        // Remap field descriptor
        String newDesc = remapDescriptor(insn.desc);
        if (!newDesc.equals(insn.desc)) {
            insn.desc = newDesc;
            return true;
        }
        
        return false;
    }
    
    // ---- Method Instruction Transformation ----
    
    private static boolean transformMethodInsn(MethodNode method, MethodInsnNode insn) {
        // Check for method name/desc transformations
        for (MethodTransform mt : METHOD_TRANSFORMS) {
            if (insn.owner.equals(mt.owner)
                && insn.name.equals(mt.oldName)
                && insn.desc.equals(mt.oldDesc)) {
                
                insn.name = mt.newName;
                insn.desc = mt.newDesc;
                return true;
            }
        }
        
        // Check for Properties.of(Material) → Material.applyTo(Properties.of())
        // This is the key transformation for the Material system
        if (insn.owner.equals("net/minecraft/world/level/block/state/BlockBehaviour$Properties")
            && insn.name.equals("of")
            && insn.desc.equals("(Lnet/minecraft/world/level/material/Material;)Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;")) {
            
            // Transform to: Material.applyTo(Properties.of())
            // We need to:
            // 1. Change the method call to Properties.of() (no args)
            // 2. Insert a call to Material.applyTo() after
            insn.desc = "()Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;";
            
            // Insert Material.applyTo() call after this instruction
            InsnList injected = new InsnList();
            // The Material is still on the stack from before Properties.of() was called
            // We need to swap: Material is below, Properties is on top
            // Actually the Material param is consumed by the old call.
            // We need a different approach: rewrite the call pattern entirely.
            // For now, we replace with a static helper call.
            
            MethodInsnNode applyCall = new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraftforge/compat/shim/material/Material",
                "applyTo",
                "(Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;",
                false
            );
            
            // The pattern in bytecode is:
            //   ALOAD material (on stack)
            //   INVOKESTATIC Properties.of(Material)Properties
            // We want:
            //   INVOKESTATIC Properties.of()Properties  
            //   ALOAD material (need to get it from somewhere)
            //   INVOKEVIRTUAL Material.applyTo(Properties)Properties
            // 
            // This is complex because we need to rearrange the stack.
            // For the initial implementation, we use a static helper instead.
            
            // Replace with: StaticHelper.propertiesOf(material)
            insn.setOpcode(Opcodes.INVOKESTATIC);
            insn.owner = "net/minecraftforge/compat/shim/material/MaterialCompat";
            insn.name = "propertiesOf";
            insn.desc = "(Lnet/minecraftforge/compat/shim/material/Material;)Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;";
            insn.itf = false;
            
            return true;
        }
        
        // Check for class remaps in method owner
        String remappedOwner = CLASS_REMAPS.get(insn.owner);
        if (remappedOwner != null) {
            insn.owner = remappedOwner;
            return true;
        }
        
        // Remap method descriptor
        String newDesc = remapDescriptor(insn.desc);
        if (!newDesc.equals(insn.desc)) {
            insn.desc = newDesc;
            return true;
        }
        
        return false;
    }
    
    // ---- Type Instruction Transformation ----
    
    private static boolean transformTypeInsn(TypeInsnNode insn) {
        String remapped = CLASS_REMAPS.get(insn.desc);
        if (remapped != null) {
            insn.desc = remapped;
            return true;
        }
        return false;
    }
    
    // ---- Phase 3: Field Descriptor Remapping ----
    
    private static boolean remapFieldDescriptor(FieldNode field) {
        String newDesc = remapDescriptor(field.desc);
        if (!newDesc.equals(field.desc)) {
            field.desc = newDesc;
            return true;
        }
        return false;
    }
    
    // ---- Descriptor Remapping ----
    
    /**
     * Remap a type descriptor string, replacing old class references with new ones.
     * Handles both simple types (Ljava/lang/String;) and arrays.
     */
    private static String remapDescriptor(String desc) {
        if (desc == null) return null;
        
        String result = desc;
        for (Map.Entry<String, String> entry : CLASS_REMAPS.entrySet()) {
            String oldInternal = entry.getKey();
            String newInternal = entry.getValue();
            result = result.replace("L" + oldInternal + ";", "L" + newInternal + ";");
        }
        return result;
    }
    
    /**
     * Check if a class should be transformed.
     * Called by the CoreMod to filter which classes to process.
     */
    public static boolean shouldTransform(String className) {
        // Transform all classes that are NOT part of ForgeCompat itself
        // and NOT part of vanilla Minecraft or Forge
        if (className.startsWith("net/minecraftforge/compat/")) return false;
        if (className.startsWith("net/minecraft/")) return false;
        if (className.startsWith("net/minecraftforge/")) return false;
        if (className.startsWith("com/mojang/")) return false;
        if (className.startsWith("org/bukkit/")) return false;
        if (className.startsWith("java/")) return false;
        if (className.startsWith("javax/")) return false;
        
        return true;
    }
}
