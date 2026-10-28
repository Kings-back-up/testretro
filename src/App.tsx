function App() {
  return (
    <div className="min-h-screen bg-gray-950 text-gray-100">
      <div className="max-w-5xl mx-auto px-4 py-8 space-y-8">
        {/* Header */}
        <div className="border-b border-gray-800 pb-6">
          <div className="flex items-center gap-3 mb-2">
            <span className="text-3xl">⚒️</span>
            <h1 className="text-3xl font-bold text-white">ForgeCompat</h1>
            <span className="px-2 py-0.5 bg-orange-500/20 border border-orange-500/30 rounded text-orange-400 text-xs font-mono">pure translator</span>
          </div>
          <p className="text-gray-400">
            Translates 1.19.2 Forge mod bytecode directly to 1.20.1 at class load time.
            No shim classes. No fake API. No wrappers.
          </p>
        </div>

        {/* The core idea */}
        <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
          <h2 className="text-xl font-bold text-white mb-4">How it works</h2>
          <p className="text-sm text-gray-400 mb-4">
            The transformer pattern-matches old bytecode sequences and replaces them with new ones.
            No intermediate classes. The old API calls become new API calls directly.
          </p>
          <div className="bg-gray-950 rounded-lg p-4 border border-gray-700 font-mono text-xs overflow-x-auto">
            <pre className="text-gray-300">{`// 1.19.2 mod bytecode:
GETSTATIC Material.WOOD
INVOKESTATIC Properties.of(Material)Properties

// Transformer rewrites to:
INVOKESTATIC Properties.of()Properties
INVOKEVIRTUAL Properties.ignitedByLava()Properties

// No Material class exists at runtime. Flags are inlined.`}</pre>
          </div>
        </div>

        {/* Translation examples */}
        <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
          <h2 className="text-xl font-bold text-white mb-4">Translation examples</h2>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-700">
                  <th className="text-left py-2 px-3 text-gray-400">1.19.2 bytecode</th>
                  <th className="text-left py-2 px-3 text-gray-400">→</th>
                  <th className="text-left py-2 px-3 text-gray-400">1.20.1 bytecode</th>
                  <th className="text-left py-2 px-3 text-gray-400">Strategy</th>
                </tr>
              </thead>
              <tbody className="font-mono text-xs">
                {[
                  ['GETSTATIC Material.WOOD\nINVOKESTATIC Properties.of(Material)', 'Properties.of().ignitedByLava()', 'inline flags'],
                  ['GETFIELD entity.level', 'INVOKEVIRTUAL entity.level()', 'field→method'],
                  ['INVOKEVIRTUAL ServerPlayer.getLevel()', 'INVOKEVIRTUAL serverLevel()', 'rename'],
                  ['GETSTATIC CapabilityItemHandler.X', 'GETSTATIC ForgeCapabilities.X', 'remap owner+field'],
                  ['INVOKESTATIC DeferredRegister.create(ForgeRegistries.BLOCKS, id)', 'INVOKESTATIC DeferredRegister.createBlocks(id)', 'pattern rewrite'],
                  ['Material.isFlammable()', 'ICONST_1 (constant-folded)', 'inline constant'],
                  ['GETSTATIC MaterialColor.WOOD', 'GETSTATIC MapColor.WOOD', 'class rename'],
                ].map(([old, neo, strat], i) => (
                  <tr key={i} className="border-b border-gray-800/50 align-top">
                    <td className="py-3 px-3 text-red-300 whitespace-pre">{old}</td>
                    <td className="py-3 px-3 text-gray-500">→</td>
                    <td className="py-3 px-3 text-green-300">{neo}</td>
                    <td className="py-3 px-3 text-orange-400">{strat}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Project structure */}
        <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
          <h2 className="text-xl font-bold text-white mb-4">Project structure</h2>
          <div className="bg-gray-950 rounded-lg p-4 border border-gray-700 font-mono text-xs overflow-x-auto">
            <pre className="text-gray-300">{`forgecompat/src/main/java/net/minecraftforge/compat/
├── ForgeCompat.java              # @Mod entry point
├── RuntimeAdapter.java           # Minimal runtime hooks
├── translate/
│   ├── TranslationRules.java     # ALL translation rules (the data)
│   ├── MaterialData.java         # Minimal data bag for variable Materials
│   └── CompatHelpers.java        # Tiny helpers for non-inlineable cases
└── transformer/
    └── ForgeCompatTransformer.java  # The bytecode rewriter

forgecompat/src/main/resources/
├── META-INF/mods.toml            # CoreMod declaration
└── forgecompat_coremod.js        # JS bridge to Java transformer`}</pre>
          </div>
        </div>

        {/* Key insight */}
        <div className="bg-gradient-to-br from-orange-500/10 to-red-500/10 border border-orange-500/20 rounded-xl p-6">
          <h2 className="text-xl font-bold text-white mb-3">Why translation, not shims?</h2>
          <div className="grid sm:grid-cols-2 gap-4 text-sm">
            <div>
              <h3 className="text-red-400 font-semibold mb-2">Shim approach (bad):</h3>
              <ul className="space-y-1 text-gray-400">
                <li>• Re-implements the entire old API surface</li>
                <li>• Maintains fake Material, RegistryObject, etc. classes</li>
                <li>• Adds runtime overhead (wrapper indirection)</li>
                <li>• More code to maintain</li>
                <li>• Errors point to shim code, not real APIs</li>
              </ul>
            </div>
            <div>
              <h3 className="text-green-400 font-semibold mb-2">Translation approach (good):</h3>
              <ul className="space-y-1 text-gray-400">
                <li>• Rewrites old code to new code directly</li>
                <li>• No fake classes (except tiny data bags)</li>
                <li>• Zero runtime overhead</li>
                <li>• Less code, easier to maintain</li>
                <li>• Errors point to real 1.20.1 APIs</li>
              </ul>
            </div>
          </div>
        </div>

        {/* TranslationRules */}
        <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
          <h2 className="text-xl font-bold text-white mb-4">TranslationRules.java — the brain</h2>
          <p className="text-sm text-gray-400 mb-4">
            All translation logic lives in one file. To add support for a new API change, just add an entry:
          </p>
          <div className="bg-gray-950 rounded-lg p-4 border border-gray-700 font-mono text-xs overflow-x-auto">
            <pre className="text-gray-300">{`// Method rename:
new MethodRename(
    "net/minecraft/server/level/ServerPlayer",
    "getLevel", "()L...ServerLevel;",
    "serverLevel", "()L...ServerLevel;"
),

// Field → method:
new FieldToMethod(
    "net/minecraft/world/entity/Entity",
    "level", "L...Level;",
    "level", "()L...Level;"
),

// Material flags (for inlining):
MATERIAL_FLAGS.put("WOOD", new MaterialFlags(
    true,   // ignitedByLava
    false,  // liquid
    false,  // noCollission
    false,  // replaceable
    false,  // requiresCorrectTool
    MaterialFlags.PUSH_NORMAL
)),

// Capability remap:
new CapabilityRemap(
    "net/minecraftforge/common/capabilities/CapabilityItemHandler",
    "ITEM_HANDLER_CAPABILITY",
    "net/minecraftforge/common/ForgeCapabilities",
    "ITEM_HANDLER"
),`}</pre>
          </div>
        </div>

        {/* Material handling */}
        <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
          <h2 className="text-xl font-bold text-white mb-4">How Material is handled</h2>
          <p className="text-sm text-gray-400 mb-4">
            Material was removed in 1.20.1. The transformer handles two cases:
          </p>
          <div className="grid sm:grid-cols-2 gap-4">
            <div className="bg-gray-800/50 rounded-lg p-4 border border-gray-700">
              <h3 className="text-sm font-semibold text-green-400 mb-2">Case 1: Compile-time constant</h3>
              <p className="text-xs text-gray-400 mb-2">
                <code className="text-gray-300">Properties.of(Material.WOOD)</code>
              </p>
              <p className="text-xs text-gray-400">
                The transformer sees <code className="text-orange-400">GETSTATIC Material.WOOD</code> followed by <code className="text-orange-400">INVOKESTATIC Properties.of(Material)</code>, looks up WOOD's flags in <code className="text-orange-400">MATERIAL_FLAGS</code>, and inlines them directly. No Material class needed at runtime.
              </p>
            </div>
            <div className="bg-gray-800/50 rounded-lg p-4 border border-gray-700">
              <h3 className="text-sm font-semibold text-yellow-400 mb-2">Case 2: Variable Material</h3>
              <p className="text-xs text-gray-400 mb-2">
                <code className="text-gray-300">Material m = getMaterial(); Properties.of(m);</code>
              </p>
              <p className="text-xs text-gray-400">
                The transformer can't inline flags it doesn't know at transform time. It rewrites to <code className="text-orange-400">CompatHelpers.of(m)</code> where <code className="text-orange-400">m</code> is a <code className="text-orange-400">MaterialData</code> (just a data bag of flag values).
              </p>
            </div>
          </div>
        </div>

        {/* Usage */}
        <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
          <h2 className="text-xl font-bold text-white mb-4">Usage</h2>
          <div className="space-y-3 text-sm text-gray-300">
            <ol className="space-y-2 list-decimal list-inside">
              <li>Set up a Forge 1.20.1 MDK</li>
              <li>Copy <code className="text-orange-400">forgecompat/src/</code> into it</li>
              <li>Run <code className="text-orange-400">./gradlew build</code></li>
              <li>Put the output JAR + a 1.19.2 mod JAR in the mods folder</li>
              <li>Launch 1.20.1 Forge — the transformer rewrites the target mod's bytecode on load</li>
            </ol>
            <p className="text-gray-400 mt-3">
              First test target: <a href="https://github.com/PrincessStrelka/Homespun" className="text-orange-400 hover:underline" target="_blank" rel="noreferrer">Homespun</a> by PrincessStrelka.
            </p>
          </div>
        </div>

        {/* Files */}
        <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
          <h2 className="text-xl font-bold text-white mb-4">Files in this project</h2>
          <div className="space-y-2 text-sm">
            {[
              ['forgecompat/build.gradle', 'ForgeGradle 6.x for 1.20.1'],
              ['forgecompat/gradle.properties', 'MC 1.20.1, Forge 47.2.0'],
              ['forgecompat/src/main/java/.../ForgeCompat.java', '@Mod entry point'],
              ['forgecompat/src/main/java/.../RuntimeAdapter.java', 'Minimal runtime hooks'],
              ['forgecompat/src/main/java/.../translate/TranslationRules.java', 'ALL translation rules (the data)'],
              ['forgecompat/src/main/java/.../translate/MaterialData.java', 'Minimal data bag for variable Materials'],
              ['forgecompat/src/main/java/.../translate/CompatHelpers.java', 'Tiny helpers for non-inlineable cases'],
              ['forgecompat/src/main/java/.../transformer/ForgeCompatTransformer.java', 'The bytecode rewriter (the brain)'],
              ['forgecompat/src/main/resources/forgecompat_coremod.js', 'CoreMod JS bridge'],
              ['forgecompat/src/main/resources/META-INF/mods.toml', 'Mod metadata + CoreMod declaration'],
            ].map(([path, desc], i) => (
              <div key={i} className="flex flex-col sm:flex-row sm:items-center gap-1 sm:gap-3 p-2 bg-gray-800/50 rounded">
                <code className="text-orange-400 text-xs font-mono break-all">{path}</code>
                <span className="text-gray-500 text-xs">— {desc}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}

export default App
