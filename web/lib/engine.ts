/**
 * SYNAPSE CORE LOGIC ENGINE - Auto-pilot, Gamification & Automation
 */

export interface BurnoutStatus {
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  message: string;
  recommendation: string;
}

// 1. PREDICTIVE ANALYTICS: Burnout Detection
export const calculateBurnoutRisk = (activeTasks: number, calendarDensity: number): BurnoutStatus => {
  if (activeTasks > 10 || calendarDensity > 8) {
    return {
      riskLevel: 'HIGH',
      message: 'Critical Workload Detected',
      recommendation: 'Auto-pilot suggests rescheduling non-essential tasks and taking a 15-minute break.'
    };
  } else if (activeTasks > 6 || calendarDensity > 5) {
    return {
      riskLevel: 'MEDIUM',
      message: 'Moderate Load',
      recommendation: 'Try to focus on one task at a time. Keep hydrated!'
    };
  }
  return {
    riskLevel: 'LOW',
    message: 'Healthy Productivity',
    recommendation: 'You are in the flow state. Keep it up!'
  };
};

// 2. GAMIFICATION: EXP System
export const EXP_PER_LEVEL = 1000;

export const calculateLevel = (totalExp: number) => {
  const level = Math.floor(totalExp / EXP_PER_LEVEL) + 1;
  const expInLevel = totalExp % EXP_PER_LEVEL;
  const progress = (expInLevel / EXP_PER_LEVEL) * 100;
  
  return { level, expInLevel, progress };
};

export const getExpForAction = (action: 'TASK_COMPLETE' | 'STREAK_FINISH' | 'FINANCE_LOG') => {
  const rewards = {
    'TASK_COMPLETE': 50,
    'STREAK_FINISH': 200,
    'FINANCE_LOG': 20
  };
  return rewards[action];
};

// 3. SMART AUTOMATION: Task Staleness Detection
export const checkTaskStaleness = (lastOpenedDate: string, thresholdDays: number = 5) => {
  const lastDate = new Date(lastOpenedDate).getTime();
  const today = new Date().getTime();
  const diffDays = Math.ceil((today - lastDate) / (1000 * 60 * 60 * 24));
  
  return {
    isStale: diffDays >= thresholdDays,
    daysInactive: diffDays
  };
};
