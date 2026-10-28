export default function ArchitectureSection() {
  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-bold text-white mb-2">Architecture Design</h1>
        <p className="text-gray-400">How the compatibility layer is structured and how it intercepts/modifies mod code</p>
      </div>

      {/* System Diagram */}
      <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
        <h2 className="text-xl font-bold text-white mb-6">System Overview</h2>
        <div className="bg-gray-950 rounded-lg p-6 border border-gray-700 font-mono text-sm overflow-x-auto">
          <pre className="text-gray-300">{`
┌─────────────────────────────────────────────────────────────────────┐
│                        Minecraft 1.20.1 + Forge 47.x                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────┐    ┌──────────────────┐    ┌──────────────────┐  │
│  │  Target Mod   │    │   ForgeCompat    │    │  Forge 47.x API  │  │
│  │ (1.19.2 code) │───▶│  Shim Library    │───▶│  (1.20.1 native) │  │
│  │              │    │                  │    │                  │  │
│  │ • Old imports│    │ • Old API stubs  │    │ • New classes    │  │
│  │ • Old methods│    │ • Translators    │    │ • New methods    │  │
│  │ • Old events │    │ • Adapters       │    │ • New events     │  │
│  └──────────────┘    └──────────────────┘    └──────────────────┘  │
│         │                      │                       │            │
│         │         ┌────────────┴────────────┐          │            │
│         │         │   Bytecode Transformer  │          │            │
│         │         │   (ASM/CoreMods)        │          │            │
│         │         │                         │          │            │
│         │         │ • Rewrite class refs    │          │            │
│         │         │ • Transform method sigs │          │            │
│         │         │ • Inject adapter calls  │          │            │
│         │         └─────────────────────────┘          │            │
│         │                                               │            │
│  ┌──────┴───────────────────────────────────────────────┴──────┐    │
│  │                    ClassLoader Layer                         │    │
│  │  • Loads original mod JAR                                    │    │
│  │  • Applies transformations on class load                     │    │
│  │  • Resolves shim references to ForgeCompat library           │    │
│  └──────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
          `}</pre>
        </div>
      </div>

      {/* Component Details */}
      <div className="grid gap-6">
        <ComponentCard
          title="1. API Shim Library (forgecompat-shim)"
          description="A Java library that provides the old 1.19.2 API surface. When the target mod references classes/methods that no longer exist in 1.20.1, these shim classes provide compatible implementations that delegate to the new APIs."
          items={[
            { label: 'Package Structure', value: 'net.minecraftforge.compat.shim.*' },
            { label: 'Material Compat', value: 'Re-implements Material class, maps to BlockBehaviour.Properties flags' },
            { label: 'Registry Compat', value: 'DeferredRegister shim that wraps new DeferredRegister API' },
            { label: 'Event Compat', value: 'Old event classes that extend/wrap new event classes' },
            { label: 'Network Compat', value: 'Old SimpleChannel API wrapping new network system' },
          ]}
        />

        <ComponentCard
          title="2. Bytecode Transformer (forgecompat-transformer)"
          description="A CoreMod/ASM transformer that intercepts class loading and rewrites the target mod's bytecode to redirect old API calls to the shim layer."
          items={[
            { label: 'Technology', value: 'Forge CoreMods (JavaScript/ASM) or Java Agent' },
            { label: 'Class Remapping', value: 'Renames class references (e.g., Material → shim.Material)' },
            { label: 'Method Signature Rewrite', value: 'Updates method descriptors to match new signatures' },
            { label: 'Field Access Redirect', value: 'Redirects field access to getter/setter methods' },
            { label: 'Interface Injection', value: 'Adds new required interfaces to existing classes' },
          ]}
        />

        <ComponentCard
          title="3. Runtime Adapter (forgecompat-adapter)"
          description="Handles behavioral differences that can't be resolved at compile/transform time. This includes event timing changes, data format migrations, and behavioral patches."
          items={[
            { label: 'Event Bus Adapter', value: 'Translates between old and new event hierarchies' },
            { label: 'Data Migration', value: 'Handles NBT/CompoundTag format changes for saved data' },
            { label: 'Registry Sync', value: 'Ensures mod registries are properly initialized in new order' },
            { label: 'Behavioral Patches', value: 'Fixes subtle behavior differences via Mixin/ASM' },
          ]}
        />
      </div>

      {/* Loading Order */}
      <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
        <h2 className="text-xl font-bold text-white mb-4">Initialization Order</h2>
        <div className="space-y-3">
          <StepItem step={1} title="ForgeCompat CoreMod registers" description="Registers the ASM transformer with Forge's loading system via mods.toml" />
          <StepItem step={2} title="Transformer intercepts target mod classes" description="As the target mod's classes are loaded, the transformer rewrites references" />
          <StepItem step={3} title="Shim library classes are loaded" description="Old API classes from the shim are available on the classpath" />
          <StepItem step={4} title="Target mod's @Mod constructor runs" description="The mod initializes using what it thinks is the 1.19.2 API" />
          <StepItem step={5} title="Runtime adapters handle behavioral differences" description="Event bus hooks and mixins ensure correct runtime behavior" />
        </div>
      </div>

      {/* Limitations */}
      <div className="bg-gray-900 border border-red-900/30 rounded-xl p-6">
        <h2 className="text-xl font-bold text-white mb-4">⚠️ Known Limitations</h2>
        <div className="grid sm:grid-cols-2 gap-4 text-sm">
          <div className="space-y-2">
            <h3 className="text-red-400 font-semibold">Cannot Be Shimmed:</h3>
            <ul className="space-y-1 text-gray-400">
              <li>• Direct access to removed vanilla fields/methods</li>
              <li>• Mods that extend heavily modified vanilla classes</li>
              <li>• World generation changes requiring new chunk formats</li>
              <li>• Networking protocol changes (packet format)</li>
            </ul>
          </div>
          <div className="space-y-2">
            <h3 className="text-yellow-400 font-semibold">Partially Supported:</h3>
            <ul className="space-y-1 text-gray-400">
              <li>• Block/Item registration (mostly shim-able)</li>
              <li>• Event handling (requires adapter layer)</li>
              <li>• Rendering changes (significant rewrites needed)</li>
              <li>• Recipe system (format changes)</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  )
}

function ComponentCard({ title, description, items }: { title: string; description: string; items: { label: string; value: string }[] }) {
  return (
    <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
      <h3 className="text-lg font-bold text-white mb-2">{title}</h3>
      <p className="text-sm text-gray-400 mb-4">{description}</p>
      <div className="space-y-2">
        {items.map((item, i) => (
          <div key={i} className="flex flex-col sm:flex-row sm:items-center gap-1 sm:gap-3 text-sm">
            <span className="text-gray-500 font-medium min-w-[140px]">{item.label}:</span>
            <span className="text-gray-300 font-mono text-xs bg-gray-800 px-2 py-1 rounded">{item.value}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

function StepItem({ step, title, description }: { step: number; title: string; description: string }) {
  return (
    <div className="flex gap-4 items-start">
      <div className="flex-shrink-0 w-8 h-8 rounded-full bg-orange-500/20 border border-orange-500/30 flex items-center justify-center text-orange-400 text-sm font-bold">
        {step}
      </div>
      <div>
        <h4 className="text-sm font-semibold text-white">{title}</h4>
        <p className="text-xs text-gray-400 mt-0.5">{description}</p>
      </div>
    </div>
  )
}
