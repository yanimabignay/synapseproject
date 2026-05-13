import React, { useState } from 'react';
import { Plus, MoreHorizontal } from 'lucide-react';

interface Task {
  id: string;
  title: string;
  priority: 'low' | 'medium' | 'high';
  status: 'todo' | 'in-progress' | 'done';
}

const KanbanBoard = () => {
  const [tasks, setTasks] = useState<Task[]>([
    { id: '1', title: 'Design Landing Page', priority: 'high', status: 'todo' },
    { id: '2', title: 'Setup Firebase Auth', priority: 'medium', status: 'in-progress' },
    { id: '3', title: 'Initial Project Setup', priority: 'low', status: 'done' },
  ]);

  const columns = [
    { id: 'todo', title: 'To Do' },
    { id: 'in-progress', title: 'In Progress' },
    { id: 'done', title: 'Done' },
  ];

  const getTasksByStatus = (status: string) => tasks.filter(t => t.status === status);

  return (
    <div className="flex-1 p-8 bg-white overflow-x-auto">
      <div className="flex gap-6 min-w-max">
        {columns.map(column => (
          <div key={column.id} className="w-80 flex flex-col gap-4">
            <div className="flex items-center justify-between px-2">
              <div className="flex items-center gap-2">
                <span className="font-semibold text-[#37352F]">{column.title}</span>
                <span className="text-gray-400 text-sm">{getTasksByStatus(column.id).length}</span>
              </div>
              <div className="flex items-center gap-1 text-gray-400">
                <Plus size={18} className="hover:bg-gray-100 rounded cursor-pointer p-0.5" />
                <MoreHorizontal size={18} className="hover:bg-gray-100 rounded cursor-pointer p-0.5" />
              </div>
            </div>

            <div className="flex flex-col gap-3">
              {getTasksByStatus(column.id).map(task => (
                <div 
                  key={task.id}
                  className="bg-white border border-gray-200 rounded-lg p-4 shadow-sm hover:shadow-md transition-shadow cursor-grab active:cursor-grabbing group"
                >
                  <div className="flex justify-between items-start mb-2">
                    <span className={`text-[10px] font-bold uppercase px-2 py-0.5 rounded ${
                      task.priority === 'high' ? 'bg-red-100 text-red-700' :
                      task.priority === 'medium' ? 'bg-yellow-100 text-yellow-700' :
                      'bg-blue-100 text-blue-700'
                    }`}>
                      {task.priority}
                    </span>
                  </div>
                  <h3 className="text-sm text-[#37352F] font-medium leading-snug">{task.title}</h3>
                  
                  {/* Auto-pilot Suggestion Mockup */}
                  {task.priority === 'high' && column.id === 'todo' && (
                    <div className="mt-3 pt-3 border-t border-gray-100 text-[11px] text-blue-600 flex items-center gap-1">
                      <div className="w-1.5 h-1.5 bg-blue-500 rounded-full animate-pulse" />
                      AI Suggestion: Start this next
                    </div>
                  )}
                </div>
              ))}
              <button className="text-gray-400 text-sm flex items-center gap-2 px-2 py-1.5 hover:bg-gray-100 rounded transition-colors mt-1">
                <Plus size={16} /> New
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default KanbanBoard;
