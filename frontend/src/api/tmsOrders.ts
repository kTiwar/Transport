import api from './axios';

export interface TmsOrderLine {
  id: number;
  lineNo: number;
  actionCode: string;
  addressNo: string;
  initialDatetimeFrom: string | null;
  initialDatetimeUntil: string | null;
  requestedDatetimeFrom: string | null;
  requestedDatetimeUntil: string | null;
  containerNo: string;
  loaded: boolean;
}

export interface TmsOrderCargo {
  id: number;
  lineNo: number;
  goodNo: string;
  goodTypeCode: string;
  goodSubTypeCode: string;
  quantity: number;
  unitOfMeasureCode: string;
  description: string;
  netWeight: number;
  grossWeight: number;
  adrType: string;
  dangerousGoods: boolean;
}

export interface TmsOrderReference {
  id: number;
  referenceCode: string;
  reference: string;
  orderLineNo: number;
}

export interface TmsOrder {
  id: number;
  orderNo: string;
  customerNo: string;
  transportType: string;
  tripTypeNo: string;
  office: string;
  carrierNo: string;
  communicationPartner: string;
  source: string;
  status: string;
  countryOfOrigin: string;
  countryOfDestination: string;
  orderDate: string;
  impEntryNo: number;
  /** Sub-order id from import when one staging row produced several TMS orders. */
  importExternalOrderNo?: string | null;
  lines?: TmsOrderLine[];
  cargoItems?: TmsOrderCargo[];
  references?: TmsOrderReference[];
}

export interface PagedResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const tmsOrdersApi = {
  list: (page = 0, size = 20, impEntryNo?: number) =>
    api
      .get<PagedResult<TmsOrder>>('/tms-orders', {
        params: { page, size, ...(impEntryNo != null ? { impEntryNo } : {}) },
      })
      .then((r) => r.data),

  get: (orderNo: string) => api.get<TmsOrder>(`/tms-orders/${orderNo}`).then((r) => r.data),
};
