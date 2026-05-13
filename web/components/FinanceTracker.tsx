import React, { useState } from 'react';
import { Plus, ArrowUpRight, ArrowDownLeft, Wallet, TrendingUp } from 'lucide-react';

interface Transaction {
  id: string;
  type: 'income' | 'expense';
  category: string;
  amount: number;
  date: string;
  description: string;
}

const FinanceTracker = () => {
  const [transactions, setTransactions] = useState<Transaction[]>([
    { id: '1', type: 'income', category: 'Salary', amount: 5000, date: '2023-10-01', description: 'Monthly Salary' },
    { id: '2', type: 'expense', category: 'Rent', amount: 1500, date: '2023-10-02', description: 'Apartment Rent' },
    { id: '3', type: 'expense', category: 'Food', amount: 200, date: '2023-10-03', description: 'Groceries' },
  ]);

  const totalIncome = transactions.filter(t => t.type === 'income').reduce((acc, t) => acc + t.amount, 0);
  const totalExpense = transactions.filter(t => t.type === 'expense').reduce((acc, t) => acc + t.amount, 0);
  const balance = totalIncome - totalExpense;

  return (
    <div className="flex-1 p-8 bg-[#FBFBFA] overflow-y-auto">
      <div className="max-w-5xl mx-auto">
        <h1 className="text-3xl font-bold text-[#37352F] mb-8">Finance Dashboard</h1>

        {/* Summary Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm">
            <div className="flex items-center gap-3 text-gray-500 mb-2">
              <Wallet size={20} />
              <span className="text-sm font-medium">Total Balance</span>
            </div>
            <div className="text-2xl font-bold text-[#37352F]">${balance.toLocaleString()}</div>
          </div>
          <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm">
            <div className="flex items-center gap-3 text-green-500 mb-2">
              <ArrowUpRight size={20} />
              <span className="text-sm font-medium">Income</span>
            </div>
            <div className="text-2xl font-bold text-[#37352F]">${totalIncome.toLocaleString()}</div>
          </div>
          <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm">
            <div className="flex items-center gap-3 text-red-500 mb-2">
              <ArrowDownLeft size={20} />
              <span className="text-sm font-medium">Expenses</span>
            </div>
            <div className="text-2xl font-bold text-[#37352F]">${totalExpense.toLocaleString()}</div>
          </div>
        </div>

        {/* Main Content Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Transactions List */}
          <div className="lg:col-span-2 bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
            <div className="p-4 border-b border-gray-100 flex justify-between items-center">
              <h2 className="font-semibold text-[#37352F]">Recent Transactions</h2>
              <button className="p-1.5 hover:bg-gray-100 rounded-lg text-blue-600">
                <Plus size={20} />
              </button>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-gray-50 text-gray-500 uppercase text-[10px] font-bold">
                  <tr>
                    <th className="px-6 py-3">Category</th>
                    <th className="px-6 py-3">Description</th>
                    <th className="px-6 py-3 text-right">Amount</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {transactions.map(t => (
                    <tr key={t.id} className="hover:bg-gray-50 transition-colors">
                      <td className="px-6 py-4">
                        <span className="px-2 py-1 bg-gray-100 rounded text-xs font-medium">{t.category}</span>
                      </td>
                      <td className="px-6 py-4 text-[#37352F]">{t.description}</td>
                      <td className={`px-6 py-4 text-right font-medium ${t.type === 'income' ? 'text-green-600' : 'text-red-600'}`}>
                        {t.type === 'income' ? '+' : '-'}${t.amount.toLocaleString()}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Analytics Placeholder */}
          <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm">
            <div className="flex items-center gap-2 mb-6">
              <TrendingUp size={20} className="text-blue-500" />
              <h2 className="font-semibold text-[#37352F]">Quick Analytics</h2>
            </div>
            <div className="space-y-6">
              <div>
                <div className="flex justify-between text-xs mb-2">
                  <span className="text-gray-500">Fixed Costs (Rent, etc.)</span>
                  <span className="font-medium">75%</span>
                </div>
                <div className="w-full bg-gray-100 h-2 rounded-full overflow-hidden">
                  <div className="bg-blue-500 h-full" style={{ width: '75%' }}></div>
                </div>
              </div>
              <div>
                <div className="flex justify-between text-xs mb-2">
                  <span className="text-gray-500">Discretionary (Food, Fun)</span>
                  <span className="font-medium">25%</span>
                </div>
                <div className="w-full bg-gray-100 h-2 rounded-full overflow-hidden">
                  <div className="bg-yellow-500 h-full" style={{ width: '25%' }}></div>
                </div>
              </div>
            </div>
            <div className="mt-8 p-4 bg-blue-50 rounded-lg border border-blue-100">
              <p className="text-xs text-blue-700 leading-relaxed">
                <span className="font-bold">AI Tip:</span> You've spent 15% more on food than last week. Consider the "Saving Streak" challenge to earn 100 EXP!
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default FinanceTracker;
