import api from './axios';

// ─── Child entity types ────────────────────────────────────────────────────

export interface ImportOrderLine {
  id?: number;
  entryNo: number;
  lineNo: number;
  /** Distinct values across lines can split processing into multiple TMS orders. */
  externalOrderNo?: string | null;
  uniqueReference?: string;
  original?: boolean;
  processed?: boolean;
  noPlanningRequired?: boolean;
  actionCode?: string;
  externalAddressNo?: string;
  addressName?: string;
  addressName2?: string;
  addressSearchName?: string;
  addressZoneName?: string;
  addressStreet?: string;
  addressNumber?: string;
  addressPostalCode?: string;
  addressCity?: string;
  addressCountryCode?: string;
  addressCityId?: number;
  regionNo?: string;
  masterRegionNo?: string;
  planningZoneNo?: string;
  planningZoneDescription?: string;
  planningZone?: string;
  orderBlock?: string;
  orderBlockId?: number;
  transportOrderNo?: string;
  planningSequence?: number;
  groupingId?: number;
  sortingKey?: number;
  noSeries?: string;
  initialDatetimeFrom?: string | null;
  initialDatetimeUntil?: string | null;
  initialDatetimeFrom2?: string | null;
  initialDatetimeUntil2?: string | null;
  bookedDatetimeFrom?: string | null;
  bookedDatetimeUntil?: string | null;
  requestedDatetimeFrom?: string | null;
  requestedDatetimeUntil?: string | null;
  closingDatetime?: string | null;
  latestDepartureHour?: string | null;
  latestHourOfDeparture?: string | null;
  timeFrom?: string | null;
  timeUntil?: string | null;
  plannedDatetimeOfPrevOl?: string | null;
  confirmedDatetime?: string | null;
  orderLineType?: string;
  boundCalculated?: boolean;
  plugIn?: boolean;
  transportMode?: string;
  transportModeType?: string;
  supplierNo?: string;
  bookingInfoValidation?: string;
  typeOfTime?: string;
  orderLineId?: number;
  planningBlockId?: number;
  haulierNo?: string;
  truckNo?: string;
  truckDescription?: string;
  driverNo?: string;
  driverFullName?: string;
  driverShortName?: string;
  coDriverNo?: string;
  coDriverFullName?: string;
  coDriverShortName?: string;
  confirmedBy?: string;
  selectedBy?: string;
  containerNo?: string;
  containerNo2?: string;
  containerNumber?: string;
  containerNumber2?: string;
  trailerNo?: string;
  trailerDescription?: string;
  chassisNo?: string;
  chassisDescription?: string;
  otherEquipmentNo?: string;
  otherEquipmentDescription?: string;
  splitPb?: string;
  equipmentTraction?: string;
  fleetNoTrailer?: string;
  registrationNoTrailer?: string;
  fleetNoChassis?: string;
  registrationNoChassis?: string;
  calculatedDistance?: number;
  calculatedDistanceTo?: number;
  mileage?: number;
  mileageDifference?: number;
  durationActualDifference?: number;
  calculatedDrivingTime?: string;
  calculatedDrivingTimeTo?: string;
  distanceId?: number;
  distanceIdTo?: number;
  latitude?: number;
  longitude?: number;
  shippingLineDiaryId?: number;
  slBookedForOl?: boolean;
  capacityDiaryId?: number;
  shippingCompanyNo?: string;
  shippingCompanyName?: string;
  shippingLineNo?: string;
  shippingLineName?: string;
  preferredShippingLine?: string;
  preBook?: boolean;
  isLate?: boolean;
  partOfOrder?: boolean;
  bookingChangeReason?: string;
  sentToHaulier?: boolean;
  needsRevision?: boolean;
  manualCalculated?: boolean;
  checkBooking?: boolean;
  bookingRequired?: boolean;
  needsRecalculate?: boolean;
  selected?: boolean;
  sentToBc?: boolean;
  cancelledByDriver?: boolean;
  loaded?: boolean;
  groupageBlock?: number;
  source?: string;
  externalOrderLineId?: string;
  orderLineRef1?: string;
  orderLineRef2?: string;
  salesResponsible?: string;
  custServResponsible?: string;
  office?: string;
  localOrderLineId?: number;
  communicationPartner?: string;
  importDatetime?: string | null;
  processedDatetime?: string | null;
  createdBy?: string;
  creationDatetime?: string | null;
  lastModifiedBy?: string;
  lastModificationDatetime?: string | null;
  orderLineCargos?: ImportOrderLineCargo[];
}

