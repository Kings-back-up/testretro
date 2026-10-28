import { useState } from 'react'

interface Mapping {
  category: string
  oldAPI: string
  newAPI: string
  complexity: 'simple' | 'moderate' | 'complex'
  notes?: string
}

const mappings: Mapping[] = [
  // Material System
  { category: 'Materials', oldAPI: 'Material.WOOD', newAPI: 'BlockBehaviour.Properties.of().ignitedByLava()', complexity: 'complex', notes: 'Material class removed entirely' },
  { category: 'Materials', oldAPI: 'Material.STONE', newAPI: 'BlockBehaviour.Properties.of()', complexity: 'moderate', notes: 'Default behavior matches' },
  { category: 'Materials', oldAPI: 'material.isFlammable()', newAPI: 'properties.ignitedByLava()', complexity: 'simple' },
  { category: 'Materials', oldAPI: 'material.isLiquid()', newAPI: 'properties.liquid()', complexity: 'simple' },
  { category: 'Materials', oldAPI: 'MaterialColor.*', newAPI: 'MapColor.*', complexity: 'simple', notes: 'Direct rename' },
  { category: 'Materials', oldAPI: 'material.getPushReaction()', newAPI: 'properties.pushReaction(PushReaction.*)', complexity: 'moderate' },

  // Registry Changes
  { category: 'Registry', oldAPI: 'RegistryObject<T>', newAPI: 'DeferredHolder<R,T> / DeferredBlock<T> / DeferredItem<T>', complexity: 'moderate', notes: 'Type-specific deferred holders' },
  { category: 'Registry', oldAPI: 'DeferredRegister.create()', newAPI: 'DeferredRegister.createBlocks() / .createItems()', complexity: 'moderate', notes: 'Type-safe factory methods' },
  { category: 'Registry', oldAPI: 'BLOCKS.register("name", () -> new Block(...))', newAPI: 'BLOCKS.register("name", () -> new Block(...))', complexity: 'simple', notes: 'Same API, different return type' },
  { category: 'Registry', oldAPI: 'ForgeRegistries.BLOCKS.getKey(block)', newAPI: 'BuiltInRegistries.BLOCK.getKey(block)', complexity: 'simple', notes: 'Moved to vanilla registries' },

  // GUI Changes
  { category: 'GUI', oldAPI: 'GuiComponent.blit(poseStack, ...)', newAPI: 'GuiGraphics.blit(resourceLocation, ...)', complexity: 'moderate', notes: 'GuiComponent removed' },
  { category: 'GUI', oldAPI: 'Screen#renderBackground(PoseStack)', newAPI: 'Screen#renderBackground(GuiGraphics)', complexity: 'moderate' },
  { category: 'GUI', oldAPI: 'ItemRenderer#renderGuiItem(...)', newAPI: 'GuiGraphics#renderItem(...)', complexity: 'moderate' },
  { category: 'GUI', oldAPI: 'Font#drawShadow(poseStack, ...)', newAPI: 'GuiGraphics#drawString(font, ...)', complexity: 'moderate' },

  // Creative Tabs
  { category: 'Creative Tabs', oldAPI: 'CreativeModeTab.Builder()', newAPI: 'CreativeModeTab.builder() (static)', complexity: 'simple' },
  { category: 'Creative Tabs', oldAPI: 'CreativeModeTabEvent.BuildContents', newAPI: 'BuildCreativeModeTabContentsEvent', complexity: 'moderate', notes: 'New event-based system' },

  // Events
  { category: 'Events', oldAPI: 'LivingSetAttackTargetEvent', newAPI: 'LivingChangeTargetEvent', complexity: 'simple', notes: 'Direct rename' },
  { category: 'Events', oldAPI: 'RenderLevelLastEvent', newAPI: 'RenderLevelStageEvent', complexity: 'complex', notes: 'No direct translation, must choose stage' },
  { category: 'Events', oldAPI: 'SaplingGrowTreeEvent', newAPI: 'BlockGrowFeatureEvent', complexity: 'simple', notes: 'Direct rename' },
  { category: 'Events', oldAPI: 'PlayerEvent.BreakSpeed#getPos()', newAPI: '#getPosition() returns Optional<BlockPos>', complexity: 'moderate' },

  // Network
  { category: 'Network', oldAPI: 'SimpleChannel#consumer(...)', newAPI: '#consumerNetworkThread() or #consumerMainThread()', complexity: 'moderate', notes: 'Split into two methods' },
  { category: 'Network', oldAPI: 'NetworkRegistry.newSimpleChannel()', newAPI: 'RegisterEvent-based registration', complexity: 'complex' },

  // Entity
  { category: 'Entity', oldAPI: 'entity.level (field)', newAPI: 'entity.level() (method)', complexity: 'moderate', notes: 'Field → method accessor' },
  { category: 'Entity', oldAPI: 'entity.isOnGround (field)', newAPI: 'entity.onGround() (method)', complexity: 'moderate', notes: 'Field → method accessor' },
  { category: 'Entity', oldAPI: 'ServerPlayer#getLevel()', newAPI: '#serverLevel()', complexity: 'simple' },
  { category: 'Entity', oldAPI: 'Entity#outOfWorld()', newAPI: '#onBelowWorld()', complexity: 'simple' },

  // Block Properties
  { category: 'Block Properties', oldAPI: 'Properties.of(Material)', newAPI: 'Properties.of()', complexity: 'moderate', notes: 'Material param removed' },
  { category: 'Block Properties', oldAPI: 'Properties#color(MapColor)', newAPI: 'Properties#mapColor(MapColor)', complexity: 'simple' },
  { category: 'Block Properties', oldAPI: 'BlockBehaviour.Properties.ofFullCopy(block)', newAPI: 'BlockBehaviour.Properties.ofFullCopy(block)', complexity: 'simple', notes: 'Same in 1.20.1' },

  // Capabilities
  { category: 'Capabilities', oldAPI: 'CapabilityItemHandler.*', newAPI: 'ForgeCapabilities.ITEM_HANDLER', complexity: 'simple', notes: 'Class removed, use ForgeCapabilities' },
  { category: 'Capabilities', oldAPI: 'CapabilityFluidHandler.*', newAPI: 'ForgeCapabilities.FLUID_HANDLER', complexity: 'simple' },
  { category: 'Capabilities', oldAPI: 'CapabilityEnergy.*', newAPI: 'ForgeCapabilities.ENERGY', complexity: 'simple' },

  // Player Reach
  { category: 'Player', oldAPI: 'IForgePlayer#getAttackRange()', newAPI: '#getEntityReach()', complexity: 'simple' },
  { category: 'Player', oldAPI: 'IForgePlayer#getReachDistance()', newAPI: '#getBlockReach()', complexity: 'simple' },
  { category: 'Player', oldAPI: 'ForgeMod#REACH_DISTANCE', newAPI: 'ForgeMod#BLOCK_REACH', complexity: 'simple' },

  // Damage
  { category: 'Damage', oldAPI: 'DamageSources#outOfWorld()', newAPI: '#fellOutOfWorld()', complexity: 'simple' },
  { category: 'Damage', oldAPI: 'CombatTracker#recordDamage(src, float, float)', newAPI: '#recordDamage(DamageSource, float)', complexity: 'moderate' },

  // Loot
  { category: 'Loot', oldAPI: 'LootContext$Builder', newAPI: 'LootParams$Builder', complexity: 'moderate', notes: 'Split into LootParams + LootContext' },
  { category: 'Loot', oldAPI: 'server.getLootTables()', newAPI: 'server.getLootData().getElement(LootDataType.TABLE, rl)', complexity: 'complex' },

  // Light Engine
  { category: 'Lighting', oldAPI: 'LayerLightEngine', newAPI: 'LightEngine', complexity: 'simple', notes: 'Direct rename' },
  { category: 'Lighting', oldAPI: 'LightLayer (with values)', newAPI: 'LightLayer (enum constants only)', complexity: 'moderate' },
]

