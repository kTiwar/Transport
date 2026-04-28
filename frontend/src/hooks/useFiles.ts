import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { fileApi } from '../api/fileApi';
import toast from 'react-hot-toast';

export function useFiles(page = 0, size = 50) {
  return useQuery({
    queryKey: ['files', page, size],
    queryFn: () => fileApi.list(page, size),
    placeholderData: (prev) => prev,
  });
}

export function useFile(entryNo: number | null) {
  return useQuery({
    queryKey: ['file', entryNo],
    queryFn: () => fileApi.get(entryNo!),
    enabled: entryNo != null,
  });
}

export function useFileStructure(entryNo: number | null) {
  return useQuery({
    queryKey: ['file-structure', entryNo],
    queryFn: () => fileApi.getStructure(entryNo!),
    enabled: entryNo != null,
    staleTime: Infinity,
  });
}

function apiErrorMessage(err: unknown): string | null {
  const ax = err as {
    response?: { status?: number; data?: { message?: string } };
  };
  const m = ax.response?.data?.message;
  return typeof m === 'string' && m.trim() ? m.trim() : null;
}

export function useProcessFile() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ entryNo, mappingId }: { entryNo: number; mappingId?: number }) =>
      fileApi.process(entryNo, mappingId),
    onSuccess: (_, { entryNo }) => {
      toast.success('Processing finished');
      qc.invalidateQueries({ queryKey: ['files'] });
      qc.invalidateQueries({ queryKey: ['file', entryNo] });
    },
    onError: (err: unknown) => {
      const ax = err as { response?: { status?: number } };
      if (ax.response?.status === 403) {
        toast.error('You need Operator or Admin role to process files.');
        return;
      }
      toast.error(apiErrorMessage(err) ?? 'Failed to process file');
    },
  });
}

export function useRetryFile() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (entryNo: number) => fileApi.retry(entryNo),
    onSuccess: (_, entryNo) => {
      toast.success('File reset — you can run Process again');
      qc.invalidateQueries({ queryKey: ['files'] });
      qc.invalidateQueries({ queryKey: ['file', entryNo] });
    },
    onError: (err: unknown) =>
      toast.error(apiErrorMessage(err) ?? 'Reset failed'),
  });
}

export function useDeleteFile() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (entryNo: number) => fileApi.delete(entryNo),
    onSuccess: () => {
      toast.success('File deleted');
      qc.invalidateQueries({ queryKey: ['files'] });
    },
    onError: () => toast.error('Delete failed'),
  });
}

export function useUploadFile() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      file,
      partnerId,
      mode,
    }: {
      file: File;
      partnerId: number;
      mode: import('../types').ProcessingMode;
    }) => fileApi.upload(file, partnerId, mode),
    onSuccess: () => {
      toast.success('File uploaded successfully');
      qc.invalidateQueries({ queryKey: ['files'] });
    },
    onError: (err: unknown) => {
      const msg =
        err &&
        typeof err === 'object' &&
        'response' in err &&
        (err as { response?: { data?: { message?: string } } }).response?.data?.message;
      toast.error(typeof msg === 'string' && msg ? msg : 'Upload failed');
    },
  });
}
