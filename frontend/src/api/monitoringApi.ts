import api from './axios';
import type { MonitoringStats } from '../types';

export const monitoringApi = {
  getStats: () => api.get<MonitoringStats>('/monitoring/stats').then((r) => r.data),
};
