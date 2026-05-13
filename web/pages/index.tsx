import React, { useState, useEffect } from 'react';
import Sidebar from '../components/Sidebar';
import BlockEditor from '../components/BlockEditor';
import KanbanBoard from '../components/KanbanBoard';
import FinanceTracker from '../components/FinanceTracker';
import AISuggestionPanel from '../components/AISuggestionPanel';
import BurnoutAlert from '../components/BurnoutAlert';
import Settings from '../components/Settings';
import { calculateBurnoutRisk, BurnoutStatus } from '../lib/engine';

export default function SynapseApp() {
  const [currentView, setCurrentView] = useState('editor');
  const [burnoutStatus, setBurnoutStatus] = useState<BurnoutStatus | null>(null);
  const [showAI, setShowAI] = useState(false);

  // Auto-pilot: Background Monitor
  useEffect(() => {
    const monitorVitals = () => {
      // Mock data - in production, fetch from your Firestore hooks
      const risk = calculateBurnoutRisk(12, 9); // High load example
      if (risk.riskLevel === 'HIGH') {
        setBurnoutStatus(risk);
        
        // Trigger Android Audio via JS Bridge
        if (window.Android) {
          window.Android.playAudioReminder("Burnout warning. Your schedule is critical. Should I reschedule your low-priority tasks?");
        }
      }
    };

    const interval = setInterval(monitorVitals, 60000); // Check every minute
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="flex h-screen bg-white text-[#37352F] overflow-hidden">
      {/* Sidebar Navigation */}
      <Sidebar onViewChange={setCurrentView} activeView={currentView} />

      <main className="flex-1 flex flex-col relative overflow-hidden">
        {/* Top Navbar */}
        <div className="h-12 border-b border-gray-100 flex items-center px-6 justify-between bg-white/80 backdrop-blur-md sticky top-0 z-10">
          <div className="text-xs font-medium text-gray-400 uppercase tracking-widest">
            Synapse / {currentView.replace('-', ' ')}
          </div>
          <div className="flex items-center gap-4">
            <button 
              onClick={() => setShowAI(!showAI)}
              className="text-xs font-bold text-blue-600 px-3 py-1 bg-blue-50 hover:bg-blue-100 rounded-full transition-colors"
            >
              Ask AI
            </button>
            <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-purple-500 to-blue-500 border-2 border-white shadow-sm" />
          </div>
        </div>

        {/* Dynamic View Rendering */}
        <div className="flex-1 overflow-y-auto">
          {currentView === 'editor' && <BlockEditor />}
          {currentView === 'kanban' && <KanbanBoard />}
          {currentView === 'finance' && <FinanceTracker />}
          {currentView === 'settings' && <Settings />}
        </div>

        {/* AI Overlay Panel */}
        {showAI && (
          <div className="absolute top-14 right-6 w-96 h-[calc(100%-5rem)] z-20 animate-in fade-in slide-in-from-right-4">
            <AISuggestionPanel onClose={() => setShowAI(false)} />
          </div>
        )}

        {/* Proactive Burnout Alert */}
        {burnoutStatus && (
          <BurnoutAlert 
            status={burnoutStatus} 
            onClose={() => setBurnoutStatus(null)} 
          />
        )}
      </main>
    </div>
  );
}