export default function APIMappingSection() {
  const [filter, setFilter] = useState('all')
  const categories = ['all', ...new Set(mappings.map(m => m.category))]
  
  const filtered = filter === 'all' ? mappings : mappings.filter(m => m.category === filter)

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-bold text-white mb-2">API Mapping Reference</h1>
        <p className="text-gray-400">Complete mapping of 1.19.2 Forge APIs to their 1.20.1 equivalents</p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <StatCard label="Total Mappings" value={mappings.length.toString()} color="orange" />
        <StatCard label="Simple" value={mappings.filter(m => m.complexity === 'simple').length.toString()} color="green" />
        <StatCard label="Moderate" value={mappings.filter(m => m.complexity === 'moderate').length.toString()} color="yellow" />
        <StatCard label="Complex" value={mappings.filter(m => m.complexity === 'complex').length.toString()} color="red" />
      </div>

      {/* Filter */}
      <div className="flex flex-wrap gap-2">
        {categories.map(cat => (
          <button
            key={cat}
            onClick={() => setFilter(cat)}
            className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-all ${
              filter === cat
                ? 'bg-orange-500/20 text-orange-400 border border-orange-500/30'
                : 'bg-gray-800 text-gray-400 border border-gray-700 hover:text-gray-200'
            }`}
          >
            {cat === 'all' ? 'All' : cat}
          </button>
        ))}
      </div>

      {/* Mapping Table */}
      <div className="bg-gray-900 border border-gray-800 rounded-xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-800 bg-gray-800/50">
                <th className="text-left px-4 py-3 text-gray-400 font-medium">Category</th>
                <th className="text-left px-4 py-3 text-gray-400 font-medium">1.19.2 API</th>
                <th className="text-left px-4 py-3 text-gray-400 font-medium">1.20.1 API</th>
                <th className="text-left px-4 py-3 text-gray-400 font-medium">Complexity</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((mapping, i) => (
                <tr key={i} className="border-b border-gray-800/50 hover:bg-gray-800/30">
                  <td className="px-4 py-3">
                    <span className="text-gray-400 text-xs">{mapping.category}</span>
                  </td>
                  <td className="px-4 py-3">
                    <code className="text-red-300 text-xs bg-red-500/10 px-1.5 py-0.5 rounded">{mapping.oldAPI}</code>
                  </td>
                  <td className="px-4 py-3">
                    <code className="text-green-300 text-xs bg-green-500/10 px-1.5 py-0.5 rounded">{mapping.newAPI}</code>
                  </td>
                  <td className="px-4 py-3">
                    <ComplexityBadge complexity={mapping.complexity} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Notes */}
      {filtered.some(m => m.notes) && (
        <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
          <h3 className="text-lg font-bold text-white mb-3">📝 Implementation Notes</h3>
          <div className="space-y-2">
            {filtered.filter(m => m.notes).map((m, i) => (
              <div key={i} className="flex gap-2 text-sm">
                <code className="text-orange-400 text-xs whitespace-nowrap">{m.oldAPI}</code>
                <span className="text-gray-500">→</span>
                <span className="text-gray-400">{m.notes}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

function StatCard({ label, value, color }: { label: string; value: string; color: string }) {
  const colors: Record<string, string> = {
    orange: 'border-orange-500/20 text-orange-400',
    green: 'border-green-500/20 text-green-400',
    yellow: 'border-yellow-500/20 text-yellow-400',
    red: 'border-red-500/20 text-red-400',
  }
  return (
    <div className={`bg-gray-900 border rounded-xl p-4 ${colors[color]}`}>
      <div className="text-2xl font-bold">{value}</div>
      <div className="text-xs text-gray-400 mt-1">{label}</div>
    </div>
  )
}

function ComplexityBadge({ complexity }: { complexity: string }) {
  const colors: Record<string, string> = {
    simple: 'bg-green-500/20 text-green-400',
    moderate: 'bg-yellow-500/20 text-yellow-400',
    complex: 'bg-red-500/20 text-red-400',
  }
  return (
    <span className={`px-2 py-0.5 rounded text-xs font-medium ${colors[complexity]}`}>
      {complexity}
    </span>
  )
}
