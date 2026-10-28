export default function ImplementationGuide() {
  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-bold text-white mb-2">Implementation Guide</h1>
        <p className="text-gray-400">Step-by-step guide to building the ForgeCompat compatibility layer</p>
      </div>

      {/* Phase 1 */}
      <PhaseCard
        phase={1}
        title="Project Setup & Build System"
        duration="1-2 days"
        steps={[
          {
            title: 'Create multi-module Gradle project',
            description: 'Set up the ForgeCompat project with separate modules for shim, transformer, and adapter.',
            detail: `forgecompat/
├── forgecompat-shim/       # Old API re-implementations
├── forgecompat-transformer/ # ASM/CoreMod transformer
├── forgecompat-adapter/    # Runtime behavioral patches
├── forgecompat-test/       # Test harness with Homespun
└── build.gradle`
          },
          {
            title: 'Configure Forge 1.20.1 development environment',
            description: 'Set up ForgeGradle for 1.20.1 (Forge 47.x) as the target platform.',
            detail: `// build.gradle
plugins {
    id 'net.minecraftforge.gradle' version '[6.0,6.2)'
}

minecraft {
    mappings channel: 'official', version: '1.20.1'
}

dependencies {
    minecraft 'net.minecraftforge:forge:1.20.1-47.2.0'
}`
          },
          {
            title: 'Add 1.19.2 Forge as a compile-only dependency',
            description: 'Include the 1.19.2 Forge API JAR for compilation of shim classes, but don\'t include it at runtime.',
            detail: `dependencies {
    // For compiling shim classes against old API
    compileOnly 'net.minecraftforge:forge:1.19.2-43.3.0'
    // Runtime is 1.20.1
    minecraft 'net.minecraftforge:forge:1.20.1-47.2.0'
}`
          },
        ]}
      />

      {/* Phase 2 */}
      <PhaseCard
        phase={2}
        title="Material System Shim"
        duration="2-3 days"
        steps={[
          {
            title: 'Re-implement Material class',
            description: 'Create a Material class that maps old material types to new property flags.',
            detail: `package net.minecraftforge.compat.shim.material;

public class Material {
    public static final Material WOOD = new Material(
        true,  // flammable (ignitedByLava)
        false, // not liquid
        PushReaction.NORMAL
    );
    
    public static final Material STONE = new Material(
        false, false, PushReaction.NORMAL
    );
    
    public static final Material PLANT = new Material(
        true, false, PushReaction.DESTROY
    );
    
    // ... other materials
    
    private final boolean flammable;
    private final boolean liquid;
    private final PushReaction pushReaction;
    
    public boolean isFlammable() { return flammable; }
    public boolean isLiquid() { return liquid; }
    public PushReaction getPushReaction() { return pushReaction; }
    
    // Apply to BlockBehaviour.Properties
    public BlockBehaviour.Properties applyTo(BlockBehaviour.Properties props) {
        if (flammable) props = props.ignitedByLava();
        if (liquid) props = props.liquid();
        props = props.pushReaction(pushReaction);
        return props;
    }
}`
          },
          {
            title: 'Re-implement MaterialColor as MapColor alias',
            description: 'Create MaterialColor that delegates to MapColor.',
            detail: `package net.minecraftforge.compat.shim.material;

// Simple type alias approach
public class MaterialColor extends MapColor {
    // Map all 1.19.2 MaterialColor constants to 1.20.1 MapColor
    public static final MaterialColor WOOD = 
        (MaterialColor)(Object) MapColor.WOOD;
    public static final MaterialColor STONE = 
        (MaterialColor)(Object) MapColor.STONE;
    public static final MaterialColor PLANT = 
        (MaterialColor)(Object) MapColor.PLANT;
    // ... etc
}`
          },
          {
            title: 'Shim BlockBehaviour.Properties constructor',
            description: 'Provide the old Properties.of(Material) constructor that translates to new API.',
            detail: `// In transformer: rewrite Properties.of(Material) calls
// to Properties.of() + material.applyTo(props)

// The transformer pattern:
// BEFORE: BlockBehaviour.Properties.of(Material.WOOD)
// AFTER:  Material.WOOD.applyTo(BlockBehaviour.Properties.of())`
          },
        ]}
      />

      {/* Phase 3 */}
      <PhaseCard
        phase={3}
        title="Registry System Shim"
        duration="2-3 days"
        steps={[
          {
            title: 'Implement RegistryObject<T> wrapper',
            description: 'Create RegistryObject that wraps DeferredHolder from 1.20.1.',
            detail: `package net.minecraftforge.compat.shim.registries;

public class RegistryObject<T> implements Supplier<T> {
    private final DeferredHolder<?, T> holder;
    
    public static <T> RegistryObject<T> of(DeferredHolder<?, T> holder) {
        return new RegistryObject<>(holder);
    }
    
    private RegistryObject(DeferredHolder<?, T> holder) {
        this.holder = holder;
    }
    
    @Override
    public T get() {
        return holder.get();
    }
    
    public ResourceLocation getId() {
        return holder.getId();
    }
}`
          },
          {
            title: 'Shim DeferredRegister API',
            description: 'Provide the old DeferredRegister.create() that maps to new type-specific methods.',
            detail: `package net.minecraftforge.compat.shim.registries;

public class DeferredRegister<T> {
    private final DeferredRegister<?> internal;
    
    // Old API: DeferredRegister.create(ForgeRegistries.BLOCKS, modid)
    public static DeferredRegister<Block> create(
            IForgeRegistry<Block> registry, String modid) {
        return new DeferredRegister<>(
            DeferredRegister.createBlocks(modid));
    }
    
    // Old API: register(name, supplier) -> RegistryObject
    public RegistryObject<T> register(String name, Supplier<T> supplier) {
        // Delegate to internal register, wrap result
        return RegistryObject.of(internal.register(name, supplier));
    }
    
    public void register(IEventBus bus) {
        internal.register(bus);
    }
}`
          },
        ]}
      />

      {/* Phase 4 */}
      <PhaseCard
        phase={4}
        title="CoreMod Transformer"
        duration="3-5 days"
        steps={[
          {
            title: 'Set up Forge CoreMod',
            description: 'Create the CoreMod that intercepts class loading for the target mod.',
            detail: `// src/main/resources/META-INF/services/javax.annotation.processing.Processor
// or use mods.toml:
[[mods]]
modId="forgecompat"

[[mods]]
modId="target_mod"
# In target mod's mods.toml, add:
coremods = "forgecompat_transformer.js"

// forgecompat_transformer.js
var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');

function initializeCoreMod() {
    return {
        'forgecompat_transformer': {
            'target': {
                'type': 'CLASS',
                'names': function(list) {
                    // Return all classes in target mod package
                    return list.filter(name => 
                        name.startsWith('strelka/homespun/')
                    );
                }
            },
            'transformer': function(classNode) {
                // Transform class references
                transformClassNode(classNode);
                return classNode;
            }
        }
    };
}`
          },
          {
            title: 'Implement class reference rewriting',
            description: 'Rewrite old class references to point to shim classes.',
            detail: `function transformClassNode(classNode) {
    // Rewrite Material references
    rewriteClassRef(classNode, 
        'net/minecraft/world.level/material/Material',
        'net/minecraftforge/compat/shim/material/Material');
    
    // Rewrite MaterialColor -> MapColor
    rewriteClassRef(classNode,
        'net/minecraft/world/level/material/MaterialColor',
        'net/minecraft/world/level/material/MapColor');
    
    // Rewrite RegistryObject references
    rewriteClassRef(classNode,
        'net/minecraftforge/registries/RegistryObject',
        'net/minecraftforge/compat/shim/registries/RegistryObject');
    
    // Transform method descriptors...
    transformMethodDescriptors(classNode);
}

function rewriteClassRef(classNode, oldRef, newRef) {
    // Update superclass
    if (classNode.superName === oldRef) {
        classNode.superName = newRef;
    }
    // Update interfaces
    for (var i = 0; i < classNode.interfaces.size(); i++) {
        if (classNode.interfaces.get(i) === oldRef) {
            classNode.interfaces.set(i, newRef);
        }
    }
    // Update method/field references in instructions
    // ... (iterate through methods and transform)
}`
          },
          {
            title: 'Handle method signature transformations',
            description: 'Update method calls that changed signatures between versions.',
            detail: `// Example: entity.level (field) -> entity.level() (method)
// Transform GETFIELD to INVOKEVIRTUAL
function transformFieldToMethod(methodNode, owner, fieldName, methodName, desc) {
    var instructions = methodNode.instructions;
    var iterator = instructions.iterator();
    while (iterator.hasNext()) {
        var insn = iterator.next();
        if (insn.getOpcode() === Opcodes.GETFIELD 
            && insn.owner === owner 
            && insn.name === fieldName) {
            // Replace GETFIELD with INVOKEVIRTUAL
            var newInsn = new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL, owner, methodName, desc, false);
            instructions.set(insn, newInsn);
        }
    }
}`
          },
        ]}
      />

      {/* Phase 5 */}
      <PhaseCard
        phase={5}
        title="Testing with Homespun"
        duration="2-3 days"
        steps={[
          {
            title: 'Set up test environment',
            description: 'Create a test mod that simulates the 1.19.2 Homespun code running on 1.20.1.',
            detail: `// Create a copy of Homespun's 1.19.2 source
// Compile it against ForgeCompat shim
// Load it in a 1.20.1 Forge environment

// Test checklist:
// [ ] Blocks register correctly
// [ ] Items register correctly  
// [ ] Creative tab populates
// [ ] Block properties work (flammability, push reaction)
// [ ] Fluid registration works
// [ ] Custom recipes function
// [ ] World generation triggers
// [ ] Block entities tick correctly`
          },
          {
            title: 'Iterate on missing shims',
            description: 'Run the test, identify ClassNotFound/NoSuchMethod errors, add missing shims.',
            detail: `# Common errors to watch for:
# 1. java.lang.NoSuchMethodError - method signature changed
# 2. java.lang.NoClassDefFoundError - class was removed/renamed
# 3. java.lang.AbstractMethodError - interface method added
# 4. java.lang.NoSuchFieldError - field became method
# 5. java.lang.ClassCastException - type hierarchy changed

# Each error tells you exactly what needs a shim!`
          },
        ]}
      />
    </div>
  )
}

function PhaseCard({ phase, title, duration, steps }: {
  phase: number; title: string; duration: string; steps: { title: string; description: string; detail: string }[]
}) {
  return (
    <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-orange-500/20 border border-orange-500/30 flex items-center justify-center text-orange-400 font-bold">
            {phase}
          </div>
          <h2 className="text-xl font-bold text-white">{title}</h2>
        </div>
        <span className="text-sm text-gray-400 bg-gray-800 px-3 py-1 rounded-lg">{duration}</span>
      </div>
      
      <div className="space-y-4">
        {steps.map((step, i) => (
          <div key={i} className="border border-gray-700/50 rounded-lg p-4">
            <h3 className="font-semibold text-white text-sm mb-1">{step.title}</h3>
            <p className="text-xs text-gray-400 mb-3">{step.description}</p>
            <div className="bg-gray-950 rounded p-3 border border-gray-700 overflow-x-auto">
              <pre className="text-xs text-gray-300 font-mono whitespace-pre">{step.detail}</pre>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
