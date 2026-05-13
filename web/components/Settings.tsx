import React, { useState } from 'react';
import { Bell, Shield, Cpu, Key, User, Moon, Sun, Volume2 } from 'lucide-react';

const Settings = () => {
  const [automationEnabled, setAutomationEnabled] = useState(true);
  const [burnoutSensitivity, setBurnoutSensitivity] = useState(7);
  const [darkMode, setDarkMode] = useState(false);

  return (
    <div className="max-w-4xl mx-auto p-12">
      <h1 className="text-3xl font-bold mb-8">Settings</h1>

      <div className="space-y-12">
        {/* Profile Section */}
        <section>
          <div className="flex items-center gap-2 mb-4 text-gray-500">
            <User size={18} />
            <h2 className="text-sm font-semibold uppercase tracking-wider">Account</h2>
          </div>
          <div className="bg-white border border-gray-200 rounded-xl p-6 flex items-center justify-between">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 font-bold">JD</div>
              <div>
                <div className="font-semibold">John Doe</div>
                <div className="text-sm text-gray-400">john@example.com</div>
              </div>
            </div>
            <button className="text-sm text-blue-600 font-medium hover:underline">Edit Profile</button>
          </div>
        </section>

        {/* Auto-pilot & AI Section */}
        <section>
          <div className="flex items-center gap-2 mb-4 text-gray-500">
            <Cpu size={18} />
            <h2 className="text-sm font-semibold uppercase tracking-wider">Auto-pilot & AI</h2>
          </div>
          <div className="bg-white border border-gray-200 rounded-xl divide-y divide-gray-100">
            <div className="p-6 flex items-center justify-between">
              <div>
                <div className="font-medium">Smart Automation</div>
                <div className="text-sm text-gray-400">Automatically highlight stale tasks and manage burnout risk.</div>
              </div>
              <button 
                onClick={() => setAutomationEnabled(!automationEnabled)}
                className={`w-12 h-6 rounded-full transition-colors relative ${automationEnabled ? 'bg-blue-600' : 'bg-gray-200'}`}
              >
                <div className={`absolute top-1 w-4 h-4 bg-white rounded-full transition-all ${automationEnabled ? 'left-7' : 'left-1'}`} />
              </button>
            </div>
            <div className="p-6">
              <div className="flex items-center justify-between mb-2">
                <div className="font-medium">Burnout Sensitivity</div>
                <span className="text-sm font-bold text-blue-600">{burnoutSensitivity}</span>
              </div>
              <input 
                type="range" min="1" max="10" value={burnoutSensitivity} 
                onChange={(e) => setBurnoutSensitivity(parseInt(e.target.value))}
                className="w-full h-2 bg-gray-100 rounded-lg appearance-none cursor-pointer accent-blue-600"
              />
              <div className="flex justify-between text-[10px] text-gray-400 mt-2 uppercase">
                <span>Relaxed</span>
                <span>Strict</span>
              </div>
            </div>
            <div className="p-6 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <Key size={18} className="text-gray-400" />
                <div className="font-medium">Gemini API Key</div>
              </div>
              <input 
                type="password" 
                placeholder="sk-••••••••••••"
                className="bg-gray-50 border border-gray-200 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>
        </section>

        {/* UI & Audio Section */}
        <section>
          <div className="flex items-center gap-2 mb-4 text-gray-500">
            <Bell size={18} />
            <h2 className="text-sm font-semibold uppercase tracking-wider">Interface & Audio</h2>
          </div>
          <div className="bg-white border border-gray-200 rounded-xl divide-y divide-gray-100">
            <div className="p-6 flex items-center justify-between">
              <div className="flex items-center gap-3">
                {darkMode ? <Moon size={18} /> : <Sun size={18} />}
                <div className="font-medium">Appearance</div>
              </div>
              <button 
                onClick={() => setDarkMode(!darkMode)}
                className="text-xs bg-gray-100 px-3 py-1 rounded-md font-medium"
              >
                {darkMode ? 'Dark Mode' : 'Light Mode'}
              </button>
            </div>
            <div className="p-6 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <Volume2 size={18} />
                <div className="font-medium">Audio Reminders</div>
              </div>
              <select className="bg-gray-50 border border-gray-200 rounded-lg px-3 py-1.5 text-sm outline-none">
                <option>Motivational (Predefined)</option>
                <option>Calm (Predefined)</option>
                <option>Urgent (Predefined)</option>
                <option>Custom Upload...</option>
              </select>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
};

export default Settings;
