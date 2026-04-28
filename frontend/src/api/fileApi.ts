import api from './axios';
import type { TmsFile, PageResponse, SchemaNode, ProcessingMode } from '../types';

export const fileApi = {
  list: (page = 0, size = 50, sort = 'receivedTimestamp', dir = 'desc') =>
    api
      .get<PageResponse<TmsFile>>('/files', { params: { page, size, sort, dir } })
      .then((r) => r.data),

  get: (entryNo: number) =>
    api.get<TmsFile>(`/files/${entryNo}`).then((r) => r.data),

  getStructure: (entryNo: number) =>
    api.get<SchemaNode>(`/files/${entryNo}/structure`).then((r) => r.data),

  upload: (file: File, partnerId: number, processingMode: ProcessingMode) => {
    const form = new FormData();
    form.append('file', file);
    form.append('partnerId', String(partnerId));
    form.append('processingMode', processingMode);
    return api
      .post<TmsFile>('/files/upload', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((r) => r.data);
  },

  process: (entryNo: number, mappingId?: number) =>
    api.post(`/files/${entryNo}/process`, { mappingId }).then((r) => r.data),

  retry: (entryNo: number) =>
    api.post(`/files/${entryNo}/retry`).then((r) => r.data),

  delete: (entryNo: number) =>
    api.delete(`/files/${entryNo}`).then((r) => r.data),

  downloadUrl: (entryNo: number) => `/api/v1/files/${entryNo}/download`,

  download: async (entryNo: number, fileName: string) => {
    const response = await api.get(`/files/${entryNo}/download`, { responseType: 'blob' });
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', fileName || `edi_file_${entryNo}`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },
};
