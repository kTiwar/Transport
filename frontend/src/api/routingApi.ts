import api from './axios';

export type RoutingOrder = {
  id: number;
  publicOrderId: string | null;
  pickupAddress: string;
  pickupPostcode?: string | null;
  deliveryAddress: string;
  deliveryPostcode?: string | null;
  weightKg: number;
  volumeM3?: number;
  timeWindowStart?: string | null;
  timeWindowEnd?: string | null;
  status: string;
  tmsOrderNo?: string | null;
  createdAt?: string | null;
};

export type RoutingVehicle = {
  vehicleId: number;
  code: string;
  capacityWeightKg: number;
  active: boolean | null;
};

export type RoutingStop = {
  stopId: number;
  sequenceNumber: number;
  stopType: string;
  orderId: number | null;
  latitude: number | null;
  longitude: number | null;
  arrivalTime?: string | null;
  departureTime?: string | null;
};

export type RoutingRoute = {
  routeId: number;
  vehicleId: number;
  vehicleCode: string;
  routeDate: string;
  totalDistanceM: number | null;
  totalDurationS: number | null;
  status: string;
  optimizerRunId: string | null;
  stops: RoutingStop[];
};

export type OptimizePayload = {
  depotLatitude?: number;
  depotLongitude?: number;
  routeDate: string;
  orderIds: number[];
  vehicleIds: number[];
};

export const routingApi = {
  listOrders: () => api.get<RoutingOrder[]>('/routing/orders').then((r) => r.data),
  listVehicles: () => api.get<RoutingVehicle[]>('/routing/vehicles').then((r) => r.data),
  listRoutes: () => api.get<RoutingRoute[]>('/routing/routes').then((r) => r.data),
  getRoute: (id: number) => api.get<RoutingRoute>(`/routing/routes/${id}`).then((r) => r.data),
  optimize: (body: OptimizePayload) =>
    api.post<RoutingRoute[]>('/routing/routes/optimize', body).then((r) => r.data),
  importFromTms: (orderNo: string) =>
    api.post<RoutingOrder>(`/routing/orders/from-tms/${encodeURIComponent(orderNo)}`).then((r) => r.data),

  importFromImportOrder: (entryNo: number) =>
    api.post<RoutingOrder[]>(`/routing/orders/from-import/${entryNo}`).then((r) => r.data),

  deleteOrder: (id: number) =>
    api.delete(`/routing/orders/${id}`).then((r) => r.data),
};
