import { useQuery } from '@tanstack/react-query';
import { monitoringApi } from '../api/monitoringApi';

export function useMonitoringStats() {
  return useQuery({
    queryKey: ['monitoring-stats'],
    queryFn: monitoringApi.getStats,
    refetchInterval: 30_000, // auto-refresh every 30s
  });
}
