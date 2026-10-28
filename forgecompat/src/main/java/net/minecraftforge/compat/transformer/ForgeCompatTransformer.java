package net.minecraftforge.compat.transformer;

import net.minecraftforge.compat.translate.TranslationRules;
import net.minecraftforge.compat.translate.TranslationRules.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;

/**
 * Pure bytecode translator: rewrites 1.19.2 Forge bytecode directly to 1.20.1.
 * 
 * NO SHIM CLASSES. NO WRAPPERS. NO INDIRECTION.
 * 
 * The transformer reads TranslationRules and applies them directly:
 * - Method renames: INVOKEVIRTUAL old.name → INVOKEVIRTUAL new.name
 * - Field→method: GETFIELD x.y → INVOKEVIRTUAL x.y()
 * - Material inlining: Properties.of(Material.WOOD) → Properties.of().ignitedByLava()
 * - Class renames: MaterialColor → MapColor (everywhere in descriptors)
 * - Capability remaps: GETSTATIC CapabilityItemHandler.X → GETSTATIC ForgeCapabilities.Y
 * - Event renames: type references rewritten
 * - Registry factory: DeferredRegister.create(ForgeRegistries.BLOCKS, id) → DeferredRegister.createBlocks(id)
 * 
 * Only falls back to CompatHelpers for cases where the Material is in a variable
 * (not a compile-time constant) and can't be inlined.
 */
public class ForgeCompatTransformer {
    
    private static final String MATERIAL_INTERNAL = "net/minecraft/world/level/material/Material";
    private static final String MATERIAL_DATA_INTERNAL = "net/minecraftforge/compat/translate/MaterialData";
    private static final String COMPAT_HELPERS_INTERNAL = "net/minecraftforge/compat/translate/CompatHelpers";
    private static final String PROPERTIES_INTERNAL = "net/minecraft/world/level/block/state/BlockBehaviour$Properties";
    private static final String FORGE_REGISTRIES_INTERNAL = "net/minecraftforge/registries/ForgeRegistries";
    private static final String DEFERRED_REGISTER_INTERNAL = "net/minecraftforge/registries/DeferredRegister";
    
    /**
     * Transform a class. Called by the CoreMod JS bridge.
     */
    public static ClassNode transform(String className, ClassNode classNode) {
        boolean modified = false;
        
        // Phase 1: Remap class hierarchy (superclass, interfaces)
        modified |= remapClassHierarchy(classNode);
        
        // Phase 2: Transform each method
        for (MethodNode method : classNode.methods) {
            modified |= transformMethod(className, method);
        }
        
        // Phase 3: Remap field descriptors
        for (FieldNode field : classNode.fields) {
            String newDesc = remapDescriptor(field.desc);
            if (!newDesc.equals(field.desc)) {
                field.desc = newDesc;
                modified = true;
            }
            // Also remap field signature (generics)
            if (field.signature != null) {
                String newSig = remapDescriptor(field.signature);
                if (!newSig.equals(field.signature)) {
                    field.signature = newSig;
                    modified = true;
                }
            }
        }
        
        // Phase 4: Remap class signature (generics)
        if (classNode.signature != null) {
            String newSig = remapDescriptor(classNode.signature);
            if (!newSig.equals(classNode.signature)) {
                classNode.signature = newSig;
                modified = true;
            }
        }
        
        return classNode;
    }
    
    // ================================================================
    // PHASE 1: Class hierarchy
    // ================================================================
    
    private static boolean remapClassHierarchy(ClassNode cn) {
        boolean modified = false;
        
        // Superclass
        String newSuper = TranslationRules.CLASS_RENAMES.get(cn.superName);
        if (newSuper != null) { cn.superName = newSuper; modified = true; }
        
        // Material → MaterialData in superclass
        if (MATERIAL_INTERNAL.equals(cn.superName)) {
            cn.superName = MATERIAL_DATA_INTERNAL;
            modified = true;
        }
        
        // Interfaces
        for (int i = 0; i < cn.interfaces.size(); i++) {
            String iface = cn.interfaces.get(i);
            String renamed = TranslationRules.CLASS_RENAMES.get(iface);
            if (renamed != null) { cn.interfaces.set(i, renamed); modified = true; }
            
            // Event renames
            String eventRenamed = TranslationRules.EVENT_RENAMES.get(iface);
            if (eventRenamed != null) { cn.interfaces.set(i, eventRenamed); modified = true; }
        }
        
        return modified;
    }
    
    // ================================================================
    // PHASE 2: Method transformation
    // ================================================================
    
