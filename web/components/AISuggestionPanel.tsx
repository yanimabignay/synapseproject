import React, { useState } from 'react';
import { Sparkles, ArrowRight, BrainCircuit, Zap, User, Briefcase, GraduationCap } from 'lucide-react';

type UserPersona = 'student' | 'freelancer' | 'general';

const AISuggestionPanel = () => {
  const [userPersona, setUserPersona] = useState<UserPersona>('general');
  const [isLoading, setIsLoading] = useState(false);
  const [suggestion, setSuggestion] = useState<string | null>(null);

  const generateDashboard = () => {
    setIsLoading(true);
    // Mocking Gemini API logic
    setTimeout(() => {
      const suggestions = {
        student: "Based on your upcoming 'Advanced Math' exam, I've prioritized study blocks and created a 3-day revision streak goal.",
        freelancer: "Detected 3 pending invoices. I've added 'Payment Follow-up' to your priority list and blocked out deep-work time for 'Client Project A'.",
        general: "Your schedule looks dense. I recommend tackling the 'Project Synapse' design task now while your focus level is typically high."
      };
      setSuggestion(suggestions[userPersona]);
      setIsLoading(false);
    }, 1500);
  };

  return (
    <div className="bg-white border border-gray-200 rounded-2xl shadow-sm overflow-hidden flex flex-col h-full">
      <div className="p-6 border-b border-gray-100 bg-gradient-to-br from-blue-50 to-white">
        <div className="flex items-center gap-2 text-blue-600 mb-1">
          <Sparkles size={20} fill="currentColor" />
          <span className="font-bold text-sm uppercase tracking-wider">AI Co-Pilot</span>
        </div>
        <h2 className="text-xl font-bold text-[#37352F]">What should I do next?</h2>
      </div>

      <div className="p-6 flex-1 flex flex-col gap-6">
        {/* Persona Selector */}
        <div>
          <label className="text-xs font-semibold text-gray-500 uppercase mb-3 block">My Profile</label>
          <div className="grid grid-cols-3 gap-2">
            {[
              { id: 'student', icon: GraduationCap, label: 'Student' },
              { id: 'freelancer', icon: Briefcase, label: 'Freelancer' },
              { id: 'general', icon: User, label: 'General' }
            ].map((p) => (
              <button
                key={p.id}
                onClick={() => setUserPersona(p.id as UserPersona)}
                className={`flex flex-col items-center gap-2 p-3 rounded-xl border transition-all ${
                  userPersona === p.id 
                  ? 'border-blue-500 bg-blue-50 text-blue-600' 
                  : 'border-gray-100 hover:bg-gray-50 text-gray-400'
                }`}
              >
                <p.icon size={20} />
                <span className="text-[10px] font-medium">{p.label}</span>
              </button>
            ))}
          </div>
        </div>

        {/* Suggestion Output */}
        <div className="flex-1 bg-gray-50 rounded-xl p-5 border border-dashed border-gray-200 flex flex-col items-center justify-center text-center">
          {isLoading ? (
            <div className="flex flex-col items-center gap-3">
              <div className="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
              <p className="text-sm text-gray-500">Syncing with Gemini...</p>
            </div>
          ) : suggestion ? (
            <div className="animate-in fade-in zoom-in duration-300">
              <div className="bg-white p-3 rounded-full w-fit mx-auto mb-4 shadow-sm">
                <BrainCircuit className="text-blue-500" size={24} />
              </div>
              <p className="text-[#37352F] text-sm leading-relaxed italic">"{suggestion}"</p>
            </div>
          ) : (
            <p className="text-gray-400 text-sm">Select your persona and let the AI analyze your workspace.</p>
          )}
        </div>

        <button
          onClick={generateDashboard}
          disabled={isLoading}
          className="w-full bg-[#37352F] text-white py-3 rounded-xl font-semibold flex items-center justify-center gap-2 hover:bg-black transition-colors disabled:opacity-50"
        >
          {suggestion ? <Zap size={18} /> : <ArrowRight size={18} />}
          {suggestion ? 'Refresh Suggestions' : 'Analyze My Day'}
        </button>
      </div>
    </div>
  );
};

export default AISuggestionPanel;
