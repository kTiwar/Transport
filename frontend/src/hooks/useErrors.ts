import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { errorApi } from '../api/errorApi';
import toast from 'react-hot-toast';

export function useErrors(page = 0, size = 50, showResolved = false) {
  return useQuery({
    queryKey: ['errors', page, size, showResolved],
    queryFn: () => errorApi.list(page, size, showResolved),
    placeholderData: (prev) => prev,
  });
}

export function useFileErrors(entryNo: number | null) {
  return useQuery({
    queryKey: ['file-errors', entryNo],
    queryFn: () => errorApi.listForFile(entryNo!),
    enabled: entryNo != null,
  });
}

export function useResolveError() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ errorId, note }: { errorId: number; note: string }) =>
      errorApi.resolve(errorId, note),
    onSuccess: () => {
      toast.success('Error marked as resolved');
      qc.invalidateQueries({ queryKey: ['errors'] });
    },
    onError: () => toast.error('Failed to resolve error'),
  });
}
