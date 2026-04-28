import api from './axios';

export interface AddressMasterSummary {
  addressId: number;
  addressCode: string;
  addressType: string | null;
  entityType: string | null;
  entityId: number | null;
  city: string | null;
  postalCode: string | null;
  countryCode: string | null;
  isPrimary: boolean;
  isActive: boolean;
  validationStatus: string | null;
  updatedAt: string | null;
}

export interface AddressContact {
  contactId: number;
  contactName: string | null;
  phoneNumber: string | null;
  alternatePhone: string | null;
  email: string | null;
  isPrimaryContact: boolean;
}

export interface AddressAttribute {
  attrId: number;
  attrKey: string;
  attrValue: string | null;
}

export interface AddressUsage {
  usageId: number;
  usageType: string | null;
  priority: number | null;
}

export interface AddressI18n {
  id: number;
  languageCode: string;
  addressText: string | null;
}

export interface AddressAudit {
  auditId: number;
  changedBy: string | null;
  oldValue: string | null;
  newValue: string | null;
  changedAt: string | null;
}

export interface AddressMasterDetail extends AddressMasterSummary {
  addressLine1: string | null;
  addressLine2: string | null;
  addressLine3: string | null;
  landmark: string | null;
  district: string | null;
  stateProvince: string | null;
  countryName: string | null;
  latitude: number | null;
  longitude: number | null;
  timezone: string | null;
  createdAt: string | null;
  contacts: AddressContact[];
  attributes: AddressAttribute[];
  usages: AddressUsage[];
  translations: AddressI18n[];
  auditTrail: AddressAudit[];
}

export interface PagedResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface AddressMasterImportResult {
  rowsRead: number;
  inserted: number;
  updated: number;
  skipped: number;
  errors: string[];
}

export interface AddressMasterUpsertPayload {
  addressCode: string;
  addressType: string | null;
  entityType: string | null;
  entityId: number | null;
  addressLine1: string | null;
  addressLine2: string | null;
  addressLine3: string | null;
  landmark: string | null;
  city: string | null;
  district: string | null;
  stateProvince: string | null;
  postalCode: string | null;
  countryCode: string | null;
  countryName: string | null;
  latitude: number | null;
  longitude: number | null;
  timezone: string | null;
  isPrimary: boolean;
  isActive: boolean;
  validationStatus: string | null;
}

export const addressMasterApi = {
  list: (page = 0, size = 20, q?: string) =>
    api
      .get<PagedResult<AddressMasterSummary>>('/address-master', {
        params: { page, size, ...(q?.trim() ? { q: q.trim() } : {}) },
      })
      .then((r) => r.data),

  get: (addressId: number) =>
    api.get<AddressMasterDetail>(`/address-master/${addressId}`).then((r) => r.data),

  create: (payload: AddressMasterUpsertPayload) =>
    api.post<AddressMasterDetail>('/address-master', payload).then((r) => r.data),

  update: (addressId: number, payload: AddressMasterUpsertPayload) =>
    api.put<AddressMasterDetail>(`/address-master/${addressId}`, payload).then((r) => r.data),

  deactivate: (addressId: number) =>
    api.patch<AddressMasterSummary>(`/address-master/${addressId}/deactivate`).then((r) => r.data),

  activate: (addressId: number) =>
    api.patch<AddressMasterSummary>(`/address-master/${addressId}/activate`).then((r) => r.data),

  remove: (addressId: number) => api.delete(`/address-master/${addressId}`),

  importExcel: (file: File) => {
    const fd = new FormData();
    fd.append('file', file);
    return api
      .post<AddressMasterImportResult>('/address-master/import', fd)
      .then((r) => r.data);
  },

  downloadImportTemplate: () =>
    api
      .get<Blob>('/address-master/import/template', { responseType: 'blob' })
      .then((r) => r.data),
};