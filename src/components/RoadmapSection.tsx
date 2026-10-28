export default function RoadmapSection() {
  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-bold text-white mb-2">Project Roadmap</h1>
        <p className="text-gray-400">Phased development plan for the ForgeCompat compatibility layer</p>
      </div>

      {/* Timeline */}
      <div className="space-y-6">
        <Milestone
          phase="Phase 1"
          title="Foundation & Material Shim"
          status="current"
          duration="Week 1-2"
          description="Build the project skeleton and implement the most critical shim: the Material system."
          tasks={[
            { text: 'Set up multi-module Gradle project with Forge 1.20.1', done: false },
            { text: 'Implement Material class shim with all material type mappings', done: false },
            { text: 'Implement MaterialColor → MapColor alias', done: false },
            { text: 'Create basic ASM transformer framework', done: false },
            { text: 'Write unit tests for Material translations', done: false },
          ]}
        />

        <Milestone
          phase="Phase 2"
          title="Registry System Shim"
          status="upcoming"
          duration="Week 2-3"
          description="Implement the registry compatibility layer so mods can register blocks/items using the old API."
          tasks={[
            { text: 'Implement RegistryObject<T> wrapper around DeferredHolder', done: false },
            { text: 'Shim DeferredRegister.create() to type-specific methods', done: false },
            { text: 'Handle ForgeRegistries → BuiltInRegistries mapping', done: false },
            { text: 'Test with simple block/item registration', done: false },
            { text: 'Handle creative tab registration changes', done: false },
          ]}
        />

        <Milestone
          phase="Phase 3"
          title="Entity & Block API Shims"
          status="upcoming"
          duration="Week 3-4"
          description="Handle the field-to-method transformations and entity API changes."
          tasks={[
            { text: 'Implement entity.level field → method transformer', done: false },
            { text: 'Implement entity.isOnGround field → method transformer', done: false },
            { text: 'Handle ServerPlayer.getLevel() → serverLevel()', done: false },
            { text: 'Handle BlockBehaviour.Properties constructor changes', done: false },
            { text: 'Implement BlockSetType/WoodType compatibility', done: false },
          ]}
        />

        <Milestone
          phase="Phase 4"
          title="Event & Network Compatibility"
          status="upcoming"
          duration="Week 4-5"
          description="Bridge the event system and network message handling between versions."
          tasks={[
            { text: 'Create event adapter classes (old events extend new events)', done: false },
            { text: 'Implement LivingSetAttackTarget → LivingChangeTarget adapter', done: false },
            { text: 'Handle RenderLevelLastEvent → RenderLevelStageEvent', done: false },
            { text: 'Shim network consumer split (consumer → mainThread/networkThread)', done: false },
            { text: 'Handle SaplingGrowTree → BlockGrowFeature rename', done: false },
          ]}
        />

        <Milestone
          phase="Phase 5"
          title="Homespun Integration Test"
          status="upcoming"
          duration="Week 5-6"
          description="Full integration test with the Homespun mod as the target."
          tasks={[
            { text: 'Compile Homespun 1.19.2 source against ForgeCompat', done: false },
            { text: 'Run in 1.20.1 Forge environment with transformer active', done: false },
            { text: 'Verify all blocks register and render correctly', done: false },
            { text: 'Verify all items register and appear in creative tab', done: false },
            { text: 'Test fluid system (ironberry juice)', done: false },
            { text: 'Test custom recipes (crushing tub, evaporating basin)', done: false },
            { text: 'Test world generation (tree spawning)', done: false },
            { text: 'Fix any remaining compatibility issues', done: false },
          ]}
        />

        <Milestone
          phase="Phase 6"
          title="Going Further Back"
          status="future"
          duration="Week 7+"
          description="Extend the layer to support even older versions (1.18.2, 1.16.5, etc.)"
          tasks={[
            { text: 'Analyze 1.18.2 → 1.19.2 differences', done: false },
            { text: 'Add 1.18.2 shim layer (extends 1.19.2 shim)', done: false },
            { text: 'Handle 1.18.2 specific changes (e.g., tag system)', done: false },
            { text: 'Test with a 1.18.2 mod', done: false },
            { text: 'Design pluggable version adapter architecture', done: false },
            { text: 'Community contribution framework for mappings', done: false },
          ]}
        />
      </div>

      {/* Architecture for Going Back */}
      <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
        <h2 className="text-xl font-bold text-white mb-4">🔮 Future: Multi-Version Architecture</h2>
        <p className="text-sm text-gray-400 mb-4">
          Once the 1.19.2→1.20.1 layer works, we can extend it to support arbitrary version jumps using a chain approach:
        </p>
        <div className="bg-gray-950 rounded-lg p-4 border border-gray-700 overflow-x-auto">
          <pre className="text-xs text-gray-300 font-mono whitespace-pre">{`
Version Chain Architecture:
==========================

1.16.5 ──shim──▶ 1.18.2 ──shim──▶ 1.19.2 ──shim──▶ 1.20.1 ──▶ Runtime
  │                  │                  │                  │
  └── shim-1.16 ────┘── shim-1.18 ────┘── shim-1.19 ────┘
       .jar               .jar               .jar

Each shim layer only handles ONE version jump.
Chaining them allows any old version to run on any new version.

Configuration:
  forgecompat.versions.source = 1.16.5
  forgecompat.versions.target = 1.20.1
  forgecompat.chain.enabled = true  // automatically chain shims
`}</pre>
        </div>
      </div>

      {/* Key Decisions */}
      <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
        <h2 className="text-xl font-bold text-white mb-4">🤔 Key Design Decisions</h2>
        <div className="space-y-4">
          <DecisionCard
            question="ASM/CoreMod vs Mixin vs Source Rewrite?"
            answer="We use ASM/CoreMod because: (1) We don't need source access to the target mod, (2) It works on compiled JARs, (3) Forge has built-in CoreMod support, (4) Mixins would require the target mod to include Mixin as a dependency."
            decision="ASM/CoreMod transformer"
          />
          <DecisionCard
            question="Shim library or pure bytecode transformation?"
            answer="Hybrid approach: Shim library for complex re-implementations (Material system, RegistryObject), bytecode transformation for simple renames (field→method, class renames). This gives us the best of both worlds."
            decision="Hybrid: Shim + Transform"
          />
          <DecisionCard
            question="How to handle mods that use unsupported APIs?"
            answer="Fail gracefully with clear error messages. Log which APIs are unsupported and suggest manual porting. Maintain a compatibility database that tracks which mods work and which don't."
            decision="Graceful degradation + reporting"
          />
          <DecisionCard
            question="Performance impact?"
            answer="Minimal at runtime. The ASM transformation happens once during class loading (startup). The shim classes add negligible overhead - they're thin wrappers that delegate directly to new APIs. No reflection needed."
            decision="Near-zero runtime overhead"
          />
        </div>
      </div>

      {/* Getting Started */}
      <div className="bg-gradient-to-br from-orange-500/10 to-red-500/10 border border-orange-500/20 rounded-xl p-6">
        <h2 className="text-xl font-bold text-white mb-4">🚀 Getting Started</h2>
        <div className="space-y-3 text-sm text-gray-300">
          <p>To start implementing this compatibility layer:</p>
          <ol className="space-y-2 list-decimal list-inside text-gray-400">
            <li>Clone the ForgeCompat skeleton project (TBD)</li>
            <li>Set up Forge 1.20.1 dev environment with ForgeGradle 6.x</li>
            <li>Begin with the Material system shim (Phase 1)</li>
            <li>Use Homespun's 1.19.2 source as your integration test target</li>
            <li>Iterate: run → find errors → add shims → repeat</li>
          </ol>
          <div className="mt-4 p-3 bg-gray-900/50 rounded-lg border border-gray-700">
            <p className="text-xs text-gray-400">
              <span className="text-orange-400 font-medium">Tip:</span> Start with a minimal test mod that just registers a single block using the 1.19.2 API. 
              Get that working first before attempting the full Homespun mod. This isolates issues and lets you verify each shim independently.
            </p>
          </div>
        </div>
      </div>

      {/* Resources */}
      <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
        <h2 className="text-xl font-bold text-white mb-4">📚 Reference Resources</h2>
        <div className="grid sm:grid-cols-2 gap-4 text-sm">
          <ResourceLink
            title="1.19.4 → 1.20 Migration Primer"
            url="https://gist.github.com/ChampionAsh5357/cf818acc53ffea6f4387fe28c2977d56"
            description="Comprehensive list of all API changes"
          />
          <ResourceLink
            title="Forge Documentation"
            url="https://docs.minecraftforge.net/"
            description="Official Forge modding docs"
          />
          <ResourceLink
            title="Homespun Source (GitHub)"
            url="https://github.com/PrincessStrelka/Homespun"
            description="Our test mod's source code"
          />
          <ResourceLink
            title="Forge CoreMods Guide"
            url="https://docs.minecraftforge.net/en/latest/advanced/coremods/"
            description="How to write ASM transformers for Forge"
          />
          <ResourceLink
            title="NeoForge Porting Guide"
            url="https://mcjty.eu/docs/1.20.4_neo/porting"
            description="Porting guide (relevant patterns)"
          />
          <ResourceLink
            title="Minecraft Version Changelog"
            url="https://misode.github.io/versions/?id=1.20&tab=changelog"
            description="Detailed vanilla changes between versions"
          />
        </div>
      </div>
    </div>
  )
}