    private static boolean transformMethod(String className, MethodNode method) {
        boolean modified = false;
        
        // Remap method descriptor
        String newDesc = remapDescriptor(method.desc);
        if (!newDesc.equals(method.desc)) { method.desc = newDesc; modified = true; }
        
        // Remap method signature (generics)
        if (method.signature != null) {
            String newSig = remapDescriptor(method.signature);
            if (!newSig.equals(method.signature)) { method.signature = newSig; modified = true; }
        }
        
        // First pass: detect and handle multi-instruction patterns
        modified |= handleMaterialPattern(method);
        modified |= handleRegistryFactoryPattern(method);
        
        // Second pass: single-instruction transformations
        AbstractInsnNode insn = method.instructions.getFirst();
        while (insn != null) {
            AbstractInsnNode next = insn.getNext();
            boolean replaced = false;
            
            switch (insn.getOpcode()) {
                case Opcodes.GETFIELD:
                    replaced = transformGetField(method, (FieldInsnNode) insn);
                    break;
                case Opcodes.GETSTATIC:
                    replaced = transformGetStatic(method, (FieldInsnNode) insn);
                    break;
                case Opcodes.INVOKEVIRTUAL:
                case Opcodes.INVOKESPECIAL:
                case Opcodes.INVOKESTATIC:
                case Opcodes.INVOKEINTERFACE:
                    replaced = transformMethodCall(method, (MethodInsnNode) insn);
                    break;
                case Opcodes.NEW:
                case Opcodes.ANEWARRAY:
                case Opcodes.CHECKCAST:
                case Opcodes.INSTANCEOF:
                    replaced = transformTypeInsn((TypeInsnNode) insn);
                    break;
            }
            
            if (replaced) modified = true;
            insn = next;
        }
        
        // Remap local variables
        if (method.localVariables != null) {
            for (LocalVariableNode lv : method.localVariables) {
                String newLvDesc = remapDescriptor(lv.desc);
                if (!newLvDesc.equals(lv.desc)) { lv.desc = newLvDesc; modified = true; }
                if (lv.signature != null) {
                    String newLvSig = remapDescriptor(lv.signature);
                    if (!newLvSig.equals(lv.signature)) { lv.signature = newLvSig; modified = true; }
                }
            }
        }
        
        // Remap try-catch types
        if (method.tryCatchBlocks != null) {
            for (TryCatchBlockNode tcb : method.tryCatchBlocks) {
                if (tcb.type != null) {
                    String renamed = TranslationRules.CLASS_RENAMES.get(tcb.type);
                    if (renamed != null) { tcb.type = renamed; modified = true; }
                    String eventRenamed = TranslationRules.EVENT_RENAMES.get(tcb.type);
                    if (eventRenamed != null) { tcb.type = eventRenamed; modified = true; }
                }
            }
        }
        
        return modified;
    }
    
    // ================================================================
    // MATERIAL PATTERN: Properties.of(Material.X) → Properties.of().ignitedByLava()
    //
    // This is the most complex translation. We look for the pattern:
    //   GETSTATIC Material.X        (push the material constant)
    //   INVOKESTATIC Properties.of(Material)Properties
    //
    // And rewrite to the inlined property flags:
    //   INVOKESTATIC Properties.of()Properties
    //   INVOKEVIRTUAL Properties.ignitedByLava()Properties   (if flammable)
    //   INVOKEVIRTUAL Properties.pushReaction(DESTROY)Properties  (etc)
    //
    // If the Material is NOT a constant (it's in a variable), we rewrite to:
    //   INVOKESTATIC CompatHelpers.of(MaterialData)Properties
    // And the GETSTATIC Material.X becomes GETSTATIC MaterialData.X
    // ================================================================
    