export interface ImportOrderLineCargo {
  id?: number;
  entryNo?: number;
  orderLineNo?: number;
  cargoLineNo?: number;
  externalGoodNo?: string;
  goodDescription?: string;
  goodType?: string;
  goodSubType?: string;
  quantity?: number;
  unitOfMeasure?: string;
  grossWeight?: number;
  netWeight?: number;
  volume?: number;
  length?: number;
  width?: number;
  height?: number;
  loadingMeters?: number;
  diameter?: number;
  tracingNumber1?: string;
  tracingNumber2?: string;
  adrType?: string;
  adrDangerousForEnvironment?: boolean;
  adrUnNo?: string;
  adrHazardClass?: string;
  adrPackingGroup?: string;
  adrTunnelRestrictionCode?: string;
  setTemperature?: boolean;
  temperature?: number;
  createdBy?: string;
  creationDatetime?: string | null;
  lastModifiedBy?: string;
  lastModificationDatetime?: string | null;
}

export interface ImportOrderCargo {
  id?: number;
  entryNo: number;
  lineNo: number;
  /** When set, links this cargo row to an import order line. */
  orderLineNo?: number | null;
  cargoNo?: number;
  externalOrderNo?: string;
  original?: boolean;
  processed?: boolean;
  actionType?: string;
  externalGoodNo?: string;
  externalGoodType?: string;
  externalGoodSubType?: string;
  description?: string;
  description2?: string | null;
  quantity?: number;
  grossWeight?: number;
  netWeight?: number;
  unitsPerParcel?: number;
  unitVolume?: number;
  unitOfMeasureCode?: string;
  unitOfMeasureDescription?: string;
  qtyPerUnitOfMeasure?: number;
  quantityBase?: number;
  volume?: number;
  loadingMeters?: number;
  palletPlaces?: number;
  forceLoadingMeters?: boolean;
  adrType?: string;
  adrDangerousForEnvironment?: boolean;
  adrUnNo?: string;
  adrHazardClass?: string;
  adrPackingGroup?: string;
  adrTunnelRestrictionCode?: string;
  imdgType?: string;
  dangerousGoods?: boolean;
  length?: number;
  width?: number;
  height?: number;
  diameter?: number;
  limitedQuantity?: boolean;
  tracingNo1?: string;
  tracingNo2?: string;
  temperature?: number;
  setTemperature?: boolean;
  minTemperature?: number;
  maxTemperature?: number;
  communicationPartner?: string;
  importDatetime?: string | null;
  processedDatetime?: string | null;
}

export interface ImportOrderEquipment {
  id?: number;
  entryNo: number;
  lineNo: number;
  externalOrderNo?: string;
  materialType?: string;
  equipmentTypeNo?: string;
  equipmentSubTypeNo?: string;
  customizedBoolean?: boolean;
  source?: string;
  remark?: string;
  tareWeight?: number;
  vgmWeight?: number;
  importDatetime?: string | null;
  processedDatetime?: string | null;
  communicationPartner?: string;
  externalOrderLineId?: string;
  orderLineNo?: number;
  cleaningInstruction?: string;
  createdBy?: string;
  creationDatetime?: string | null;
  lastModifiedBy?: string;
  lastModificationDatetime?: string | null;
}

export interface ImportOrderReference {
  id?: number;
  entryNo: number;
  lineNo: number;
  externalOrderNo?: string;
  referenceCode?: string;
  reference?: string;
  orderLineNo?: number;
  original?: boolean;
  processed?: boolean;
  globalReferenceNo?: number;
  searchReference?: string;
  customerNo?: string;
  remark?: string;
  requiredForInvoice?: boolean;
  printOnTransportOrder?: boolean;
  communicationPartner?: string;
}

