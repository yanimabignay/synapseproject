import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

const CalendarView = ({ tasks }: { tasks: any[] }) => {
  const days = Array.from({ length: 35 }, (_, i) => i + 1); // Simple grid mockup

  return (
    <div className="flex-1 p-8 bg-white">
      <div className="flex justify-between items-center mb-8">
        <h2 className="text-2xl font-bold">October 2023</h2>
        <div className="flex gap-2">
          <button className="p-2 hover:bg-gray-100 rounded-full"><ChevronLeft size={20}/></button>
          <button className="p-2 hover:bg-gray-100 rounded-full"><ChevronRight size={20}/></button>
        </div>
      </div>

      <div className="grid grid-cols-7 border-t border-l border-gray-100">
        {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map(day => (
          <div key={day} className="p-4 border-r border-b border-gray-100 text-xs font-bold text-gray-400 uppercase">
            {day}
          </div>
        ))}
        {days.map(day => {
            const hasTask = day % 7 === 0;
            return (
              <div key={day} className="h-32 p-2 border-r border-b border-gray-100 relative group hover:bg-gray-50 transition-colors">
                <span className="text-sm font-medium text-gray-400">{day > 31 ? day - 31 : day}</span>
                {hasTask && (
                  <div className="mt-2 p-1 bg-indigo-100 text-indigo-700 text-[10px] rounded border border-indigo-200 truncate">
                    Priority: Review AI Dashboard
                  </div>
                )}
              </div>
            );
        })}
      </div>
    </div>
  );
};

export default CalendarView;
