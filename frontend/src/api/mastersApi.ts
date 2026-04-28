import api from './axios';

export interface PagedResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ReferenceMasterRow {
  id: number;
  category: string;
  code: string;
  name: string;
  description: string | null;
  extraJson: string | null;
  sortOrder: number | null;
  isActive: boolean;
}

export interface MasterPartyRow {
  id: number;
  partyType: string;
  partyCode: string;
  name: string;
  legalName: string | null;
  vatNumber: string | null;
  countryCode: string | null;
  city: string | null;
  email: string | null;
  phone: string | null;
  isActive: boolean;
}

export const mastersApi = {
  referenceCategories: () =>
    api.get<string[]>('/reference-master/categories').then((r) => r.data),

  referenceList: (category: string, page = 0, size = 50, activeOnly = true) =>
    api
      .get<PagedResult<ReferenceMasterRow>>('/reference-master', {
        params: { category, page, size, activeOnly },
      })
      .then((r) => r.data),

  partyTypes: () => api.get<string[]>('/master-parties/types').then((r) => r.data),

  partiesList: (partyType: string, page = 0, size = 30, activeOnly = true) =>
    api
      .get<PagedResult<MasterPartyRow>>('/master-parties', {
        params: { partyType, page, size, activeOnly },
      })
      .then((r) => r.data),
};
