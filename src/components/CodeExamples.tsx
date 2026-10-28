import { useState } from 'react'

interface CodeExample {
  id: string
  title: string
  description: string
  before: string
  after: string
  language: string
}

const examples: CodeExample[] = [
  {
    id: 'material',
    title: 'Material System → Properties',
    description: 'How the Material class is shimmed to work with the new Properties system',
    language: 'java',
    before: `// 1.19.2 - Material-based block properties
public static final BlockBehaviour.Properties WOOD_PROPS = 
    BlockBehaviour.Properties.of(Material.WOOD)
        .strength(2.0F, 3.0F)
        .sound(SoundType.WOOD)
        .color(MaterialColor.WOOD);`,
    after: `// 1.20.1 - Direct properties (no Material)
public static final BlockBehaviour.Properties WOOD_PROPS = 
    BlockBehaviour.Properties.of()
        .strength(2.0F, 3.0F)
        .sound(SoundType.WOOD)
        .mapColor(MapColor.WOOD)
        .ignitedByLava();  // replaces Material.WOOD flammability

// Shim approach: Transformer rewrites to:
// Material.WOOD.applyTo(BlockBehaviour.Properties.of())
//     .strength(2.0F, 3.0F)
//     .sound(SoundType.WOOD)
//     .mapColor(MapColor.WOOD);`
  },
  {
    id: 'registry',
    title: 'DeferredRegister & RegistryObject',
    description: 'How the registry system is shimmed between versions',
    language: 'java',
    before: `// 1.19.2 - Old DeferredRegister API
public static final DeferredRegister<Block> BLOCKS = 
    DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);

public static final RegistryObject<Block> MY_BLOCK = 
    BLOCKS.register("my_block", () -> new Block(
        BlockBehaviour.Properties.of(Material.STONE)
    ));

// Registration
BLOCKS.register(modEventBus);`,
    after: `// 1.20.1 - New DeferredRegister API
public static final DeferredRegister.Blocks BLOCKS = 
    DeferredRegister.createBlocks(MOD_ID);

public static final DeferredBlock<Block> MY_BLOCK = 
    BLOCKS.register("my_block", () -> new Block(
        BlockBehaviour.Properties.of()
    ));

// Registration (same)
BLOCKS.register(modEventBus);

// Shim: RegistryObject wraps DeferredHolder
// Transformer rewrites RegistryObject<T> → shim.RegistryObject<T>
// which internally holds a DeferredHolder<?, T>`
  },
  {
    id: 'entity-fields',
    title: 'Entity Field → Method Access',
    description: 'How field access is transformed to method calls',
    language: 'java',
    before: `// 1.19.2 - Direct field access
public void doSomething(Entity entity) {
    Level level = entity.level;           // field access
    boolean grounded = entity.isOnGround;  // field access
    
    if (entity instanceof ServerPlayer sp) {
        ServerLevel sLevel = sp.getLevel();
    }
}`,
    after: `// 1.20.1 - Method accessors
public void doSomething(Entity entity) {
    Level level = entity.level();           // method call
    boolean grounded = entity.onGround();   // method call
    
    if (entity instanceof ServerPlayer sp) {
        ServerLevel sLevel = sp.serverLevel();
    }
}

// Transformer converts:
// GETFIELD entity.level → INVOKEVIRTUAL entity.level()
// GETFIELD entity.isOnGround → INVOKEVIRTUAL entity.onGround()`
  },
  {
    id: 'creative-tabs',
    title: 'Creative Mode Tab Registration',
    description: 'How creative tabs are bridged between versions',
    language: 'java',
    before: `// 1.19.2 - Old creative tab system
public static final DeferredRegister<CreativeModeTab> TABS = 
    DeferredRegister.create(Registry.CREATIVE_MODE_TAB_REGISTRY, MOD_ID);

public static final RegistryObject<CreativeModeTab> MY_TAB = 
    TABS.register("my_tab", () -> CreativeModeTab.builder()
        .icon(() -> new ItemStack(MY_BLOCK.get()))
        .displayItems((params, output) -> {
            output.accept(MY_BLOCK.get());
            output.accept(MY_ITEM.get());
        })
        .build());`,
    after: `// 1.20.1 - New creative tab system
public static final DeferredRegister<CreativeModeTab> TABS = 
    DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MY_TAB = 
    TABS.register("my_tab", () -> CreativeModeTab.builder()
        .icon(() -> new ItemStack(MY_BLOCK.get()))
        .displayItems((params, output) -> {
            output.accept(MY_BLOCK.get());
            output.accept(MY_ITEM.get());
        })
        .build());

// Shim: Registry<CreativeModeTab> → Registries.CREATIVE_MODE_TAB
// RegistryObject → DeferredHolder wrapper`
  },
  {
    id: 'network',
    title: 'Network Message Handling',
    description: 'How the network consumer API is shimmed',
    language: 'java',
    before: `// 1.19.2 - Old network consumer
channel.registerMessage(id, MyMessage.class, 
    MyMessage::encode,
    MyMessage::decode,
    MyMessage::handle,  // single consumer method
    Optional.of(NetworkDirection.PLAY_TO_SERVER));

// In the message handler:
public static void handle(MyMessage msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
        // handle on main thread
    });
    ctx.get().setPacketHandled(true);
}`,
    after: `// 1.20.1 - New network consumer (split)
channel.messageBuilder(MyMessage.class, id, NetworkDirection.PLAY_TO_SERVER)
    .decoder(MyMessage::decode)
    .encoder(MyMessage::encode)
    .consumerMainThread(MyMessage::handleMain)  // or consumerNetworkThread
    .register();

// Split into two methods:
public static void handleMain(MyMessage msg, Supplier<NetworkEvent.Context> ctx) {
    // Already on main thread, no enqueueWork needed
}

// Shim: old single consumer → consumerMainThread
// Transformer rewrites registerMessage to messageBuilder pattern`
  },
  {
    id: 'events',
    title: 'Event System Changes',
    description: 'How renamed/changed events are handled',
    language: 'java',
    before: `// 1.19.2 - Old event names
@SubscribeEvent
public void onTargetChange(LivingSetAttackTargetEvent event) {
    LivingEntity entity = event.getEntity();
    LivingEntity target = event.getTarget();
    // ...
}

@SubscribeEvent  
public void onRenderLast(RenderLevelLastEvent event) {
    PoseStack poseStack = event.getPoseStack();
    // render stuff
}

@SubscribeEvent
public void onSaplingGrow(SaplingGrowTreeEvent event) {
    // ...
}`,
    after: `// 1.20.1 - New event names
@SubscribeEvent
public void onTargetChange(LivingChangeTargetEvent event) {
    LivingEntity entity = event.getEntity();
    LivingEntity target = event.getNewTarget();  // also renamed!
    // ...
}

@SubscribeEvent
public void onRenderStage(RenderLevelStageEvent event) {
    PoseStack poseStack = event.getPoseStack();
    // Must check event.getStage() for appropriate stage
    if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
        // render stuff
    }
}

@SubscribeEvent
public void onFeatureGrow(BlockGrowFeatureEvent event) {
    // ...
}

// Shim: Adapter layer maps old events to new ones
// LivingSetAttackTargetEvent extends LivingChangeTargetEvent`
  },
  {
    id: 'capabilities',
    title: 'Capability System',
    description: 'How capability references are updated',
    language: 'java',
    before: `// 1.19.2 - Old capability references
import net.minecraftforge.common.capabilities.CapabilityItemHandler;
import net.minecraftforge.common.capabilities.CapabilityFluidHandler;
import net.minecraftforge.common.capabilities.CapabilityEnergy;

// Usage:
lazyCap = LazyOptional.of(() -> new ItemHandler())
    .cast(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);

// Checking capabilities:
if (tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).isPresent()) {
    // ...
}`,
    after: `// 1.20.1 - New capability references
import net.minecraftforge.common.ForgeCapabilities;

// Usage:
lazyCap = LazyOptional.of(() -> new ItemHandler())
    .cast(ForgeCapabilities.ITEM_HANDLER);

// Checking capabilities:
if (tileEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) {
    // ...
}

// Shim: Simple class reference rewrite
// CapabilityItemHandler.ITEM_HANDLER_CAPABILITY 
//   → ForgeCapabilities.ITEM_HANDLER`
  },
]