    private static boolean handleMaterialPattern(MethodNode method) {
        boolean modified = false;
        AbstractInsnNode insn = method.instructions.getFirst();
        
        while (insn != null) {
            AbstractInsnNode next = insn.getNext();
            
            // Look for: GETSTATIC Material.X followed by INVOKESTATIC Properties.of(Material)
            if (insn.getOpcode() == Opcodes.GETSTATIC) {
                FieldInsnNode fin = (FieldInsnNode) insn;
                if (fin.owner.equals(MATERIAL_INTERNAL) && next != null
                    && next.getOpcode() == Opcodes.INVOKESTATIC) {
                    
                    MethodInsnNode min = (MethodInsnNode) next;
                    if (min.owner.equals(PROPERTIES_INTERNAL) 
                        && min.name.equals("of")
                        && min.desc.equals("(L" + MATERIAL_INTERNAL + ";)L" + PROPERTIES_INTERNAL + ";")) {
                        
                        String materialName = fin.name; // e.g. "WOOD"
                        MaterialFlags flags = TranslationRules.MATERIAL_FLAGS.get(materialName);
                        
                        if (flags != null) {
                            // SUCCESS: We know the material at compile time.
                            // Inline the flags directly.
                            InsnList replacement = new InsnList();
                            
                            // Properties.of() — no args
                            replacement.add(new MethodInsnNode(
                                Opcodes.INVOKESTATIC, PROPERTIES_INTERNAL,
                                "of", "()L" + PROPERTIES_INTERNAL + ";", false
                            ));
                            
                            // Chain the property flags
                            if (flags.ignitedByLava()) {
                                replacement.add(new MethodInsnNode(
                                    Opcodes.INVOKEVIRTUAL, PROPERTIES_INTERNAL,
                                    "ignitedByLava", "()L" + PROPERTIES_INTERNAL + ";", false
                                ));
                            }
                            if (flags.liquid()) {
                                replacement.add(new MethodInsnNode(
                                    Opcodes.INVOKEVIRTUAL, PROPERTIES_INTERNAL,
                                    "liquid", "()L" + PROPERTIES_INTERNAL + ";", false
                                ));
                            }
                            if (flags.noCollission()) {
                                replacement.add(new MethodInsnNode(
                                    Opcodes.INVOKEVIRTUAL, PROPERTIES_INTERNAL,
                                    "noCollission", "()L" + PROPERTIES_INTERNAL + ";", false
                                ));
                            }
                            if (flags.replaceable()) {
                                replacement.add(new MethodInsnNode(
                                    Opcodes.INVOKEVIRTUAL, PROPERTIES_INTERNAL,
                                    "replaceable", "()L" + PROPERTIES_INTERNAL + ";", false
                                ));
                            }
                            if (flags.requiresCorrectTool()) {
                                replacement.add(new MethodInsnNode(
                                    Opcodes.INVOKEVIRTUAL, PROPERTIES_INTERNAL,
                                    "requiresCorrectToolForDrops", "()L" + PROPERTIES_INTERNAL + ";", false
                                ));
                            }
                            if (flags.pushReaction() != MaterialFlags.PUSH_NORMAL) {
                                String pushDesc = "(Lnet/minecraft/world/level/material/PushReaction;)L" + PROPERTIES_INTERNAL + ";";
                                String pushField = switch (flags.pushReaction()) {
                                    case MaterialFlags.PUSH_DESTROY -> "DESTROY";
                                    case MaterialFlags.PUSH_BLOCK -> "BLOCK";
                                    case MaterialFlags.PUSH_ONLY -> "PUSH_ONLY";
                                    default -> "NORMAL";
                                };
                                replacement.add(new FieldInsnNode(
                                    Opcodes.GETSTATIC,
                                    "net/minecraft/world/level/material/PushReaction",
                                    pushField,
                                    "Lnet/minecraft/world/level/material/PushReaction;"
                                ));
                                replacement.add(new MethodInsnNode(
                                    Opcodes.INVOKEVIRTUAL, PROPERTIES_INTERNAL,
                                    "pushReaction", pushDesc, false
                                ));
                            }
                            
                            // Replace the two instructions (GETSTATIC + INVOKESTATIC)
                            method.instructions.insert(insn, replacement);
                            method.instructions.remove(insn);  // remove GETSTATIC
                            method.instructions.remove(min);    // remove INVOKESTATIC
                            modified = true;
                            
                            insn = next.getNext();
                            continue;
                        } else {
                            // Unknown material — fall back to CompatHelpers
                            // Rewrite: GETSTATIC Material.X → GETSTATIC MaterialData.X
                            // Rewrite: Properties.of(Material) → CompatHelpers.of(MaterialData)
                            fin.owner = MATERIAL_DATA_INTERNAL;
                            min.owner = COMPAT_HELPERS_INTERNAL;
                            min.name = "of";
                            min.desc = "(L" + MATERIAL_DATA_INTERNAL + ";)L" + PROPERTIES_INTERNAL + ";";
                            min.setOpcode(Opcodes.INVOKESTATIC);
                            modified = true;
                        }
                    }
                }
            }
            
            // Also handle: Material.X.isFlammable() → true/false (constant fold)
            // or material.isFlammable() → CompatHelpers.isFlammable(material)
            if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                MethodInsnNode min = (MethodInsnNode) insn;
                if (min.owner.equals(MATERIAL_INTERNAL)) {
                    // Rewrite Material method calls to CompatHelpers
                    AbstractInsnNode prev = insn.getPrevious();
                    String helperMethod = switch (min.name) {
                        case "isFlammable", "canBurn" -> "isFlammable";
                        case "isLiquid" -> "isLiquid";
                        case "isSolidBlocking" -> "isSolidBlocking";
                        case "isReplaceable" -> "isReplaceable";
                        default -> null;
                    };
                    
                    if (helperMethod != null) {
                        // Check if previous is GETSTATIC Material.X (constant)
                        if (prev != null && prev.getOpcode() == Opcodes.GETSTATIC) {
                            FieldInsnNode pfin = (FieldInsnNode) prev;
                            if (pfin.owner.equals(MATERIAL_INTERNAL)) {
                                MaterialFlags flags = TranslationRules.MATERIAL_FLAGS.get(pfin.name);
                                if (flags != null) {
                                    // Constant fold: replace with ICONST_0 or ICONST_1
                                    boolean result = switch (helperMethod) {
                                        case "isFlammable" -> flags.ignitedByLava();
                                        case "isLiquid" -> flags.liquid();
                                        case "isSolidBlocking" -> !flags.noCollission() && !flags.liquid();
                                        case "isReplaceable" -> flags.replaceable();
                                        default -> false;
                                    };
                                    InsnList replacement = new InsnList();
                                    replacement.add(new InsnNode(result ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                                    method.instructions.insert(prev, replacement);
                                    method.instructions.remove(prev);
                                    method.instructions.remove(insn);
                                    modified = true;
                                    insn = next;
                                    continue;
                                }
                            }
                        }
                        
                        // Variable material: rewrite to CompatHelpers.isFlammable(material)
                        min.setOpcode(Opcodes.INVOKESTATIC);
                        min.owner = COMPAT_HELPERS_INTERNAL;
                        min.name = helperMethod;
                        min.desc = "(L" + MATERIAL_DATA_INTERNAL + ";)Z";
                        modified = true;
                    }
                }
            }
            
            insn = next;
        }
        
        return modified;
    }
    
    // ================================================================
    // REGISTRY FACTORY PATTERN
    // DeferredRegister.create(ForgeRegistries.BLOCKS, modid)
    //   → DeferredRegister.createBlocks(modid)
    //
    // Bytecode pattern:
    //   GETSTATIC ForgeRegistries.BLOCKS
    //   LDC "modid"
    //   INVOKESTATIC DeferredRegister.create(IForgeRegistry, String)DeferredRegister
    //
    // Rewrite to:
    //   LDC "modid"
    //   INVOKESTATIC DeferredRegister.createBlocks(String)DeferredRegister$Blocks
    // ================================================================
    
    private static boolean handleRegistryFactoryPattern(MethodNode method) {
        boolean modified = false;
        AbstractInsnNode insn = method.instructions.getFirst();
        
        while (insn != null) {
            AbstractInsnNode next = insn.getNext();
            
            if (insn.getOpcode() == Opcodes.GETSTATIC) {
                FieldInsnNode fin = (FieldInsnNode) insn;
                if (fin.owner.equals(FORGE_REGISTRIES_INTERNAL)) {
                    RegistryFactoryRemap remap = TranslationRules.REGISTRY_FACTORY_REMAPS.get(fin.name);
                    if (remap != null && next != null && next.getOpcode() == Opcodes.LDC) {
                        AbstractInsnNode afterLdc = next.getNext();
                        if (afterLdc != null && afterLdc.getOpcode() == Opcodes.INVOKESTATIC) {
                            MethodInsnNode min = (MethodInsnNode) afterLdc;
                            if (min.owner.equals(DEFERRED_REGISTER_INTERNAL) && min.name.equals("create")) {
                                // Found the pattern! Rewrite it.
                                // Remove the GETSTATIC ForgeRegistries.X
                                method.instructions.remove(insn);
                                // Change the INVOKESTATIC to the new factory method
                                min.name = remap.newFactoryMethod();
                                min.desc = "(Ljava/lang/String;)" + remap.newReturnType();
                                modified = true;
                            }
                        }
                    }
                }
            }
            
            insn = next;
        }
        
        return modified;
    }
    
    // ================================================================
    // GETFIELD → INVOKEVIRTUAL (field became a method)
    // ================================================================
    
    private static boolean transformGetField(MethodNode method, FieldInsnNode insn) {
        if (insn.getOpcode() != Opcodes.GETFIELD) return false;
        
        for (FieldToMethod ftm : TranslationRules.FIELD_TO_METHODS) {
            if (insn.owner.equals(ftm.owner()) 
                && insn.name.equals(ftm.fieldName())
                && insn.desc.equals(ftm.fieldDesc())) {
                
                // Replace GETFIELD with INVOKEVIRTUAL
                insn.setOpcode(Opcodes.INVOKEVIRTUAL);
                insn.name = ftm.methodName();
                insn.desc = ftm.methodDesc();
                return true;
            }
        }
        
        return false;
    }
    
    // ================================================================
    // GETSTATIC remaps (capabilities, Material constants)
    // ================================================================
    
    private static boolean transformGetStatic(MethodNode method, FieldInsnNode insn) {
        // Capability remaps
        for (CapabilityRemap cr : TranslationRules.CAPABILITY_REMAPS) {
            if (insn.owner.equals(cr.oldOwner()) && insn.name.equals(cr.oldField())) {
                insn.owner = cr.newOwner();
                insn.name = cr.newField();
                return true;
            }
        }
        
        // Material.X → MaterialData.X (type change only, data is the same)
        if (insn.owner.equals(MATERIAL_INTERNAL)) {
            insn.owner = MATERIAL_DATA_INTERNAL;
            return true;
        }
        
        return false;
    }
    
    // ================================================================
    // Method call renames
    // ================================================================
    
    private static boolean transformMethodCall(MethodNode method, MethodInsnNode insn) {
        for (MethodRename mr : TranslationRules.METHOD_RENAMES) {
            if (mr.oldName() == null) continue; // skip placeholder entries
            if (insn.owner.equals(mr.owner())
                && insn.name.equals(mr.oldName())
                && insn.desc.equals(mr.oldDesc())) {
                insn.name = mr.newName();
                insn.desc = mr.newDesc();
                return true;
            }
        }
        
        // Remap method owner if it's a renamed class
        String newOwner = TranslationRules.CLASS_RENAMES.get(insn.owner);
        if (newOwner != null) {
            insn.owner = newOwner;
            return true;
        }
        
        // Remap event class owners
        String newEventOwner = TranslationRules.EVENT_RENAMES.get(insn.owner);
        if (newEventOwner != null) {
            insn.owner = newEventOwner;
            return true;
        }
        
        // Material → MaterialData in method owners
        if (insn.owner.equals(MATERIAL_INTERNAL)) {
            insn.owner = MATERIAL_DATA_INTERNAL;
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
    
    // ================================================================
    // Type instruction remaps (NEW, ANEWARRAY, CHECKCAST, INSTANCEOF)
    // ================================================================
    
    private static boolean transformTypeInsn(TypeInsnNode insn) {
        String renamed = TranslationRules.CLASS_RENAMES.get(insn.desc);
        if (renamed != null) { insn.desc = renamed; return true; }
        
        String eventRenamed = TranslationRules.EVENT_RENAMES.get(insn.desc);
        if (eventRenamed != null) { insn.desc = eventRenamed; return true; }
        
        if (insn.desc.equals(MATERIAL_INTERNAL)) {
            insn.desc = MATERIAL_DATA_INTERNAL;
            return true;
        }
        
        return false;
    }
    
    // ================================================================
    // Descriptor remapping (replaces class refs in type descriptors)
    // ================================================================
    
    private static String remapDescriptor(String desc) {
        if (desc == null) return null;
        String result = desc;
        
        // Class renames
        for (Map.Entry<String, String> e : TranslationRules.CLASS_RENAMES.entrySet()) {
            result = result.replace("L" + e.getKey() + ";", "L" + e.getValue() + ";");
        }
        
        // Event renames
        for (Map.Entry<String, String> e : TranslationRules.EVENT_RENAMES.entrySet()) {
            result = result.replace("L" + e.getKey() + ";", "L" + e.getValue() + ";");
        }
        
        // Material → MaterialData
        result = result.replace("L" + MATERIAL_INTERNAL + ";", "L" + MATERIAL_DATA_INTERNAL + ";");
        
        return result;
    }
    
    // ================================================================
    // Filter: which classes should be transformed
    // ================================================================
    
    public static boolean shouldTransform(String className) {
        // Don't transform ourselves
        if (className.startsWith("net/minecraftforge/compat/")) return false;
        // Don't transform vanilla/forge
        if (className.startsWith("net/minecraft/")) return false;
        if (className.startsWith("net/minecraftforge/")) return false;
        if (className.startsWith("com/mojang/")) return false;
        // Don't transform JDK
        if (className.startsWith("java/")) return false;
        if (className.startsWith("javax/")) return false;
        if (className.startsWith("org/")) return false;
        
        return true;
    }
}
