import api from './axios';
import type { Partner } from '../types';

export const partnerApi = {
  list: () => api.get<Partner[]>('/partners').then((r) => r.data),
  get:  (id: number) => api.get<Partner>(`/partners/${id}`).then((r) => r.data),
  create: (data: Partial<Partner>) => api.post<Partner>('/partners', data).then((r) => r.data),
  update: (id: number, data: Partial<Partner>) =>
    api.put<Partner>(`/partners/${id}`, data).then((r) => r.data),
  testConnection: (id: number) =>
    api.post(`/partners/${id}/test-connection`).then((r) => r.data),
};
