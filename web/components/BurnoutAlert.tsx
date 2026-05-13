import React from 'react';
import { AlertTriangle, X, Coffee, Brain } from 'lucide-react';
import { BurnoutStatus } from '../lib/engine';

interface BurnoutAlertProps {
  status: BurnoutStatus;
  onClose: () => void;
}

const BurnoutAlert = ({ status, onClose }: BurnoutAlertProps) => {
  if (status.riskLevel === 'LOW') return null;

  const isHigh = status.riskLevel === 'HIGH';

  return (
    <div className={`fixed bottom-6 right-6 max-w-sm w-full shadow-2xl rounded-2xl border overflow-hidden transition-all transform animate-in slide-in-from-bottom-10 ${
      isHigh ? 'bg-red-50 border-red-200' : 'bg-amber-50 border-amber-200'
    }`}>
      <div className="p-5">
        <div className="flex justify-between items-start mb-3">
          <div className={`p-2 rounded-lg ${isHigh ? 'bg-red-100 text-red-600' : 'bg-amber-100 text-amber-600'}`}>
            {isHigh ? <AlertTriangle size={20} /> : <Brain size={20} />}
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 transition-colors">
            <X size={18} />
          </button>
        </div>

        <h3 className={`font-bold text-lg mb-1 ${isHigh ? 'text-red-900' : 'text-amber-900'}`}>
          {status.message}
        </h3>
        <p className={`text-sm leading-relaxed mb-4 ${isHigh ? 'text-red-700' : 'text-amber-700'}`}>
          {status.recommendation}
        </p>

        <div className="flex gap-2">
          <button className={`flex-1 py-2 px-4 rounded-xl text-sm font-semibold transition-colors ${
            isHigh ? 'bg-red-600 text-white hover:bg-red-700' : 'bg-amber-600 text-white hover:bg-amber-700'
          }`}>
            Auto-Reschedule
          </button>
          <button className="flex items-center justify-center p-2 rounded-xl border border-gray-200 bg-white hover:bg-gray-50 transition-colors text-gray-600">
            <Coffee size={18} />
          </button>
        </div>
      </div>
      
      {/* Animated progress bar for auto-action */}
      {isHigh && (
        <div className="h-1 bg-red-100 w-full">
          <div className="h-full bg-red-500 animate-progress-shrink" style={{ width: '100%' }}></div>
        </div>
      )}
    </div>
  );
};

export default BurnoutAlert;
