import api from './axios';
import type { EdiError, PageResponse } from '../types';

export const errorApi = {
  list: (page = 0, size = 50, showResolved = false) =>
    api
      .get<PageResponse<EdiError>>('/errors', {
        params: { page, size, resolvedOnly: showResolved },
      })
      .then((r) => r.data),

  listForFile: (entryNo: number) =>
    api.get<EdiError[]>(`/errors/file/${entryNo}`).then((r) => r.data),

  resolve: (errorId: number, note: string) =>
    api.put<EdiError>(`/errors/${errorId}/resolve`, { note }).then((r) => r.data),
};
