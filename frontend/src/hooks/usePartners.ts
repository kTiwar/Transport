import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { partnerApi } from '../api/partnerApi';
import toast from 'react-hot-toast';
import type { Partner } from '../types';

export function usePartners() {
  return useQuery({ queryKey: ['partners'], queryFn: partnerApi.list });
}

export function useCreatePartner() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<Partner>) => partnerApi.create(data),
    onSuccess: () => {
      toast.success('Partner created');
      qc.invalidateQueries({ queryKey: ['partners'] });
    },
    onError: () => toast.error('Failed to create partner'),
  });
}

export function useUpdatePartner() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<Partner> }) =>
      partnerApi.update(id, data),
    onSuccess: () => {
      toast.success('Partner updated');
      qc.invalidateQueries({ queryKey: ['partners'] });
    },
    onError: () => toast.error('Failed to update partner'),
  });
}