export interface ImportOrderRemark {
  id?: number;
  entryNo: number;
  lineNo: number;
  externalOrderNo?: string;
  remarkType?: string;
  remarks?: string;
  externalRemarkCode?: string;
  communicationPartner?: string;
  orderLineNo?: number;
  externalOrderLineId?: string;
  importDatetime?: string | null;
  processedDatetime?: string | null;
  createdBy?: string;
  creationDatetime?: string | null;
  lastModifiedBy?: string;
  lastModificationDatetime?: string | null;
}

export interface ImportOrderCustomField {
  id?: number;
  entryNo: number;
  lineNo: number;
  fieldName?: string;
  fieldValue?: string;
  externalOrderNo?: string;
  communicationPartner?: string;
  createdBy?: string;
  creationDatetime?: string | null;
  lastModifiedBy?: string;
  lastModificationDatetime?: string | null;
}

export interface ImportTmsLink {
  importEntryNo: number;
  externalOrderNo?: string | null;
  tmsOrderNo?: string | null;
  tmsPartitionExternalOrderNo?: string | null;
}

// ─── Main ImportOrder (header) type ────────────────────────────────────────

export interface ImportOrder {
  entryNo: number;
  communicationPartner: string;
  externalOrderNo?: string;
  externalCustomerNo?: string;
  transactionType?: string;
  status: string;
  tmsOrderNo?: string | null;
  errorMessage?: string | null;

  // Order description
  orderDescription?: string;
  description2?: string;
  orderDate?: string | null;
  collectionDate?: string | null;
  deliveryDate?: string | null;
  firstOrderLineDate?: string | null;

  // Customer / Sell-to
  customerName?: string;
  customerName2?: string;
  customerSearchName?: string;
  sellToAddress?: string;
  sellToAddress2?: string;
  sellToCity?: string;
  sellToContact?: string;
  sellToPostCode?: string;
  sellToCounty?: string;
  sellToCountryRegionCode?: string;
  vatRegistrationNo?: string;
  boundName?: string;

  // Bill-to
  billToCustomerNo?: string;
  billToName?: string;
  billToName2?: string;
  billToAddress?: string;
  billToAddress2?: string;
  billToCity?: string;
  billToContact?: string;
  billToPostCode?: string;
  billToCounty?: string;
  billToCountryRegionCode?: string;

  // IDs / references
  noSeries?: string;
  forwardingOrderNo?: string;
  importFileEntryNo?: number;
  shortcutReference1Code?: string;
  shortcutReference2Code?: string;
  shortcutReference3Code?: string;
  bookingReference?: string;
  overruleCustRefDuplicate?: boolean;

  // Transport
  transportType?: string;
  tradeLaneNo?: string;
  transitTimeNo?: string;
  tripTypeNo?: string;
  office?: string;
  salesResponsible?: string;
  custServResponsible?: string;
  carrierNo?: string;
  carrierName?: string;
  webPortalUser?: string;
  traction_order?: string;
  source?: string;
  userId?: string;

  // Vessel / container / port
  vesselNameImport?: string;
  vesselNameExport?: string;
  vesselName?: string;
  vesselEta?: string | null;
  vesselEtd?: string | null;
  originPortName?: string;
  destinationPortName?: string;
  originInfo?: string;
  destinationInfo?: string;
  sealNo?: string;
  countryOfOrigin?: string;
  countryOfDestination?: string;

  // Container info
  containerNumber?: string;
  containerType?: string;
  containerTypeIsoCode?: string;
  carrierId?: string;
  sealNumber?: string;
  importOrExport?: string;
  pickupPincode?: string;
  pickupReference?: string;
  dropoffPincode?: string;
  dropoffReference?: string;
  containerCancelled?: boolean;

