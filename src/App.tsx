import { useState } from 'react'

function App() {
  const [activeTab, setActiveTab] = useState<'overview' | 'structure' | 'transformer' | 'usage'>('overview')

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100">
      <div className="max-w-6xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="mb-8">
          <div className="flex items-center gap-3 mb-2">
            <span className="text-3xl">⚒️</span>
            <h1 className="text-3xl font-bold text-white">ForgeCompat</h1>
            <span className="px-2 py-0.5 bg-orange-500/20 border border-orange-500/30 rounded text-orange-400 text-xs font-mono">v0.1.0</span>
          </div>
          <p className="text-gray-400">
            A Forge CoreMod compatibility layer that enables 1.19.2 mods to run on 1.20.1 via bytecode transformation and API shims.
          </p>
        </div>

        {/* Tabs */}
        <div className="flex gap-2 mb-6 flex-wrap">
          {(['overview', 'structure', 'transformer', 'usage'] as const).map(tab => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${
                activeTab === tab
                  ? 'bg-orange-500/20 text-orange-400 border border-orange-500/30'
                  : 'bg-gray-800 text-gray-400 border border-gray-700 hover:text-gray-200'
              }`}
            >
              {tab.charAt(0).toUpperCase() + tab.slice(1)}
            </button>
          ))}
        </div>

        {/* Content */}
        {activeTab === 'overview' && (
          <div className="space-y-6">
            <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
              <h2 className="text-xl font-bold text-white mb-4">How It Works</h2>
              <div className="space-y-4 text-sm text-gray-300">
                <p>ForgeCompat intercepts the target mod's classes as they're loaded by the JVM and rewrites them to use the 1.20.1 API:</p>
                <div className="bg-gray-950 rounded-lg p-4 border border-gray-700 font-mono text-xs overflow-x-auto">
                  <pre className="text-gray-300">{`1. Target mod JAR (compiled for 1.19.2) is loaded
2. Forge's CoreMod system invokes forgecompat_coremod.js
3. JS bridge calls Java ForgeCompatTransformer.transform()
4. Transformer rewrites bytecode:
   - Class refs: Material → shim.Material
   - Fields: entity.level → entity.level()
   - Methods: getLevel() → serverLevel()
   - Events: LivingSetAttackTarget → shim subclass
5. Rewritten classes run on 1.20.1 Forge`}</pre>
                </div>
              </div>
            </div>

            <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
              <h2 className="text-xl font-bold text-white mb-4">What Gets Transformed</h2>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-gray-700">
                      <th className="text-left py-2 px-3 text-gray-400">1.19.2 API</th>
                      <th className="text-left py-2 px-3 text-gray-400">1.20.1 API</th>
                      <th className="text-left py-2 px-3 text-gray-400">Strategy</th>
                    </tr>
                  </thead>
                  <tbody className="font-mono text-xs">
                    {[
                      ['Properties.of(Material)', 'Properties.of().ignitedByLava()', 'Shim + Transform'],
                      ['RegistryObject<T>', 'DeferredHolder<R,T>', 'Shim wrapper'],
                      ['entity.level (field)', 'entity.level() (method)', 'ASM GETFIELD→INVOKEVIRTUAL'],
                      ['entity.isOnGround (field)', 'entity.onGround() (method)', 'ASM GETFIELD→INVOKEVIRTUAL'],
                      ['ServerPlayer.getLevel()', 'ServerPlayer.serverLevel()', 'Method name remap'],
                      ['LivingSetAttackTargetEvent', 'LivingChangeTargetEvent', 'Shim subclass'],
                      ['CapabilityItemHandler.*', 'ForgeCapabilities.*', 'GETSTATIC owner remap'],
                      ['SimpleChannel.registerMessage()', 'messageBuilder().consumerMainThread()', 'Shim wrapper'],
                    ].map(([old, neo, strat], i) => (
                      <tr key={i} className="border-b border-gray-800/50">
                        <td className="py-2 px-3 text-red-300">{old}</td>
                        <td className="py-2 px-3 text-green-300">{neo}</td>
                        <td className="py-2 px-3 text-orange-400">{strat}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
              <h2 className="text-xl font-bold text-white mb-4">Key Files</h2>
              <div className="space-y-2 text-sm">
                <FileItem path="forgecompat/src/main/java/.../ForgeCompat.java" desc="Main @Mod class" />
                <FileItem path="forgecompat/src/main/java/.../transformer/ForgeCompatTransformer.java" desc="ASM bytecode transformer (the brain)" />
                <FileItem path="forgecompat/src/main/java/.../shim/material/Material.java" desc="Re-implements the removed Material class" />
                <FileItem path="forgecompat/src/main/java/.../shim/registries/RegistryObject.java" desc="Wraps DeferredHolder as old RegistryObject" />
                <FileItem path="forgecompat/src/main/resources/forgecompat_coremod.js" desc="CoreMod JS bridge to Java transformer" />
                <FileItem path="forgecompat/src/main/resources/META-INF/mods.toml" desc="Mod metadata + CoreMod declaration" />
              </div>
            </div>
          </div>
        )}

        {activeTab === 'structure' && (
          <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
            <h2 className="text-xl font-bold text-white mb-4">Project Structure</h2>
            <div className="bg-gray-950 rounded-lg p-4 border border-gray-700 font-mono text-xs overflow-x-auto">
              <pre className="text-gray-300">{`forgecompat/
├── build.gradle                          # ForgeGradle 6.x for 1.20.1
├── gradle.properties                     # MC 1.20.1, Forge 47.2.0
├── settings.gradle
├── README.md
└── src/main/
    ├── java/net/minecraftforge/compat/
    │   ├── ForgeCompat.java              # @Mod entry point
    │   ├── RuntimeAdapter.java           # Event bus hooks
    │   │
    │   ├── shim/
    │   │   ├── material/
    │   │   │   ├── Material.java         # All 1.19.2 material constants
    │   │   │   ├── MaterialColor.java    # MaterialColor → MapColor alias
    │   │   │   └── MaterialCompat.java   # propertiesOf(Material) helper
    │   │   │
    │   │   ├── registries/
    │   │   │   ├── RegistryObject.java   # Wraps DeferredHolder
    │   │   │   ├── ShimDeferredRegister.java  # create(ForgeRegistries.X, modid)
    │   │   │   └── ShimForgeRegistry.java     # BLOCKS/ITEMS/FLUIDS markers
    │   │   │
    │   │   ├── entity/
    │   │   │   └── EntityCompat.java     # getLevel/isOnGround helpers
    │   │   │
    │   │   ├── events/
    │   │   │   └── LivingSetAttackTargetEvent.java  # Extends LivingChangeTargetEvent
    │   │   │
    │   │   ├── network/
    │   │   │   └── ShimSimpleChannel.java  # registerMessage() wrapper
    │   │   │
    │   │   └── capabilities/
    │   │       └── CapabilityCompat.java   # ITEM_HANDLER_CAPABILITY etc.
    │   │
    │   └── transformer/
    │       └── ForgeCompatTransformer.java # THE CORE: ASM class rewriting
    │
    └── resources/
        ├── META-INF/
        │   └── mods.toml                 # CoreMod declaration
        └── forgecompat_coremod.js        # JS → Java bridge`}</pre>
            </div>
          </div>
        )}

        {activeTab === 'transformer' && (
          <div className="space-y-6">
            <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
              <h2 className="text-xl font-bold text-white mb-4">ForgeCompatTransformer.java</h2>
              <p className="text-sm text-gray-400 mb-4">
                This is the core of the system. It uses ASM to rewrite the target mod's bytecode as classes are loaded.
              </p>
              <div className="bg-gray-950 rounded-lg p-4 border border-gray-700 font-mono text-xs overflow-x-auto">
                <pre className="text-gray-300">{`// The transformer runs 3 phases on each target class:

Phase 1: Class Hierarchy Remapping
  ├── superclass: old class → shim class
  └── interfaces: old interfaces → shim interfaces

Phase 2: Method Transformation (for each method)
  ├── Remap method descriptor types
  ├── Transform GETFIELD → INVOKEVIRTUAL (entity.level → entity.level())
  ├── Transform INVOKESTATIC/INVOKEVIRTUAL (method renames)
  ├── Transform Properties.of(Material) → MaterialCompat.propertiesOf(Material)
  ├── Remap capability GETSTATIC owners
  └── Remap local variable descriptors

Phase 3: Field Descriptor Remapping
  └── Replace old class refs in field type descriptors

// Example transformation for Homespun's ModBlocks.java:
// BEFORE: BlockBehaviour.Properties.of(Material.WOOD)
//   ALOAD Material.WOOD (GETSTATIC)
//   INVOKESTATIC Properties.of(Material)Properties
//
// AFTER:
//   ALOAD Material.WOOD (GETSTATIC → now loads shim.Material.WOOD)
//   INVOKESTATIC MaterialCompat.propertiesOf(Material)Properties`}</pre>
              </div>
            </div>

            <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
              <h2 className="text-xl font-bold text-white mb-4">Class Remap Table</h2>
              <div className="overflow-x-auto">
                <table className="w-full text-xs font-mono">
                  <thead>
                    <tr className="border-b border-gray-700">
                      <th className="text-left py-2 px-3 text-gray-400">Old (1.19.2)</th>
                      <th className="text-left py-2 px-3 text-gray-400">New (shim/1.20.1)</th>
                    </tr>
                  </thead>
                  <tbody className="text-gray-300">
                    {[
                      ['net/minecraft/world/level/material/Material', 'net/minecraftforge/compat/shim/material/Material'],
                      ['net/minecraft/world/level/material/MaterialColor', 'net/minecraftforge/compat/shim/material/MaterialColor'],
                      ['net/minecraftforge/registries/RegistryObject', 'net/minecraftforge/compat/shim/registries/RegistryObject'],
                      ['net/minecraftforge/registries/ForgeRegistries', 'net/minecraftforge/compat/shim/registries/ShimForgeRegistry'],
                      ['net/minecraftforge/event/entity/living/LivingSetAttackTargetEvent', 'net/minecraftforge/compat/shim/events/LivingSetAttackTargetEvent'],
                      ['net/minecraftforge/common/capabilities/CapabilityItemHandler', 'net/minecraftforge/compat/shim/capabilities/CapabilityCompat'],
                    ].map(([old, neo], i) => (
                      <tr key={i} className="border-b border-gray-800/50">
                        <td className="py-2 px-3 text-red-300">{old}</td>
                        <td className="py-2 px-3 text-green-300">{neo}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
              <h2 className="text-xl font-bold text-white mb-4">Field → Method Transforms</h2>
              <div className="overflow-x-auto">
                <table className="w-full text-xs font-mono">
                  <thead>
                    <tr className="border-b border-gray-700">
                      <th className="text-left py-2 px-3 text-gray-400">Owner</th>
                      <th className="text-left py-2 px-3 text-gray-400">Old (GETFIELD)</th>
                      <th className="text-left py-2 px-3 text-gray-400">New (INVOKEVIRTUAL)</th>
                    </tr>
                  </thead>
                  <tbody className="text-gray-300">
                    <tr className="border-b border-gray-800/50">
                      <td className="py-2 px-3">Entity</td>
                      <td className="py-2 px-3 text-red-300">level : Level</td>
                      <td className="py-2 px-3 text-green-300">level() : Level</td>
                    </tr>
                    <tr className="border-b border-gray-800/50">
                      <td className="py-2 px-3">Entity</td>
                      <td className="py-2 px-3 text-red-300">isOnGround : boolean</td>
                      <td className="py-2 px-3 text-green-300">onGround() : boolean</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'usage' && (
          <div className="space-y-6">
            <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
              <h2 className="text-xl font-bold text-white mb-4">Building the Mod</h2>
              <div className="bg-gray-950 rounded-lg p-4 border border-gray-700 font-mono text-sm">
                <pre className="text-gray-300">{`# Clone/copy the forgecompat/ directory
cd forgecompat

# Requires Java 17+
./gradlew build

# Output: build/libs/forgecompat-0.1.0.jar`}</pre>
              </div>
            </div>

            <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
              <h2 className="text-xl font-bold text-white mb-4">Using with Homespun</h2>
              <div className="space-y-3 text-sm text-gray-300">
                <ol className="space-y-2 list-decimal list-inside">
                  <li>Build the 1.19.2 version of Homespun (or use the source from the <code className="text-orange-400">master</code> branch before its NeoForge port)</li>
                  <li>Place <code className="text-orange-400">forgecompat-0.1.0.jar</code> in your 1.20.1 Forge mods folder</li>
                  <li>Place the Homespun 1.19.2 JAR in the same folder</li>
                  <li>Launch Minecraft 1.20.1 with Forge 47.2.0</li>
                  <li>Check the log for <code className="text-orange-400">[ForgeCompat] Transformed class strelka/homespun/...</code></li>
                  <li>Verify blocks, items, creative tab, fluids, and recipes work</li>
                </ol>
              </div>
            </div>

            <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
              <h2 className="text-xl font-bold text-white mb-4">Debugging</h2>
              <div className="bg-gray-950 rounded-lg p-4 border border-gray-700 font-mono text-xs overflow-x-auto">
                <pre className="text-gray-300">{`# Enable debug logging in run/forge.cfg or via JVM args:
-Dforge.logging.console.level=debug

# Common errors and fixes:
# 1. NoSuchMethodError: Properties.of(Material)
#    → Transformer didn't catch the class. Check shouldTransform() filter.
#
# 2. NoClassDefFoundError: Material
#    → The shim Material class isn't on the classpath. Check build.gradle deps.
#
# 3. ClassCastException: RegistryObject → DeferredHolder
#    → Transformer missed a RegistryObject reference. Add to CLASS_REMAPS.
#
# 4. AbstractMethodError on event handler
#    → The old event signature doesn't match. Add to event shim.
#
# 5. Missing blocks/items in creative tab
#    → Creative tab registration API changed. Check ShimDeferredRegister.`}</pre>
              </div>
            </div>

            <div className="bg-gradient-to-br from-orange-500/10 to-red-500/10 border border-orange-500/20 rounded-xl p-6">
              <h2 className="text-xl font-bold text-white mb-3">⚠️ Current Status</h2>
              <p className="text-sm text-gray-300 mb-3">
                This is the initial skeleton. To get it working with Homespun, you'll need to:
              </p>
              <ul className="space-y-1.5 text-sm text-gray-400">
                <li className="flex gap-2"><span className="text-orange-400">1.</span> Set up a Forge 1.20.1 dev environment and get the project compiling</li>
                <li className="flex gap-2"><span className="text-orange-400">2.</span> Create a minimal test mod (just one block) using 1.19.2 API patterns</li>
                <li className="flex gap-2"><span className="text-orange-400">3.</span> Run it with ForgeCompat and iterate on errors</li>
                <li className="flex gap-2"><span className="text-orange-400">4.</span> Once the minimal test works, try the full Homespun mod</li>
                <li className="flex gap-2"><span className="text-orange-400">5.</span> Add more shims/transforms as needed for Homespun's specific APIs</li>
              </ul>
              <p className="text-sm text-gray-400 mt-3">
                The Material system, Registry system, and Entity field→method transforms are the three biggest pieces. 
                Get those working first and most of Homespun should follow.
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

function FileItem({ path, desc }: { path: string; desc: string }) {
  return (
    <div className="flex flex-col sm:flex-row sm:items-center gap-1 sm:gap-3 p-2 bg-gray-800/50 rounded">
      <code className="text-orange-400 text-xs font-mono break-all">{path}</code>
      <span className="text-gray-500 text-xs">— {desc}</span>
    </div>
  )
}

export default App
