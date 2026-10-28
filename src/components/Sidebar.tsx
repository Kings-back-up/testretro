interface SidebarProps {
  activeSection: string
  setActiveSection: (section: string) => void
  isOpen: boolean
  onClose: () => void
}

const sections = [
  { id: 'overview', label: 'Overview', icon: '🏠' },
  { id: 'architecture', label: 'Architecture', icon: '🏗️' },
  { id: 'api-mapping', label: 'API Mapping', icon: '🔄' },
  { id: 'homespun', label: 'Homespun Analysis', icon: '🔍' },
  { id: 'implementation', label: 'Implementation Guide', icon: '📋' },
  { id: 'code-examples', label: 'Code Examples', icon: '💻' },
  { id: 'roadmap', label: 'Roadmap', icon: '🗺️' },
]

export default function Sidebar({ activeSection, setActiveSection, isOpen, onClose }: SidebarProps) {
  return (
    <>
      {/* Overlay */}
      {isOpen && (
        <div className="lg:hidden fixed inset-0 bg-black/50 z-30" onClick={onClose} />
      )}
      
      {/* Sidebar */}
      <aside className={`fixed top-0 left-0 h-full w-72 bg-gray-900 border-r border-gray-800 z-40 transform transition-transform duration-200 ${isOpen ? 'translate-x-0' : '-translate-x-full'} lg:translate-x-0`}>
        <div className="p-6">
          <div className="flex items-center gap-3 mb-8">
            <div className="w-10 h-10 bg-gradient-to-br from-orange-500 to-red-600 rounded-lg flex items-center justify-center text-xl">
              ⚒️
            </div>
            <div>
              <h1 className="text-lg font-bold text-white">ForgeCompat</h1>
              <p className="text-xs text-gray-400">Compatibility Layer</p>
            </div>
          </div>

          <nav className="space-y-1">
            {sections.map((section) => (
              <button
                key={section.id}
                onClick={() => { setActiveSection(section.id); onClose() }}
                className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-lg text-left transition-all ${
                  activeSection === section.id
                    ? 'bg-orange-500/10 text-orange-400 border border-orange-500/20'
                    : 'text-gray-400 hover:text-gray-200 hover:bg-gray-800'
                }`}
              >
                <span className="text-lg">{section.icon}</span>
                <span className="text-sm font-medium">{section.label}</span>
              </button>
            ))}
          </nav>

          <div className="mt-8 p-4 bg-gray-800/50 rounded-lg border border-gray-700">
            <h3 className="text-sm font-semibold text-gray-300 mb-2">Target Versions</h3>
            <div className="space-y-2 text-xs">
              <div className="flex justify-between">
                <span className="text-gray-400">Source:</span>
                <span className="text-green-400 font-mono">Forge 1.19.2</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-400">Target:</span>
                <span className="text-blue-400 font-mono">Forge 1.20.1</span>
              </div>
            </div>
          </div>

          <div className="mt-4 p-4 bg-gray-800/50 rounded-lg border border-gray-700">
            <h3 className="text-sm font-semibold text-gray-300 mb-2">Test Mod</h3>
            <div className="text-xs text-gray-400">
              <p className="text-orange-400 font-medium">Homespun</p>
              <p className="mt-1">by PrincessStrelka</p>
              <p className="mt-1 text-gray-500">Blocks, items, fluids, recipes, world gen</p>
            </div>
          </div>
        </div>
      </aside>
    </>
  )
}