  // Extended vessel info
  closingDateTime?: string | null;
  depotOutFromDateTime?: string | null;
  depotInFromDateTime?: string | null;
  vgmClosingDateTime?: string | null;
  vgmWeight?: number;
  originCountry?: string;
  destinationCountry?: string;

  // Tariff / financials
  properTariff?: boolean;
  tariffNo?: string;
  tariffId?: string;
  multipleTariff?: boolean;
  specialTariffUnitCost?: number;
  executionTime?: number;
  cashOnDeliveryType?: string;
  cashOnDeliveryAmount?: number;

  // Cargo totals
  totalGrossWeight?: number;
  totalNetWeight?: number;
  temperature?: number;
  recalcDistance?: boolean;
  distance?: number;
  duration?: number;

  // Logistics flags
  neutralShipment?: boolean;
  nsAddName?: string;
  nsAddStreet?: string;
  nsAddCityPc?: string;
  shippingRequired?: boolean;
  roadTransportOrders?: string;
  shippingTransportOrder?: string;
  linkedTrucks?: string;
  linkedDriversCoDrivers?: string;
  linkedTrailersContainers?: string;
  tradelaneEqualsOrder?: boolean;

  // ATA/ATD/ETA/ETD
  ata?: string | null;
  atd?: string | null;
  eta?: string | null;
  etd?: string | null;
  daysOfDemurrage?: number;
  daysOfDetention?: number;
  daysOfStorage?: number;

  // Timestamps
  receivedAt?: string;
  processedAt?: string | null;
  createdBy?: string;
  lastModifiedBy?: string;
  lastModificationDateTime?: string | null;

  // Child relations
  lines?: ImportOrderLine[];
  cargoItems?: ImportOrderCargo[];
  references?: ImportOrderReference[];
  orderEquipments?: ImportOrderEquipment[];
  orderRemarks?: ImportOrderRemark[];
  orderCustomFields?: ImportOrderCustomField[];

  /** TMS orders created for this import entry (multiple when line/cargo external order ids differ). */
  tmsOrdersForThisEntry?: ImportTmsLink[];
  /** Other import rows from the same EDI XML file (batch). */
  importsFromSameFile?: ImportTmsLink[];
}

export interface PagedResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ImportStats {
  received: number;
  processing: number;
  processed: number;
  error: number;
  total?: number;
}

// ─── API calls ─────────────────────────────────────────────────────────────

export const importOrdersApi = {
  list: (page = 0, size = 20, search?: string) =>
    api
      .get<PagedResult<ImportOrder>>('/import-orders', { params: { page, size, search } })
      .then((r) => r.data),

  get: (entryNo: number) =>
    api.get<ImportOrder>(`/import-orders/${entryNo}`).then((r) => r.data),

  create: (order: Partial<ImportOrder>) =>
    api.post<ImportOrder>('/import-orders', order).then((r) => r.data),

  update: (entryNo: number, order: Partial<ImportOrder>) =>
    api.put<ImportOrder>(`/import-orders/${entryNo}`, order).then((r) => r.data),

  delete: (entryNo: number) =>
    api.delete(`/import-orders/${entryNo}`).then((r) => r.data),

  process: (entryNo: number) =>
    api
      .post<{ tmsOrderNo: string; tmsOrderNos: string[]; message: string }>(`/import-orders/${entryNo}/process`)
      .then((r) => r.data),

  validate: (entryNo: number) =>
    api
      .post<{ valid: boolean; errors: string[] }>(`/import-orders/${entryNo}/validate`)
      .then((r) => r.data),

  processAll: () =>
    api.post<{ processedCount: number }>('/import-orders/process-all').then((r) => r.data),

  getLogs: (entryNo: number) =>
    api.get<any[]>(`/import-orders/${entryNo}/logs`).then((r) => r.data),

  getStats: () =>
    api.get<ImportStats>('/import-orders/stats').then((r) => r.data),

  getTransportTypes: (communicationPartner: string) =>
    api
      .get<string[]>('/import-orders/transport-types', { params: { communicationPartner } })
      .then((r) => r.data),
};
