import React from 'react';
import { ShoppingBag, Lock, Zap, Layout, Paintbrush } from 'lucide-react';

const RewardsShop = () => {
  const rewards = [
    { id: '1', name: 'Dark Neural Theme', cost: 500, type: 'theme', icon: <Paintbrush size={20}/> },
    { id: '2', name: 'Student Planner Pro', cost: 1200, type: 'template', icon: <Layout size={20}/> },
    { id: '3', name: 'AI Deep Insights', cost: 3000, type: 'feature', icon: <Zap size={20}/> },
  ];

  return (
    <div className="flex-1 p-12 bg-[#FBFBFA]">
      <div className="max-w-4xl mx-auto">
        <header className="mb-12">
          <h1 className="text-3xl font-bold flex items-center gap-3">
            <ShoppingBag className="text-indigo-600" />
            Rewards Marketplace
          </h1>
          <p className="text-gray-500 mt-2">Spend your earned EXP to upgrade your neural workspace.</p>
        </header>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {rewards.map(reward => (
            <div key={reward.id} className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm hover:shadow-md transition-all group">
              <div className="w-12 h-12 bg-gray-50 rounded-xl flex items-center justify-center mb-4 text-gray-600 group-hover:bg-indigo-50 group-hover:text-indigo-600 transition-colors">
                {reward.icon}
              </div>
              <h3 className="font-bold text-gray-800">{reward.name}</h3>
              <div className="mt-4 flex items-center justify-between">
                <span className="text-indigo-600 font-bold text-sm">{reward.cost} EXP</span>
                <button className="flex items-center gap-2 px-3 py-1.5 bg-gray-900 text-white text-xs font-bold rounded-lg hover:bg-black transition-colors">
                  <Lock size={14} />
                  Unlock
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default RewardsShop;
