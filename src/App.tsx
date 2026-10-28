import { useState } from 'react'
import Sidebar from './components/Sidebar'
import HeroSection from './components/HeroSection'
import ArchitectureSection from './components/ArchitectureSection'
import APIMappingSection from './components/APIMappingSection'
import HomespunAnalysis from './components/HomespunAnalysis'
import ImplementationGuide from './components/ImplementationGuide'
import CodeExamples from './components/CodeExamples'
import RoadmapSection from './components/RoadmapSection'

function App() {
  const [activeSection, setActiveSection] = useState('overview')
  const [sidebarOpen, setSidebarOpen] = useState(false)

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100">
      {/* Mobile menu button */}
      <button
        onClick={() => setSidebarOpen(!sidebarOpen)}
        className="lg:hidden fixed top-4 left-4 z-50 p-2 bg-gray-800 rounded-lg border border-gray-700"
      >
        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
        </svg>
      </button>

      <Sidebar activeSection={activeSection} setActiveSection={setActiveSection} isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      <main className="lg:ml-72">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          {activeSection === 'overview' && <HeroSection />}
          {activeSection === 'architecture' && <ArchitectureSection />}
          {activeSection === 'api-mapping' && <APIMappingSection />}
          {activeSection === 'homespun' && <HomespunAnalysis />}
          {activeSection === 'implementation' && <ImplementationGuide />}
          {activeSection === 'code-examples' && <CodeExamples />}
          {activeSection === 'roadmap' && <RoadmapSection />}
        </div>
      </main>
    </div>
  )
}

export default App
