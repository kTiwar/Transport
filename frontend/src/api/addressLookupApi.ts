import api from './axios';

export type LookupCategory =
  | 'COUNTRY'
  | 'STATE'
  | 'CITY'
  | 'POSTAL_CODE'
  | 'ADDRESS_TYPE'
  | 'REGION'
  | 'ZONE';

export interface LookupOption {
  category: LookupCategory;
  code: string;
  name: string;
  description: string | null;
  extra: Record<string, string>;
}

export interface LookupDependencies {
  country?: LookupOption;
  state?: LookupOption;
  city?: LookupOption;
  postal?: LookupOption;
  region?: LookupOption;
  zone?: LookupOption;
}

export const addressLookupApi = {
  categories: () => api.get<LookupCategory[]>('/address-lookups/categories').then((r) => r.data),

  options: (params: {
    category: LookupCategory;
    q?: string;
    parentCode?: string;
    activeOnly?: boolean;
    limit?: number;
  }) =>
    api
      .get<LookupOption[]>('/address-lookups/options', {
        params: {
          ...params,
          activeOnly: params.activeOnly ?? true,
          limit: params.limit ?? 100,
        },
      })
      .then((r) => r.data),

  byCode: (category: LookupCategory, code: string) =>
    api
      .get<LookupOption>('/address-lookups/by-code', { params: { category, code } })
      .then((r) => r.data),

  dependencies: (category: LookupCategory, code: string) =>
    api
      .get<LookupDependencies>('/address-lookups/dependencies', { params: { category, code } })
      .then((r) => r.data),
};