function Milestone({ phase, title, status, duration, description, tasks }: {
  phase: string; title: string; status: 'current' | 'upcoming' | 'future'; duration: string; description: string; tasks: { text: string; done: boolean }[]
}) {
  const statusColors = {
    current: 'border-orange-500/30 bg-orange-500/5',
    upcoming: 'border-gray-700 bg-gray-900',
    future: 'border-gray-800 bg-gray-900/50',
  }
  const statusBadge = {
    current: 'bg-orange-500/20 text-orange-400 border-orange-500/30',
    upcoming: 'bg-gray-700/50 text-gray-400 border-gray-600',
    future: 'bg-gray-800/50 text-gray-500 border-gray-700',
  }
  return (
    <div className={`rounded-xl border p-6 ${statusColors[status]}`}>
      <div className="flex flex-wrap items-center gap-3 mb-3">
        <span className="text-sm font-bold text-orange-400">{phase}</span>
        <h3 className="text-lg font-bold text-white">{title}</h3>
        <span className={`px-2 py-0.5 rounded text-xs font-medium border ${statusBadge[status]}`}>
          {status === 'current' ? '🔨 In Progress' : status === 'upcoming' ? '📋 Upcoming' : '🔮 Future'}
        </span>
        <span className="text-xs text-gray-500 ml-auto">{duration}</span>
      </div>
      <p className="text-sm text-gray-400 mb-4">{description}</p>
      <div className="space-y-2">
        {tasks.map((task, i) => (
          <div key={i} className="flex items-center gap-2 text-sm">
            <div className={`w-4 h-4 rounded border flex items-center justify-center ${task.done ? 'bg-green-500/20 border-green-500/30' : 'border-gray-600'}`}>
              {task.done && <span className="text-green-400 text-xs">✓</span>}
            </div>
            <span className={task.done ? 'text-gray-500 line-through' : 'text-gray-300'}>{task.text}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

function DecisionCard({ question, answer, decision }: { question: string; answer: string; decision: string }) {
  return (
    <div className="border border-gray-700/50 rounded-lg p-4">
      <h3 className="text-sm font-semibold text-white mb-1">{question}</h3>
      <p className="text-xs text-gray-400 mb-2">{answer}</p>
      <div className="inline-flex items-center gap-1.5 px-2 py-1 bg-orange-500/10 border border-orange-500/20 rounded text-xs text-orange-400">
        <span>→</span>
        <span className="font-medium">{decision}</span>
      </div>
    </div>
  )
}

function ResourceLink({ title, url, description }: { title: string; url: string; description: string }) {
  return (
    <a href={url} target="_blank" rel="noreferrer" className="block p-3 bg-gray-800/50 border border-gray-700 rounded-lg hover:border-orange-500/30 transition-all group">
      <h4 className="text-sm font-semibold text-gray-200 group-hover:text-orange-400 transition-colors">{title}</h4>
      <p className="text-xs text-gray-500 mt-1">{description}</p>
    </a>
  )
}
