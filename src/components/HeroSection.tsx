export default function HeroSection() {
  return (
    <div className="space-y-8">
      {/* Hero */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 border border-gray-700 p-8 sm:p-12">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top_right,_var(--tw-gradient-stops))] from-orange-500/10 via-transparent to-transparent" />
        <div className="relative">
          <div className="inline-flex items-center gap-2 px-3 py-1 bg-orange-500/10 border border-orange-500/20 rounded-full text-orange-400 text-sm mb-4">
            <span className="w-2 h-2 bg-orange-400 rounded-full animate-pulse" />
            Technical Design Document
          </div>
          <h1 className="text-3xl sm:text-4xl font-bold text-white mb-4">
            Forge Mod Compatibility Layer
          </h1>
          <p className="text-lg text-gray-300 mb-6 max-w-2xl">
            A reusable framework for bridging Minecraft Forge API differences between versions,
            enabling 1.19.2 mods to run on 1.20.1 with minimal source modifications.
          </p>
          <div className="flex flex-wrap gap-3">
            <span className="px-3 py-1.5 bg-green-500/10 border border-green-500/20 rounded-lg text-green-400 text-sm">
              Forge 40.x (1.19.2)
            </span>
            <span className="px-3 py-1.5 bg-blue-500/10 border border-blue-500/20 rounded-lg text-blue-400 text-sm">
              Forge 47.x (1.20.1)
            </span>
            <span className="px-3 py-1.5 bg-purple-500/10 border border-purple-500/20 rounded-lg text-purple-400 text-sm">
              Test: Homespun Mod
            </span>
          </div>
        </div>
      </div>

      {/* Key Challenges */}
      <div className="grid sm:grid-cols-2 gap-4">
        <ChallengeCard
          icon="🧱"
          title="Material System Removal"
          description="Materials were completely removed in 1.20. Block properties now use individual flags like ignitedByLava(), pushReaction(), etc."
          severity="high"
        />
        <ChallengeCard
          icon="📦"
          title="Registry API Changes"
          description="DeferredRegister, DeferredBlock, DeferredItem APIs changed significantly. RegistryObject → DeferredHolder pattern."
          severity="high"
        />
        <ChallengeCard
          icon="🎨"
          title="GUI Rendering Overhaul"
          description="GuiComponent fully removed, replaced by GuiGraphics. PoseStack parameters replaced throughout GUI classes."
          severity="medium"
        />
        <ChallengeCard
          icon="🔌"
          title="Network & Events"
          description="Message consumer split into networkThread/mainThread. Event class renames and signature changes throughout."
          severity="medium"
        />
      </div>

      {/* Approach Overview */}
      <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
        <h2 className="text-xl font-bold text-white mb-4">🎯 Core Approach</h2>
        <div className="space-y-4 text-gray-300">
          <p>
            The compatibility layer works as a <strong className="text-orange-400">compile-time shim library</strong> that 
            provides the old 1.19.2 API surface, translating calls to their 1.20.1 equivalents at runtime.
          </p>
          <div className="bg-gray-800 rounded-lg p-4 border border-gray-700">
            <h3 className="text-sm font-semibold text-gray-400 mb-3">Strategy: Three-Layer Architecture</h3>
            <div className="space-y-3">
              <LayerItem
                number={1}
                title="API Shim Layer"
                description="Re-implements removed/changed classes and methods from 1.19.2 Forge, delegating to 1.20.1 equivalents"
              />
              <LayerItem
                number={2}
                title="Bytecode Transform Layer"
                description="Uses ASM/bytecode manipulation to rewrite class references and method signatures in the target mod's compiled classes"
              />
              <LayerItem
                number={3}
                title="Runtime Adapter Layer"
                description="Handles runtime differences in behavior, event timing, and data format changes between versions"
              />
            </div>
          </div>
        </div>
      </div>

      {/* Why This Approach */}
      <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
        <h2 className="text-xl font-bold text-white mb-4">💡 Why Not Just Port the Mod?</h2>
        <div className="grid sm:grid-cols-2 gap-4 text-sm">
          <div className="space-y-2">
            <h3 className="text-red-400 font-semibold">Problems with manual porting:</h3>
            <ul className="space-y-1 text-gray-400">
              <li>• Abandoned mods can't be updated by original authors</li>
              <li>• Many mods have complex interdependencies</li>
              <li>• Time-consuming for large codebases</li>
              <li>• Breaks save compatibility with original mod</li>
              <li>• Each MC version requires another port</li>
            </ul>
          </div>
          <div className="space-y-2">
            <h3 className="text-green-400 font-semibold">Benefits of compat layer:</h3>
            <ul className="space-y-1 text-gray-400">
              <li>• Works for any mod using supported APIs</li>
              <li>• No source code access required</li>
              <li>• Reusable across many mods</li>
              <li>• Incrementally expandable</li>
              <li>• Community can contribute mappings</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  )
}

function ChallengeCard({ icon, title, description, severity }: { icon: string; title: string; description: string; severity: 'high' | 'medium' | 'low' }) {
  const colors = {
    high: 'border-red-500/20 bg-red-500/5',
    medium: 'border-yellow-500/20 bg-yellow-500/5',
    low: 'border-green-500/20 bg-green-500/5',
  }
  const badges = {
    high: 'bg-red-500/20 text-red-400',
    medium: 'bg-yellow-500/20 text-yellow-400',
    low: 'bg-green-500/20 text-green-400',
  }
  return (
    <div className={`rounded-xl border p-5 ${colors[severity]}`}>
      <div className="flex items-start justify-between mb-2">
        <span className="text-2xl">{icon}</span>
        <span className={`px-2 py-0.5 rounded text-xs font-medium ${badges[severity]}`}>
          {severity}
        </span>
      </div>
      <h3 className="font-semibold text-white mb-1">{title}</h3>
      <p className="text-sm text-gray-400">{description}</p>
    </div>
  )
}

function LayerItem({ number, title, description }: { number: number; title: string; description: string }) {
  return (
    <div className="flex gap-3">
      <div className="flex-shrink-0 w-7 h-7 rounded-full bg-orange-500/20 border border-orange-500/30 flex items-center justify-center text-orange-400 text-xs font-bold">
        {number}
      </div>
      <div>
        <h4 className="text-sm font-semibold text-white">{title}</h4>
        <p className="text-xs text-gray-400 mt-0.5">{description}</p>
      </div>
    </div>
  )
}