export default function CodeExamples() {
  const [activeTab, setActiveTab] = useState(examples[0].id)
  const [showDiff, setShowDiff] = useState(false)
  
  const active = examples.find(e => e.id === activeTab)!

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-bold text-white mb-2">Code Examples</h1>
        <p className="text-gray-400">Before/after comparisons showing how the compatibility layer transforms code</p>
      </div>

      {/* Tab Navigation */}
      <div className="flex flex-wrap gap-2">
        {examples.map(ex => (
          <button
            key={ex.id}
            onClick={() => setActiveTab(ex.id)}
            className={`px-3 py-2 rounded-lg text-sm font-medium transition-all ${
              activeTab === ex.id
                ? 'bg-orange-500/20 text-orange-400 border border-orange-500/30'
                : 'bg-gray-800 text-gray-400 border border-gray-700 hover:text-gray-200'
            }`}
          >
            {ex.title}
          </button>
        ))}
      </div>

      {/* Active Example */}
      <div className="bg-gray-900 border border-gray-800 rounded-xl overflow-hidden">
        <div className="p-4 border-b border-gray-800">
          <h3 className="text-lg font-bold text-white">{active.title}</h3>
          <p className="text-sm text-gray-400 mt-1">{active.description}</p>
        </div>

        <div className="grid md:grid-cols-2 divide-y md:divide-y-0 md:divide-x divide-gray-800">
          {/* Before */}
          <div className="p-4">
            <div className="flex items-center gap-2 mb-3">
              <span className="w-3 h-3 bg-red-500 rounded-full" />
              <span className="text-sm font-semibold text-red-400">1.19.2 (Original)</span>
            </div>
            <div className="bg-gray-950 rounded-lg p-4 border border-gray-700 overflow-x-auto">
              <pre className="text-xs text-gray-300 font-mono whitespace-pre">{active.before}</pre>
            </div>
          </div>

          {/* After */}
          <div className="p-4">
            <div className="flex items-center gap-2 mb-3">
              <span className="w-3 h-3 bg-green-500 rounded-full" />
              <span className="text-sm font-semibold text-green-400">1.20.1 (Target)</span>
            </div>
            <div className="bg-gray-950 rounded-lg p-4 border border-gray-700 overflow-x-auto">
              <pre className="text-xs text-gray-300 font-mono whitespace-pre">{active.after}</pre>
            </div>
          </div>
        </div>
      </div>

      {/* Transformer Pattern */}
      <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
        <h2 className="text-xl font-bold text-white mb-4">🔧 Transformer Pattern</h2>
        <p className="text-sm text-gray-400 mb-4">
          Here's how the ASM transformer would handle a typical class transformation for the Homespun mod:
        </p>
        <div className="bg-gray-950 rounded-lg p-4 border border-gray-700 overflow-x-auto">
          <pre className="text-xs text-gray-300 font-mono whitespace-pre">{`// forgecompat-transformer/src/main/java/.../HomespunTransformer.java

public class HomespunTransformer implements IClassTransformer {
    
    // Class reference mappings: old -> new
    private static final Map<String, String> CLASS_REMAPS = Map.of(
        "net/minecraft/world/level/material/Material",
            "net/minecraftforge/compat/shim/material/Material",
        "net/minecraftforge/registries/RegistryObject",
            "net/minecraftforge/compat/shim/registries/RegistryObject"
    );
    
    // Method reference mappings
    private static final Map<MethodRef, MethodRef> METHOD_REMAPS = Map.of(
        new MethodRef("net/minecraft/world/entity/Entity", "level", 
            "Lnet/minecraft/world/level/Level;"),
        new MethodRef("net/minecraft/world/entity/Entity", "level", 
            "()Lnet/minecraft/world/level/Level;"),
        // ... more mappings
    );
    
    @Override
    public ClassNode transform(String name, ClassNode node, boolean runtime) {
        if (!name.startsWith("strelka/homespun/")) return node;
        
        // Phase 1: Remap class references
        remapClassReferences(node);
        
        // Phase 2: Transform field access to method calls
        transformFieldAccess(node);
        
        // Phase 3: Update method descriptors
        remapMethods(node);
        
        // Phase 4: Inject adapter calls where needed
        injectAdapters(node);
        
        return node;
    }
}`}</pre>
        </div>
      </div>

      {/* Toggle view */}
      <div className="flex items-center gap-4">
        <button
          onClick={() => setShowDiff(!showDiff)}
          className="px-4 py-2 bg-orange-500/20 border border-orange-500/30 rounded-lg text-orange-400 text-sm font-medium hover:bg-orange-500/30 transition-all"
        >
          {showDiff ? 'Hide' : 'Show'} All Examples Summary
        </button>
      </div>

      {showDiff && (
        <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
          <h3 className="text-lg font-bold text-white mb-4">Quick Reference Summary</h3>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-700">
                  <th className="text-left py-2 px-3 text-gray-400">Pattern</th>
                  <th className="text-left py-2 px-3 text-gray-400">1.19.2</th>
                  <th className="text-left py-2 px-3 text-gray-400">1.20.1</th>
                  <th className="text-left py-2 px-3 text-gray-400">Strategy</th>
                </tr>
              </thead>
              <tbody className="text-xs">
                <tr className="border-b border-gray-800">
                  <td className="py-2 px-3 text-gray-300">Materials</td>
                  <td className="py-2 px-3 text-red-300 font-mono">Properties.of(Material)</td>
                  <td className="py-2 px-3 text-green-300 font-mono">Properties.of()</td>
                  <td className="py-2 px-3 text-orange-400">Shim + Transform</td>
                </tr>
                <tr className="border-b border-gray-800">
                  <td className="py-2 px-3 text-gray-300">Registry</td>
                  <td className="py-2 px-3 text-red-300 font-mono">RegistryObject&lt;T&gt;</td>
                  <td className="py-2 px-3 text-green-300 font-mono">DeferredHolder&lt;R,T&gt;</td>
                  <td className="py-2 px-3 text-orange-400">Shim wrapper</td>
                </tr>
                <tr className="border-b border-gray-800">
                  <td className="py-2 px-3 text-gray-300">Entity fields</td>
                  <td className="py-2 px-3 text-red-300 font-mono">entity.level</td>
                  <td className="py-2 px-3 text-green-300 font-mono">entity.level()</td>
                  <td className="py-2 px-3 text-orange-400">ASM Transform</td>
                </tr>
                <tr className="border-b border-gray-800">
                  <td className="py-2 px-3 text-gray-300">Events</td>
                  <td className="py-2 px-3 text-red-300 font-mono">LivingSetAttackTarget</td>
                  <td className="py-2 px-3 text-green-300 font-mono">LivingChangeTarget</td>
                  <td className="py-2 px-3 text-orange-400">Event adapter</td>
                </tr>
                <tr className="border-b border-gray-800">
                  <td className="py-2 px-3 text-gray-300">Capabilities</td>
                  <td className="py-2 px-3 text-red-300 font-mono">CapabilityItemHandler</td>
                  <td className="py-2 px-3 text-green-300 font-mono">ForgeCapabilities</td>
                  <td className="py-2 px-3 text-orange-400">Class remap</td>
                </tr>
                <tr>
                  <td className="py-2 px-3 text-gray-300">Network</td>
                  <td className="py-2 px-3 text-red-300 font-mono">.consumer()</td>
                  <td className="py-2 px-3 text-green-300 font-mono">.consumerMainThread()</td>
                  <td className="py-2 px-3 text-orange-400">ASM Transform</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}
