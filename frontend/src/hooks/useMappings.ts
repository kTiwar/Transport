import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { mappingApi } from '../api/mappingApi';
import toast from 'react-hot-toast';
import type { Mapping } from '../types';
import type { MappingVersionHistoryItem } from '../api/mappingApi';

export function useMappings(partnerId?: number) {
  return useQuery({
    queryKey: ['mappings', partnerId],
    queryFn: () => mappingApi.list(partnerId),
  });
}

export function useMapping(mappingId: number | null) {
  return useQuery({
    queryKey: ['mapping', mappingId],
    queryFn: () => mappingApi.get(mappingId!),
    enabled: mappingId != null,
    staleTime: 0,          // always re-fetch on mount so lines are fresh
    refetchOnMount: true,
  });
}

export function useVersionHistory(mappingId: number | null) {
  return useQuery<MappingVersionHistoryItem[]>({
    queryKey: ['mapping-versions', mappingId],
    queryFn: () => mappingApi.getVersionHistory(mappingId!),
    enabled: mappingId != null,
  });
}

export function useCreateMapping() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<Mapping>) => mappingApi.create(data),
    onSuccess: () => {
      toast.success('Mapping created');
      qc.invalidateQueries({ queryKey: ['mappings'] });
    },
    onError: () => toast.error('Failed to create mapping'),
  });
}

function saveErrorMessage(err: unknown): string {
  const ax = err as { response?: { data?: { message?: string; error?: string } } };
  const d = ax.response?.data;
  return (d?.message || d?.error || 'Failed to save mapping') as string;
}

export function useSaveMappingLines() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ mappingId, lines }: { mappingId: number; lines: any[] }) =>
      mappingApi.update(mappingId, { lines }),
    onSuccess: (_data, { mappingId }) => {
      toast.success('Mapping saved');
      qc.invalidateQueries({ queryKey: ['mapping', mappingId] });
      qc.invalidateQueries({ queryKey: ['mappings'] });
    },
    onError: (err) => toast.error(saveErrorMessage(err)),
  });
}

export function useActivateMapping() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (mappingId: number) => mappingApi.activate(mappingId),
    onSuccess: () => {
      toast.success('Mapping activated');
      qc.invalidateQueries({ queryKey: ['mappings'] });
    },
    onError: () => toast.error('Activation failed'),
  });
}

export function useAiSuggest(entryNo: number | null) {
  return useQuery({
    queryKey: ['ai-suggest', entryNo],
    queryFn: () => mappingApi.aiSuggest(entryNo!),
    enabled: entryNo != null,
    staleTime: 5 * 60 * 1000,
  });
}
