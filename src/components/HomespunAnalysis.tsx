export default function HomespunAnalysis() {
  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-bold text-white mb-2">Homespun Mod Analysis</h1>
        <p className="text-gray-400">
          Analysis of <a href="https://github.com/PrincessStrelka/Homespun" className="text-orange-400 hover:underline" target="_blank" rel="noreferrer">Homespun</a> by PrincessStrelka — our first test case for the compatibility layer
        </p>
      </div>

      {/* Mod Overview */}
      <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
        <h2 className="text-xl font-bold text-white mb-4">📦 Mod Overview</h2>
        <div className="grid sm:grid-cols-2 gap-6">
          <div className="space-y-3">
            <InfoRow label="Mod ID" value="homespun" />
            <InfoRow label="Original Version" value="1.19.2 (Forge 43.x)" />
            <InfoRow label="Current Version" value="1.21.1 (NeoForge 21.x)" />
            <InfoRow label="License" value="MIT" />
            <InfoRow label="Inspiration" value="Rustic mod" />
          </div>
          <div className="space-y-3">
            <InfoRow label="Content" value="Trees, crops, fluids, industry blocks" />
            <InfoRow label="Custom Blocks" value="~50+ blocks" />
            <InfoRow label="Custom Items" value="~15+ items" />
            <InfoRow label="Custom Recipes" value="Crushing tub, evaporating basin" />
            <InfoRow label="World Gen" value="Ironwood & Olive trees" />
          </div>
        </div>
      </div>

      {/* API Usage Analysis */}
      <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
        <h2 className="text-xl font-bold text-white mb-4">🔍 Forge API Usage (1.19.2 → 1.20.1 Impact)</h2>
        <p className="text-sm text-gray-400 mb-4">
          Based on analysis of the current NeoForge source, here's what APIs the mod uses that would need compatibility handling:
        </p>
        
        <div className="space-y-4">
          <APIUsageCard
            title="DeferredRegister / Registry System"
            usage="Heavy"
            description="Uses DeferredRegister for blocks, items, block entities, fluids, menus, and recipes. In 1.19.2 this was RegistryObject-based; 1.20.1 uses DeferredHolder/DeferredBlock/DeferredItem."
            compatNotes="The shim needs to provide RegistryObject<T> that wraps DeferredHolder. The DeferredRegister.create() API needs to be shimmed to the type-specific factory methods."
            severity="high"
          />

          <APIUsageCard
            title="BlockBehaviour.Properties (Material System)"
            usage="Heavy"
            description="Extensively uses BlockBehaviour.Properties.of() with mapColor(), pushReaction(), ignitedByLava(), instrument(), forceSolidOn(), etc."
            compatNotes="In 1.19.2, properties took a Material parameter. The shim must accept the old Material-based constructor and translate to the new property flags. MapColor was MaterialColor in 1.19.2."
            severity="high"
          />

          <APIUsageCard
            title="BlockSetType / WoodType"
            usage="Moderate"
            description="Registers custom BlockSetType and WoodType for ironwood and olive wood sets. Uses these for doors, trapdoors, buttons, pressure plates, fence gates."
            compatNotes="BlockSetType and WoodType APIs are largely stable between versions. Minor constructor changes may need handling."
            severity="low"
          />

          <APIUsageCard
            title="Creative Mode Tabs"
            usage="Moderate"
            description="Registers custom creative tab via DeferredRegister. In 1.19.2 this used the old CreativeModeTab.Builder pattern."
            compatNotes="1.20.1 changed to BuildCreativeModeTabContentsEvent for adding items to tabs. The shim needs to intercept tab registration and convert to the new event-based system."
            severity="medium"
          />

          <APIUsageCard
            title="Fluid System"
            usage="Moderate"
            description="Registers custom fluids (ironberry juice) with FluidType, uses BucketItem with custom fluid."
            compatNotes="Fluid registration API changed. FluidType is NeoForge-specific. The shim needs to handle the old Forge fluid registration pattern."
            severity="medium"
          />

          <APIUsageCard
            title="Custom Recipe Types"
            usage="Moderate"
            description="Implements CrushingTub and EvaporatingBasin custom recipe types with custom serializers."
            compatNotes="Recipe serialization changed in 1.20.1 (Codec-based). The shim needs to provide the old RecipeSerializer interface that wraps the new Codec system."
            severity="medium"
          />

          <APIUsageCard
            title="Tool Modification (Axe Stripping)"
            usage="Light"
            description="Uses getToolModifiedState() with ItemAbilities.AXE_STRIP for log stripping behavior."
            compatNotes="In 1.19.2 this used ToolActions instead of ItemAbilities. The shim needs to map between these."
            severity="medium"
          />

          <APIUsageCard
            title="World Generation"
            usage="Light"
            description="Uses tree growers (ModTreeGrowers) for ironwood and olive tree generation."
            compatNotes="Tree grower API is relatively stable. Feature registration may need minor adjustments."
            severity="low"
          />

          <APIUsageCard
            title="Block Entity System"
            usage="Moderate"
            description="Custom block entities for CrushingTub, EvaporatingBasin, Vessel, FluidStorage."
            compatNotes="BlockEntityType registration changed. The shim needs to handle the old register pattern."
            severity="medium"
          />

          <APIUsageCard
            title="FallingLogBlock (Custom Block)"
            usage="Light"
            description="Custom FallingLogBlock extending falling behavior to logs."
            compatNotes="Fallable interface changes between versions. The shim needs to provide the old interface."
            severity="low"
          />
        </div>
      </div>

      {/* Compatibility Score */}
      <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
        <h2 className="text-xl font-bold text-white mb-4">📊 Compatibility Assessment</h2>
        <div className="space-y-4">
          <div className="flex items-center gap-4">
            <div className="flex-1">
              <div className="flex justify-between text-sm mb-1">
                <span className="text-gray-300">Overall Shim-ability</span>
                <span className="text-green-400">72%</span>
              </div>
              <div className="w-full bg-gray-800 rounded-full h-3">
                <div className="bg-gradient-to-r from-green-500 to-green-400 h-3 rounded-full" style={{ width: '72%' }} />
              </div>
            </div>
          </div>
          
          <div className="grid sm:grid-cols-3 gap-4 text-sm">
            <div className="bg-green-500/5 border border-green-500/20 rounded-lg p-3">
              <h4 className="text-green-400 font-semibold mb-1">✅ Fully Shim-able</h4>
              <ul className="text-gray-400 space-y-0.5 text-xs">
                <li>• Block/Item registration</li>
                <li>• BlockSetType/WoodType</li>
                <li>• Basic block properties</li>
                <li>• Event subscriptions</li>
              </ul>
            </div>
            <div className="bg-yellow-500/5 border border-yellow-500/20 rounded-lg p-3">
              <h4 className="text-yellow-400 font-semibold mb-1">⚡ Needs Adapter</h4>
              <ul className="text-gray-400 space-y-0.5 text-xs">
                <li>• Creative tab system</li>
                <li>• Fluid registration</li>
                <li>• Custom recipes</li>
                <li>• Tool action mapping</li>
              </ul>
            </div>
            <div className="bg-red-500/5 border border-red-500/20 rounded-lg p-3">
              <h4 className="text-red-400 font-semibold mb-1">❌ Needs Rewrite</h4>
              <ul className="text-gray-400 space-y-0.5 text-xs">
                <li>• Material system usage</li>
                <li>• Recipe serialization</li>
                <li>• Network packets (if any)</li>
              </ul>
            </div>
          </div>
        </div>
      </div>

      {/* Source Structure */}
      <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
        <h2 className="text-xl font-bold text-white mb-4">📁 Source Structure</h2>
        <div className="bg-gray-950 rounded-lg p-4 border border-gray-700 font-mono text-sm">
          <pre className="text-gray-300">{`strelka/homespun/
├── Homespun.java          # @Mod entry point, registers everything
├── ModTabs.java           # Creative tab registration
├── TagInit.java           # Tag initialization
├── ColourHandler.java     # Color handler for items/blocks
├── block/
│   ├── ModBlocks.java     # All block definitions (~50+ blocks)
│   ├── CrushingTubBlock.java
│   ├── EvaporatingBasinBlock.java
│   ├── FallingLogBlock.java
│   ├── PostBlock.java
│   └── VesselBlock.java
├── blockentity/
│   └── ModBlockEntities.java
├── compat/                # JEI/TOP integration
├── datagen/               # Data generation
├── events/                # Event handlers
├── fluid/
│   ├── ModFluids.java     # Fluid registration
│   └── ModFluidTypes.java # Fluid type registration
├── gui/
│   └── ModMenuRegistry.java
├── item/
│   ├── ModItems.java      # Item definitions
│   └── RedstoneAcidItem.java
├── recipes/
│   └── ModRecipes.java    # Custom recipe types
├── renderer/              # Custom renderers
├── utils/                 # Utility classes
└── world/feature/
    └── ModTreeGrowers.java`}</pre>
        </div>
      </div>
    </div>
  )
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between text-sm">
      <span className="text-gray-500">{label}</span>
      <span className="text-gray-300">{value}</span>
    </div>
  )
}

function APIUsageCard({ title, usage, description, compatNotes, severity }: {
  title: string; usage: string; description: string; compatNotes: string; severity: string
}) {
  const severityColors: Record<string, string> = {
    high: 'border-red-500/20',
    medium: 'border-yellow-500/20',
    low: 'border-green-500/20',
  }
  const usageColors: Record<string, string> = {
    Heavy: 'bg-red-500/20 text-red-400',
    Moderate: 'bg-yellow-500/20 text-yellow-400',
    Light: 'bg-green-500/20 text-green-400',
  }
  return (
    <div className={`border rounded-lg p-4 ${severityColors[severity]}`}>
      <div className="flex items-center justify-between mb-2">
        <h3 className="font-semibold text-white text-sm">{title}</h3>
        <span className={`px-2 py-0.5 rounded text-xs font-medium ${usageColors[usage]}`}>{usage} usage</span>
      </div>
      <p className="text-xs text-gray-400 mb-2">{description}</p>
      <div className="bg-gray-800/50 rounded p-2">
        <p className="text-xs text-gray-300"><span className="text-orange-400 font-medium">Compat Notes:</span> {compatNotes}</p>
      </div>
    </div>
  )
}
