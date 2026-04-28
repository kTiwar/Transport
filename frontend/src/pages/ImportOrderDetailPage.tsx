import { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate, useSearchParams, Link } from 'react-router-dom';
import {
  importOrdersApi,
  ImportOrder,
  ImportOrderLine,
  ImportOrderCargo,
  ImportOrderCustomField,
  ImportOrderRemark,
} from '../api/importOrders';
import {
  ArrowLeft, Play, Edit, Trash2,
  CheckCircle2, AlertTriangle, Loader2,
  Eye, EyeOff, X, ChevronRight,
} from 'lucide-react';

// ── Helpers ────────────────────────────────────────────────────────────────

const STATUS_COLOR: Record<string, string> = {
  RECEIVED: '#00d4ff', READY_TO_PROCESS: '#00d4ff', PENDING: '#f59e0b',
  PROCESSING: '#8b5cf6', PROCESSED: '#10b981', ERROR: '#ef4444',
  VOID: '#6b7280', SKIPPED: '#6b7280',
};

const HIDDEN_FIELDS = new Set([
  'shortcutReference3Code', 'description2', 'tripTypeNo', 'collectionDate',
  'deliveryDate', 'office', 'salesResponsible', 'countryOfOrigin',
  'countryOfDestination', 'neutralShipment', 'nsAddName', 'nsAddStreet',
  'nsAddCityPc', 'cashOnDeliveryType', 'cashOnDeliveryAmount', 'userId',
  'carrierNo', 'vesselNameImport', 'vesselNameExport', 'custServResponsible',
  'tractionOrder', 'ata', 'atd', 'eta', 'etd',
  'daysOfDemurrage', 'daysOfDetention', 'daysOfStorage',
]);

const fmtDate = (v?: string | null) => {
  if (!v) return '—';
  try { return new Date(v).toLocaleString(); } catch { return v; }
};

const fmtBool = (v?: boolean | null) =>
  v === true ? <span style={{ color: '#10b981', fontWeight: 700 }}>Yes</span>
    : v === false ? <span style={{ color: 'var(--muted)' }}>No</span>
    : '—';

const fmtNum = (v?: number | null) => (v == null || Number.isNaN(Number(v)) ? '—' : String(v));

/** Active `partition` query value (external id / sub-order); legacy `?partition=`. */
function normalizePartitionQuery(raw: string | null): string | null {
  if (raw == null || raw === '') return null;
  try {
    return decodeURIComponent(raw);
  } catch {
    return raw;
  }
}

/** Path segment `/partition/:partitionKey` (may be encoded). */
function partitionFromPathParam(raw: string | undefined): string | null {
  if (raw == null || raw === '') return null;
  try {
    return decodeURIComponent(raw);
  } catch {
    return raw;
  }
}

function partitionListForImport(order: ImportOrder): string[] {
  const s = new Set<string>();
  for (const l of order.lines ?? []) {
    const v = (l.externalOrderNo ?? '').trim();
    if (v) s.add(v);
  }
  for (const t of order.tmsOrdersForThisEntry ?? []) {
    const v = (t.tmsPartitionExternalOrderNo ?? t.externalOrderNo ?? '').trim();
    if (v) s.add(v);
  }
  if (s.size > 0) return [...s].sort();
  const fallback = (order.externalOrderNo ?? '').trim();
  return [fallback || '—'];
}

function lineMatchesPartition(line: ImportOrderLine, order: ImportOrder, partition: string | null): boolean {
  if (!partition) return true;
  const lo = (line.externalOrderNo ?? '').trim();
  const ho = (order.externalOrderNo ?? '').trim();
  if (partition === '—') return !lo;
  if (lo) return lo === partition;
  return ho === partition;
}

type ImportPartitionRow = {
  externalOrderNo: string;
  createDate?: string | null;
  status: string;
  entryNo: number;
  tmsOrderNo?: string | null;
};

function buildPartitionRows(order: ImportOrder): ImportPartitionRow[] {
  const parts = partitionListForImport(order);
  const lines = order.lines ?? [];
  const tmsList = order.tmsOrdersForThisEntry ?? [];
  return parts.map((ext) => {
    const extNorm = ext === '—' ? '' : ext;
    const linesInPart = lines.filter((l) => lineMatchesPartition(l, order, ext));
    const dates = linesInPart.map((l) => l.creationDatetime).filter(Boolean) as string[];
    let createDate: string | null | undefined = dates.length
      ? dates.reduce((a, b) => (new Date(a) <= new Date(b) ? a : b))
      : order.receivedAt;
    const link =
      extNorm === ''
        ? undefined
        : tmsList.find(
            (t) =>
              (t.tmsPartitionExternalOrderNo ?? '').trim() === extNorm
              || (t.externalOrderNo ?? '').trim() === extNorm,
          );
    const tmsOrderNo = link?.tmsOrderNo ?? null;
    const status = tmsOrderNo ? `${order.status} · TMS ${tmsOrderNo}` : order.status;
    return {
      externalOrderNo: ext,
      createDate,
      status,
      entryNo: order.entryNo,
      tmsOrderNo,
    };
  });
}

/** Header-level cargo rows linked to an import order line (uses order_line_no when present). */
function cargoRowsForOrderLine(
  order: ImportOrder,
  line: ImportOrderLine,
  cargoSource?: ImportOrderCargo[] | null,
): ImportOrderCargo[] {
  const items = cargoSource ?? order.cargoItems ?? [];
  const ln = Number(line.lineNo);
  return items.filter((c) => {
    if (c.orderLineNo != null && c.orderLineNo !== undefined && !Number.isNaN(Number(c.orderLineNo))) {
      return Number(c.orderLineNo) === ln;
    }
    return Number(c.lineNo) === ln;
  });
}

function customFieldsForOrderLine(
  order: ImportOrder,
  line: ImportOrderLine,
  fieldsSource?: ImportOrderCustomField[] | null,
): ImportOrderCustomField[] {
  const rows = fieldsSource ?? order.orderCustomFields ?? [];
  const ln = Number(line.lineNo);
  return rows.filter((f) => Number(f.lineNo) === ln);
}

function truckInfoLineCount(order: ImportOrder, linesSubset?: ImportOrderLine[]): number {
  const lines = linesSubset ?? order.lines ?? [];
  return lines.filter((l) =>
    !!(l.truckNo || l.truckDescription || l.driverNo || l.driverFullName || l.chassisNo || l.fleetNoChassis || l.registrationNoChassis),
  ).length;
}

function trailerInfoLineCount(order: ImportOrder, linesSubset?: ImportOrderLine[]): number {
  const lines = linesSubset ?? order.lines ?? [];
  return lines.filter((l) =>
    !!(l.trailerNo || l.trailerDescription || l.fleetNoTrailer || l.registrationNoTrailer
      || l.containerNo || l.containerNumber || l.containerNo2 || l.containerNumber2),
  ).length;
}

// ── Sub-components ─────────────────────────────────────────────────────────

function Field({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div style={{ padding: '10px 0', borderBottom: '1px solid var(--border)' }}>
      <div style={{ fontSize: 10, color: 'var(--muted)', textTransform: 'uppercase', letterSpacing: '.5px', marginBottom: 3 }}>{label}</div>
      <div style={{ fontSize: 12, color: 'var(--text)' }}>{value ?? '—'}</div>
    </div>
  );
}

function FieldGrid({ children }: { children: React.ReactNode }) {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))', gap: '0 24px' }}>
      {children}
    </div>
  );
}

function SectionTitle({ title }: { title: string }) {
  return (
    <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--cyan)', textTransform: 'uppercase', letterSpacing: '.8px', marginTop: 20, marginBottom: 4 }}>
      {title}
    </div>
  );
}

// A generic table for child records
function RecordTable({ columns, rows, onRowClick }: {
  columns: { key: string; label: string; render?: (v: any, row: any) => React.ReactNode }[];
  rows: any[];
  onRowClick?: (row: any) => void;
}) {
  if (!rows || rows.length === 0) {
    return (
      <div style={{ padding: '32px', textAlign: 'center', color: 'var(--muted)', fontSize: 13 }}>
        No records found.
      </div>
    );
  }
  return (
    <div style={{ overflowX: 'auto' }}>
      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
        <thead>
          <tr style={{ background: 'var(--bg3)', borderBottom: '1px solid var(--border)' }}>
            {columns.map(c => (
              <th key={c.key} style={{ padding: '9px 12px', textAlign: 'left', color: 'var(--muted)', fontWeight: 700, fontSize: 10, textTransform: 'uppercase', letterSpacing: '.5px', whiteSpace: 'nowrap' }}>
                {c.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr key={i}
              onClick={() => onRowClick?.(row)}
              style={{ borderBottom: '1px solid var(--border)', cursor: onRowClick ? 'pointer' : 'default', transition: 'background .1s' }}
              onMouseEnter={e => onRowClick && (e.currentTarget.style.background = 'var(--bg3)')}
              onMouseLeave={e => onRowClick && (e.currentTarget.style.background = 'transparent')}>
              {columns.map(c => (
                <td key={c.key} style={{ padding: '8px 12px', color: 'var(--text)' }}>
                  {c.render ? c.render(row[c.key], row) : (row[c.key] ?? '—')}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

// ── Main Component ─────────────────────────────────────────────────────────

const TABS = [
  'General', 'Customer', 'Bill-to', 'Transport', 'Dates', 'Additional',
  'Container Info', 'Vessel Info', 'Equipments', 'Truck Info', 'Trailer Info', 'Remarks', 'Custom Fields', 'Order Lines',
];

export default function ImportOrderDetailPage() {
  const { entryNo, partitionKey } = useParams<{ entryNo: string; partitionKey?: string }>();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const activePartition =
    partitionFromPathParam(partitionKey) ?? normalizePartitionQuery(searchParams.get('partition'));

  const [order, setOrder]           = useState<ImportOrder | null>(null);
  const [logs, setLogs]             = useState<any[]>([]);
  const [loading, setLoading]       = useState(true);
  const [validating, setValid]      = useState(false);
  const [processing, setProc]       = useState(false);
  const [validErrors, setValErr]    = useState<string[] | null>(null);
  const [toast, setToast]           = useState<{ msg: string; ok: boolean } | null>(null);
  const [activeTab, setActiveTab]   = useState(0);
  const [showHidden, setShowHidden] = useState(false);

  // Order line dialog
  const [selectedLine, setSelectedLine]       = useState<ImportOrderLine | null>(null);
  const [lineTab, setLineTab]                 = useState(0);
  const [selectedOrderCargo, setSelectedOrderCargo] = useState<ImportOrderCargo | null>(null);
  const [customFieldDetail, setCustomFieldDetail] = useState<ImportOrderCustomField | null>(null);
  const [truckLineDetail, setTruckLineDetail] = useState<ImportOrderLine | null>(null);
  const [trailerLineDetail, setTrailerLineDetail] = useState<ImportOrderLine | null>(null);
  const [remarkDetail, setRemarkDetail] = useState<ImportOrderRemark | null>(null);

  const filteredLines = useMemo(() => {
    if (!order) return [];
    return (order.lines ?? []).filter((l) => lineMatchesPartition(l, order, activePartition));
  }, [order, activePartition]);

  const lineNoSet = useMemo(
    () => new Set(filteredLines.map((l) => Number(l.lineNo)).filter((n) => !Number.isNaN(n))),
    [filteredLines],
  );

  const filteredCargoItems = useMemo(() => {
    if (!order) return [];
    const all = order.cargoItems ?? [];
    if (!activePartition) return all;
    const parts = partitionListForImport(order);
    return all.filter((c) => {
      const ce = (c.externalOrderNo ?? '').trim();
      if (ce && ce === activePartition) return true;
      if (c.orderLineNo != null && lineNoSet.has(Number(c.orderLineNo))) return true;
      if (
        (c.orderLineNo == null || Number(c.orderLineNo) === 0)
        && !ce
        && parts.length === 1
        && parts[0] === activePartition
      ) {
        return true;
      }
      return false;
    });
  }, [order, activePartition, lineNoSet]);

  const filteredEquipments = useMemo(() => {
    if (!order) return [];
    const all = order.orderEquipments ?? [];
    if (!activePartition) return all;
    return all.filter((e) => {
      const ex = (e.externalOrderNo ?? '').trim();
      if (ex && ex === activePartition) return true;
      if (e.orderLineNo != null && lineNoSet.has(Number(e.orderLineNo))) return true;
      return false;
    });
  }, [order, activePartition, lineNoSet]);

  const filteredRemarks = useMemo(() => {
    if (!order) return [];
    const all = order.orderRemarks ?? [];
    if (!activePartition) return all;
    return all.filter((r) => {
      const ex = (r.externalOrderNo ?? '').trim();
      if (ex && ex === activePartition) return true;
      if (r.orderLineNo != null && lineNoSet.has(Number(r.orderLineNo))) return true;
      return false;
    });
  }, [order, activePartition, lineNoSet]);

  const filteredCustomFields = useMemo(() => {
    if (!order) return [];
    const all = order.orderCustomFields ?? [];
    if (!activePartition) return all;
    return all.filter((f) => {
      const ln = Number(f.lineNo);
      if (ln === 0) return true;
      if (lineNoSet.has(ln)) return true;
      const ex = (f.externalOrderNo ?? '').trim();
      return !!(ex && ex === activePartition);
    });
  }, [order, activePartition, lineNoSet]);

  const partitionRows = useMemo(() => (order ? buildPartitionRows(order) : []), [order]);

  const shouldShow = (field: string) => showHidden || !HIDDEN_FIELDS.has(field);

  const showToast = (msg: string, ok = true) => {
    setToast({ msg, ok });
    setTimeout(() => setToast(null), 3000);
  };

  useEffect(() => {
    if (!entryNo) return;
    const id = parseInt(entryNo);
    Promise.all([importOrdersApi.get(id), importOrdersApi.getLogs(id)])
      .then(([o, l]) => { setOrder(o); setLogs(l); })
      .finally(() => setLoading(false));
  }, [entryNo]);

  const openPartitionDetail = (externalKey: string) => {
    if (!entryNo) return;
    navigate(`/import-orders/${entryNo}/partition/${encodeURIComponent(externalKey)}`);
  };

  const clearPartitionFilter = () => {
    if (!entryNo) return;
    navigate(`/import-orders/${entryNo}`, { replace: true });
  };

  const handleValidate = async () => {
    if (!order) return;
    setValid(true);
    try {
      const r = await importOrdersApi.validate(order.entryNo);
      setValErr(r.errors);
      showToast(r.valid ? '✓ Validation passed' : `✗ ${r.errors.length} error(s)`, r.valid);
    } finally { setValid(false); }
  };

  const handleProcess = async () => {
    if (!order) return;
    setProc(true);
    try {
      const r = await importOrdersApi.process(order.entryNo);
      const updated = await importOrdersApi.get(order.entryNo);
      setOrder(updated);
      const nos = r.tmsOrderNos?.length ? r.tmsOrderNos : r.tmsOrderNo ? [r.tmsOrderNo] : [];
      if (nos.length === 0) showToast('✓ Processed', true);
      else if (nos.length === 1) showToast(`✓ Processed → TMS ${nos[0]}`, true);
      else showToast(`✓ Processed → ${nos.length} TMS orders: ${nos.join(', ')}`, true);
    } catch (e: any) {
      showToast(e?.response?.data?.error || 'Failed', false);
    } finally { setProc(false); }
  };

  if (loading) return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 300 }}>
      <Loader2 size={28} color="var(--cyan)" />
    </div>
  );

  if (!order) return (
    <div style={{ padding: 32, color: 'var(--muted)' }}>Order not found.</div>
  );

  const statusColor = STATUS_COLOR[order.status] || 'var(--text2)';

  // ── Tab renderers ──────────────────────────────────────────────────────

  const renderGeneral = () => (
    <FieldGrid>
      <Field label="Entry No." value={<span style={{ fontFamily: 'monospace' }}>{order.entryNo}</span>} />
      <Field label="Communication Partner" value={<span style={{ color: 'var(--cyan)' }}>{order.communicationPartner}</span>} />
      <Field label="External Order No." value={<span style={{ fontFamily: 'monospace' }}>{order.externalOrderNo}</span>} />
      <Field label="Transaction Type" value={order.transactionType} />
      <Field label="Status" value={
        <span style={{ color: statusColor, background: `${statusColor}18`, border: `1px solid ${statusColor}40`, borderRadius: 4, padding: '2px 8px', fontSize: 11, fontWeight: 700 }}>
          {order.status}
        </span>
      } />
      <Field label="TMS Order No. (primary)" value={<span style={{ color: '#10b981', fontFamily: 'monospace' }}>{order.tmsOrderNo || '—'}</span>} />
      {order.importFileEntryNo != null && order.importsFromSameFile && order.importsFromSameFile.length > 0 && (
        <Field
          label={`Imports from same XML file (#${order.importFileEntryNo})`}
          value={
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              {order.importsFromSameFile.map((x, i) => (
                <div key={i} style={{ fontSize: 11 }}>
                  <span style={{ color: 'var(--cyan)', fontFamily: 'monospace' }}>Imp {x.importEntryNo}</span>
                  {x.externalOrderNo ? <span style={{ marginLeft: 8, color: 'var(--text2)' }}>{x.externalOrderNo}</span> : null}
                  {x.tmsOrderNo ? (
                    <span style={{ marginLeft: 8, color: '#10b981', fontFamily: 'monospace' }}>→ {x.tmsOrderNo}</span>
                  ) : (
                    <span style={{ marginLeft: 8, color: 'var(--muted)' }}>—</span>
                  )}
                </div>
              ))}
            </div>
          }
        />
      )}
      <Field label="Customer Name" value={order.customerName} />
      <Field label="Transport Type" value={order.transportType} />
      <Field label="Total Gross Weight" value={order.totalGrossWeight} />
      <Field label="Total Net Weight" value={order.totalNetWeight} />
      {shouldShow('office') && <Field label="Office" value={order.office} />}
      {shouldShow('salesResponsible') && <Field label="Sales Responsible" value={order.salesResponsible} />}
      {shouldShow('custServResponsible') && <Field label="Cust. Serv. Responsible" value={order.custServResponsible} />}
      <Field label="Shortcut Reference 1" value={order.shortcutReference1Code} />
      <Field label="Shortcut Reference 2" value={order.shortcutReference2Code} />
      {shouldShow('shortcutReference3Code') && <Field label="Shortcut Reference 3" value={order.shortcutReference3Code} />}
      <Field label="Order Description" value={order.orderDescription} />
      {shouldShow('description2') && <Field label="Description 2" value={order.description2} />}
      {shouldShow('tripTypeNo') && <Field label="Trip Type No." value={order.tripTypeNo} />}
      <Field label="Order Date" value={fmtDate(order.orderDate)} />
      {shouldShow('collectionDate') && <Field label="Collection Date" value={fmtDate(order.collectionDate)} />}
      {shouldShow('deliveryDate') && <Field label="Delivery Date" value={fmtDate(order.deliveryDate)} />}
      {shouldShow('neutralShipment') && <Field label="Neutral Shipment" value={fmtBool(order.neutralShipment)} />}
      {shouldShow('nsAddName') && <Field label="NS Add Name" value={order.nsAddName} />}
      {shouldShow('nsAddStreet') && <Field label="NS Add Street" value={order.nsAddStreet} />}
      {shouldShow('nsAddCityPc') && <Field label="NS Add CityPC" value={order.nsAddCityPc} />}
      {shouldShow('cashOnDeliveryType') && <Field label="Cash on Delivery Type" value={order.cashOnDeliveryType} />}
      {shouldShow('cashOnDeliveryAmount') && <Field label="Cash on Delivery Amount" value={order.cashOnDeliveryAmount} />}
      <Field label="Import Date" value={fmtDate(order.receivedAt)} />
      <Field label="Processed Date" value={fmtDate(order.processedAt)} />
      <Field label="Error Message" value={order.errorMessage ? <span style={{ color: '#ef4444' }}>{order.errorMessage}</span> : '—'} />
      {shouldShow('userId') && <Field label="User ID" value={order.userId} />}
      {shouldShow('carrierNo') && <Field label="Carrier No." value={order.carrierNo} />}
      <Field label="Carrier Name" value={order.carrierName} />
      {shouldShow('vesselNameImport') && <Field label="Vessel Name (Import)" value={order.vesselNameImport} />}
      {shouldShow('vesselNameExport') && <Field label="Vessel Name (Export)" value={order.vesselNameExport} />}
      {shouldShow('tractionOrder') && <Field label="Traction Order" value={order.traction_order} />}
      <Field label="Web Portal User" value={order.webPortalUser} />
    </FieldGrid>
  );

  const renderCustomer = () => (
    <FieldGrid>
      <Field label="Customer Name" value={order.customerName} />
      <Field label="Customer Name 2" value={order.customerName2} />
      <Field label="External Customer No." value={order.externalCustomerNo} />
      <Field label="Customer Search Name" value={order.customerSearchName} />
      <Field label="Sell-to Address" value={order.sellToAddress} />
      <Field label="Sell-to Address 2" value={order.sellToAddress2} />
      <Field label="Sell-to City" value={order.sellToCity} />
      <Field label="Sell-to Contact" value={order.sellToContact} />
      <Field label="Sell-to Post Code" value={order.sellToPostCode} />
      <Field label="Sell-to County" value={order.sellToCounty} />
      <Field label="Sell-to Country/Region Code" value={order.sellToCountryRegionCode} />
      <Field label="VAT Registration No." value={order.vatRegistrationNo} />
      <Field label="Bound Name" value={order.boundName} />
    </FieldGrid>
  );

  const renderBillTo = () => (
    <FieldGrid>
      <Field label="Bill-to Customer No." value={order.billToCustomerNo} />
      <Field label="Bill-to Name" value={order.billToName} />
      <Field label="Bill-to Name 2" value={order.billToName2} />
      <Field label="Bill-to Address" value={order.billToAddress} />
      <Field label="Bill-to Address 2" value={order.billToAddress2} />
      <Field label="Bill-to City" value={order.billToCity} />
      <Field label="Bill-to Contact" value={order.billToContact} />
      <Field label="Bill-to Post Code" value={order.billToPostCode} />
      <Field label="Bill-to County" value={order.billToCounty} />
      <Field label="Bill-to Country/Region Code" value={order.billToCountryRegionCode} />
    </FieldGrid>
  );

  const renderTransport = () => (
    <FieldGrid>
      <Field label="Transport Type" value={order.transportType} />
      <Field label="Trade Lane No." value={order.tradeLaneNo} />
      <Field label="Transit Time No." value={order.transitTimeNo} />
      {shouldShow('tripTypeNo') && <Field label="Trip Type No." value={order.tripTypeNo} />}
      {shouldShow('carrierNo') && <Field label="Carrier No." value={order.carrierNo} />}
      <Field label="Carrier Name" value={order.carrierName} />
      {shouldShow('vesselNameImport') && <Field label="Vessel Name (Import)" value={order.vesselNameImport} />}
      {shouldShow('vesselNameExport') && <Field label="Vessel Name (Export)" value={order.vesselNameExport} />}
      <Field label="Origin Port Name" value={order.originPortName} />
      <Field label="Destination Port Name" value={order.destinationPortName} />
      <Field label="Vessel ETA" value={fmtDate(order.vesselEta)} />
      <Field label="Vessel ETD" value={fmtDate(order.vesselEtd)} />
      <Field label="Seal No." value={order.sealNo} />
      {shouldShow('tractionOrder') && <Field label="Traction Order" value={order.traction_order} />}
      {shouldShow('countryOfOrigin') && <Field label="Country Of Origin" value={order.countryOfOrigin} />}
      {shouldShow('countryOfDestination') && <Field label="Country Of Destination" value={order.countryOfDestination} />}
      <Field label="Origin Info" value={order.originInfo} />
      <Field label="Destination Info" value={order.destinationInfo} />
    </FieldGrid>
  );

  const renderDates = () => (
    <FieldGrid>
      <Field label="Order Date" value={fmtDate(order.orderDate)} />
      {shouldShow('collectionDate') && <Field label="Collection Date" value={fmtDate(order.collectionDate)} />}
      {shouldShow('deliveryDate') && <Field label="Delivery Date" value={fmtDate(order.deliveryDate)} />}
      <Field label="First Order Line Date" value={fmtDate(order.firstOrderLineDate)} />
      <Field label="Import Date" value={fmtDate(order.receivedAt)} />
      <Field label="Processed Date" value={fmtDate(order.processedAt)} />
      <Field label="Last Modification" value={fmtDate(order.lastModificationDateTime)} />
      {shouldShow('ata') && <Field label="ATA" value={fmtDate(order.ata)} />}
      {shouldShow('atd') && <Field label="ATD" value={fmtDate(order.atd)} />}
      {shouldShow('eta') && <Field label="ETA" value={fmtDate(order.eta)} />}
      {shouldShow('etd') && <Field label="ETD" value={fmtDate(order.etd)} />}
    </FieldGrid>
  );

  const renderAdditional = () => (
    <FieldGrid>
      <Field label="No. Series" value={order.noSeries} />
      <Field label="Forwarding Order No." value={order.forwardingOrderNo} />
      {shouldShow('office') && <Field label="Office" value={order.office} />}
      {shouldShow('salesResponsible') && <Field label="Sales Responsible" value={order.salesResponsible} />}
      {shouldShow('custServResponsible') && <Field label="Cust. Serv. Responsible" value={order.custServResponsible} />}
      <Field label="Proper Tariff" value={fmtBool(order.properTariff)} />
      <Field label="Tariff No." value={order.tariffNo} />
      <Field label="Tariff ID" value={order.tariffId} />
      <Field label="Multiple Tariff" value={fmtBool(order.multipleTariff)} />
      <Field label="Execution Time" value={order.executionTime} />
      <Field label="Temperature" value={order.temperature} />
      <Field label="Recalc Distance" value={fmtBool(order.recalcDistance)} />
      <Field label="Distance" value={order.distance} />
      <Field label="Duration" value={order.duration} />
      <Field label="Shipping Required" value={fmtBool(order.shippingRequired)} />
      <Field label="Road Transport Orders" value={order.roadTransportOrders} />
      <Field label="Shipping Transport Order" value={order.shippingTransportOrder} />
      <Field label="Linked Trucks" value={order.linkedTrucks} />
      <Field label="Linked Drivers/Co-Drivers" value={order.linkedDriversCoDrivers} />
      <Field label="Linked Trailers/Containers" value={order.linkedTrailersContainers} />
      <Field label="Tradelane = Order" value={fmtBool(order.tradelaneEqualsOrder)} />
      <Field label="Source" value={order.source} />
      <Field label="Overrule Cust Ref Duplicate" value={fmtBool(order.overruleCustRefDuplicate)} />
      <Field label="Web Portal User" value={order.webPortalUser} />
      <Field label="Created By" value={order.createdBy} />
      <Field label="Last Modified By" value={order.lastModifiedBy} />
      {shouldShow('daysOfDemurrage') && <Field label="Days Of Demurrage" value={order.daysOfDemurrage} />}
      {shouldShow('daysOfDetention') && <Field label="Days Of Detention" value={order.daysOfDetention} />}
      {shouldShow('daysOfStorage') && <Field label="Days Of Storage" value={order.daysOfStorage} />}
    </FieldGrid>
  );

  const renderContainerInfo = () => (
    <FieldGrid>
      <Field label="Carrier Name" value={order.carrierName} />
      <Field label="Container Number" value={order.containerNumber} />
      <Field label="Container Type" value={order.containerType} />
      <Field label="Container Type ISO Code" value={order.containerTypeIsoCode} />
      <Field label="Carrier ID" value={order.carrierId} />
      <Field label="Seal Number" value={order.sealNumber} />
      <Field label="Import or Export" value={order.importOrExport} />
      <Field label="Pickup Pincode" value={order.pickupPincode} />
      <Field label="Pickup Reference" value={order.pickupReference} />
      <Field label="Dropoff Pincode" value={order.dropoffPincode} />
      <Field label="Dropoff Reference" value={order.dropoffReference} />
      <Field label="Container Cancelled" value={fmtBool(order.containerCancelled)} />
    </FieldGrid>
  );

  const renderVesselInfo = () => (
    <FieldGrid>
      <Field label="Origin Info" value={order.originInfo} />
      <Field label="Destination Info" value={order.destinationInfo} />
      <Field label="Vessel Name" value={order.vesselName} />
      <Field label="ETA" value={fmtDate(order.vesselEta)} />
      <Field label="ETD" value={fmtDate(order.vesselEtd)} />
      <Field label="Closing DateTime" value={fmtDate(order.closingDateTime)} />
      <Field label="Depot Out From DateTime" value={fmtDate(order.depotOutFromDateTime)} />
      <Field label="Depot In From DateTime" value={fmtDate(order.depotInFromDateTime)} />
      <Field label="VGM Closing DateTime" value={fmtDate(order.vgmClosingDateTime)} />
      <Field label="VGM Weight" value={order.vgmWeight} />
      <Field label="Origin Country" value={order.originCountry} />
      <Field label="Origin Port Name" value={order.originPortName} />
      <Field label="Destination Country" value={order.destinationCountry} />
      <Field label="Destination Port Name" value={order.destinationPortName} />
    </FieldGrid>
  );

  const renderEquipments = () => (
    <RecordTable
      columns={[
        { key: 'id', label: 'ID' },
        { key: 'lineNo', label: 'Line No.' },
        { key: 'materialType', label: 'Material Type' },
        { key: 'equipmentTypeNo', label: 'Equipment Type' },
        { key: 'equipmentSubTypeNo', label: 'Sub Type' },
        { key: 'remark', label: 'Remark' },
        { key: 'tareWeight', label: 'Tare Weight' },
        { key: 'vgmWeight', label: 'VGM Weight' },
        { key: 'communicationPartner', label: 'Partner' },
        { key: 'createdBy', label: 'Created By' },
        { key: 'creationDatetime', label: 'Creation Date', render: (v: string) => fmtDate(v) },
      ]}
      rows={filteredEquipments}
    />
  );

  const renderTruckInfo = () => (
    <div>
      <SectionTitle title="Order header" />
      <FieldGrid>
        <Field label="Linked trucks" value={order.linkedTrucks} />
        <Field label="Linked drivers / co-drivers" value={order.linkedDriversCoDrivers} />
        <Field label="Road transport orders" value={order.roadTransportOrders} />
      </FieldGrid>
      <SectionTitle title="Truck by order line" />
      <RecordTable
        columns={[
          { key: 'lineNo', label: 'Line' },
          { key: 'actionCode', label: 'Action' },
          { key: 'addressName', label: 'Stop / address', render: (v: string) => <span title={v}>{v ?? '—'}</span> },
          { key: 'truckNo', label: 'Truck no.' },
          { key: 'truckDescription', label: 'Truck description', render: (v: string) => <span title={v}>{v ?? '—'}</span> },
          { key: 'driverNo', label: 'Driver no.' },
          { key: 'driverFullName', label: 'Driver name', render: (v: string) => <span title={v}>{v ?? '—'}</span> },
          { key: 'chassisNo', label: 'Chassis no.' },
          { key: 'fleetNoChassis', label: 'Fleet (chassis)' },
          { key: 'registrationNoChassis', label: 'Reg. (chassis)' },
          { key: '', label: 'Details', render: (_: unknown, row: ImportOrderLine) => (
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                setTruckLineDetail(row);
              }}
              style={{ background: 'var(--cyan)', border: 'none', borderRadius: 4, padding: '3px 8px', color: '#000', cursor: 'pointer', fontSize: 11, display: 'inline-flex', alignItems: 'center', gap: 4 }}
            >
              <ChevronRight size={10} /> Details
            </button>
          ) },
        ]}
        rows={filteredLines}
      />
    </div>
  );

  const renderTrailerInfo = () => (
    <div>
      <SectionTitle title="Order header" />
      <FieldGrid>
        <Field label="Linked trailers / containers" value={order.linkedTrailersContainers} />
        <Field label="Shipping transport order" value={order.shippingTransportOrder} />
      </FieldGrid>
      <SectionTitle title="Trailer & equipment by order line" />
      <RecordTable
        columns={[
          { key: 'lineNo', label: 'Line' },
          { key: 'actionCode', label: 'Action' },
          { key: 'addressName', label: 'Stop / address', render: (v: string) => <span title={v}>{v ?? '—'}</span> },
          { key: 'trailerNo', label: 'Trailer no.' },
          { key: 'trailerDescription', label: 'Trailer description', render: (v: string) => <span title={v}>{v ?? '—'}</span> },
          { key: 'fleetNoTrailer', label: 'Fleet (trailer)' },
          { key: 'registrationNoTrailer', label: 'Reg. (trailer)' },
          { key: 'containerNo', label: 'Container no.' },
          { key: 'containerNumber', label: 'Container number' },
          { key: '', label: 'Details', render: (_: unknown, row: ImportOrderLine) => (
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                setTrailerLineDetail(row);
              }}
              style={{ background: 'var(--cyan)', border: 'none', borderRadius: 4, padding: '3px 8px', color: '#000', cursor: 'pointer', fontSize: 11, display: 'inline-flex', alignItems: 'center', gap: 4 }}
            >
              <ChevronRight size={10} /> Details
            </button>
          ) },
        ]}
        rows={filteredLines}
      />
    </div>
  );

  const renderRemarks = () => (
    <RecordTable
      columns={[
        { key: 'id', label: 'ID' },
        { key: 'lineNo', label: 'Line No.' },
        { key: 'orderLineNo', label: 'Order line no.', render: (v: number | null | undefined) => (v == null ? '—' : String(v)) },
        { key: 'remarkType', label: 'Remark Type' },
        { key: 'remarks', label: 'Remark', render: (v: string) => <span title={v}>{v}</span> },
        { key: 'externalRemarkCode', label: 'Ext. Remark Code' },
        { key: 'communicationPartner', label: 'Partner' },
        { key: 'createdBy', label: 'Created By' },
        { key: 'creationDatetime', label: 'Creation Date', render: (v: string) => fmtDate(v) },
        { key: '', label: 'Details', render: (_: unknown, row: ImportOrderRemark) => (
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              setRemarkDetail(row);
            }}
            style={{ background: 'var(--cyan)', border: 'none', borderRadius: 4, padding: '3px 8px', color: '#000', cursor: 'pointer', fontSize: 11, display: 'inline-flex', alignItems: 'center', gap: 4 }}
          >
            <ChevronRight size={10} /> Details
          </button>
        ) },
      ]}
      rows={filteredRemarks}
    />
  );

  const renderCustomFields = () => (
    <RecordTable
      columns={[
        { key: 'id', label: 'ID' },
        { key: 'lineNo', label: 'Line No.' },
        { key: 'fieldName', label: 'Field Name', render: (v: string) => <span title={v} style={{ wordBreak: 'break-word' }}>{v}</span> },
        { key: 'fieldValue', label: 'Field Value', render: (v: string) => <span title={v} style={{ wordBreak: 'break-word' }}>{v}</span> },
        { key: 'communicationPartner', label: 'Partner' },
        { key: 'createdBy', label: 'Created By' },
        { key: '', label: 'Details', render: (_: unknown, row: ImportOrderCustomField) => (
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              setCustomFieldDetail(row);
            }}
            style={{ background: 'var(--cyan)', border: 'none', borderRadius: 4, padding: '3px 8px', color: '#000', cursor: 'pointer', fontSize: 11, display: 'inline-flex', alignItems: 'center', gap: 4 }}
          >
            <ChevronRight size={10} /> Details
          </button>
        ) },
      ]}
      rows={filteredCustomFields}
    />
  );

  const renderOrderLines = () => (
    <RecordTable
      columns={[
        { key: 'entryNo', label: 'Entry No.' },
        { key: 'lineNo', label: 'Line No.' },
        { key: 'externalOrderNo', label: 'Ext. order (line)', render: (v: string | null | undefined) => (v ? <span style={{ fontFamily: 'monospace', fontSize: 11 }}>{v}</span> : '—') },
        { key: 'uniqueReference', label: 'Unique Ref' },
        { key: 'actionCode', label: 'Action Code', render: (v: string) => <span style={{ color: 'var(--orange)' }}>{v}</span> },
        { key: 'externalAddressNo', label: 'Ext. Address No.' },
        { key: 'addressName', label: 'Address Name' },
        { key: 'addressStreet', label: 'Street' },
        { key: 'addressCity', label: 'City' },
        { key: 'addressCountryCode', label: 'Country' },
        { key: 'addressPostalCode', label: 'Postal Code' },
        { key: 'orderLineType', label: 'Line Type' },
        { key: 'transportMode', label: 'Transport Mode' },
        { key: 'initialDatetimeFrom', label: 'Date From', render: (v: string) => fmtDate(v) },
        { key: 'initialDatetimeUntil', label: 'Date Until', render: (v: string) => fmtDate(v) },
        { key: 'containerNo', label: 'Container No.' },
        { key: 'loaded', label: 'Loaded', render: (v: boolean) => fmtBool(v) },
        { key: 'communicationPartner', label: 'Partner' },
        { key: '', label: 'Details', render: (_: any, row: ImportOrderLine) => (
          <button onClick={e => { e.stopPropagation(); setSelectedLine(row); setLineTab(0); }}
            style={{ background: 'var(--cyan)', border: 'none', borderRadius: 4, padding: '3px 8px', color: '#000', cursor: 'pointer', fontSize: 11, display: 'flex', alignItems: 'center', gap: 4 }}>
            <ChevronRight size={10} /> Details
          </button>
        )},
      ]}
      rows={filteredLines}
    />
  );

  // ── Order Line Detail Dialog ───────────────────────────────────────────

  const LINE_TABS = ['General', 'Address', 'Date/Time', 'Transport', 'Driver/Vehicle', 'Shipping', 'Status', 'Additional', 'Cargo', 'Custom fields'];

  const renderLineContent = (line: ImportOrderLine) => {
    if (lineTab === 0) return (
      <FieldGrid>
        <Field label="Entry No." value={line.entryNo} />
        <Field label="Line No." value={line.lineNo} />
        <Field label="External Order No." value={line.externalOrderNo} />
        <Field label="Unique Reference" value={line.uniqueReference} />
        <Field label="Original" value={fmtBool(line.original)} />
        <Field label="Processed" value={fmtBool(line.processed)} />
        <Field label="No Planning Required" value={fmtBool(line.noPlanningRequired)} />
        <Field label="Action Code" value={line.actionCode} />
        <Field label="External Address No." value={line.externalAddressNo} />
        <Field label="Order Line Type" value={line.orderLineType} />
        <Field label="Source" value={line.source} />
        <Field label="Communication Partner" value={line.communicationPartner} />
        <Field label="Import DateTime" value={fmtDate(line.importDatetime)} />
        <Field label="Processed DateTime" value={fmtDate(line.processedDatetime)} />
      </FieldGrid>
    );
    if (lineTab === 1) return (
      <FieldGrid>
        <Field label="Address Name" value={line.addressName} />
        <Field label="Address Name 2" value={line.addressName2} />
        <Field label="Address Street" value={line.addressStreet} />
        <Field label="Address Number" value={line.addressNumber} />
        <Field label="Address Postal Code" value={line.addressPostalCode} />
        <Field label="Address City" value={line.addressCity} />
        <Field label="Address Country Code" value={line.addressCountryCode} />
        <Field label="Address City ID" value={line.addressCityId} />
        <Field label="Region No." value={line.regionNo} />
        <Field label="Master Region No." value={line.masterRegionNo} />
        <Field label="Planning Zone No." value={line.planningZoneNo} />
        <Field label="Planning Zone Description" value={line.planningZoneDescription} />
        <Field label="Planning Zone" value={line.planningZone} />
        <Field label="Latitude" value={line.latitude} />
        <Field label="Longitude" value={line.longitude} />
      </FieldGrid>
    );
    if (lineTab === 2) return (
      <FieldGrid>
        <Field label="Initial DateTime From" value={fmtDate(line.initialDatetimeFrom)} />
        <Field label="Initial DateTime Until" value={fmtDate(line.initialDatetimeUntil)} />
        <Field label="Initial DateTime From 2" value={fmtDate(line.initialDatetimeFrom2)} />
        <Field label="Initial DateTime Until 2" value={fmtDate(line.initialDatetimeUntil2)} />
        <Field label="Booked DateTime From" value={fmtDate(line.bookedDatetimeFrom)} />
        <Field label="Booked DateTime Until" value={fmtDate(line.bookedDatetimeUntil)} />
        <Field label="Requested DateTime From" value={fmtDate(line.requestedDatetimeFrom)} />
        <Field label="Requested DateTime Until" value={fmtDate(line.requestedDatetimeUntil)} />
        <Field label="Closing DateTime" value={fmtDate(line.closingDatetime)} />
        <Field label="Latest Departure Hour" value={fmtDate(line.latestDepartureHour)} />
        <Field label="Latest Hour of Departure" value={fmtDate(line.latestHourOfDeparture)} />
        <Field label="Time From" value={fmtDate(line.timeFrom)} />
        <Field label="Time Until" value={fmtDate(line.timeUntil)} />
        <Field label="Planned DateTime Prev OL" value={fmtDate(line.plannedDatetimeOfPrevOl)} />
        <Field label="Confirmed DateTime" value={fmtDate(line.confirmedDatetime)} />
        <Field label="Type Of Time" value={line.typeOfTime} />
      </FieldGrid>
    );
    if (lineTab === 3) return (
      <FieldGrid>
        <Field label="Transport Mode" value={line.transportMode} />
        <Field label="Transport Mode Type" value={line.transportModeType} />
        <Field label="Transport Order No." value={line.transportOrderNo} />
        <Field label="Order Block" value={line.orderBlock} />
        <Field label="Order Block ID" value={line.orderBlockId} />
        <Field label="Planning Sequence" value={line.planningSequence} />
        <Field label="Planning Block ID" value={line.planningBlockId} />
        <Field label="Grouping ID" value={line.groupingId} />
        <Field label="Sorting Key" value={line.sortingKey} />
        <Field label="Groupage Block" value={line.groupageBlock} />
        <Field label="Calculated Distance" value={line.calculatedDistance} />
        <Field label="Calculated Distance TO" value={line.calculatedDistanceTo} />
        <Field label="Mileage" value={line.mileage} />
        <Field label="Mileage Difference" value={line.mileageDifference} />
        <Field label="Duration Actual Difference" value={line.durationActualDifference} />
        <Field label="Bound Calculated" value={fmtBool(line.boundCalculated)} />
        <Field label="Manual Calculated" value={fmtBool(line.manualCalculated)} />
        <Field label="Needs Recalculate" value={fmtBool(line.needsRecalculate)} />
      </FieldGrid>
    );
    if (lineTab === 4) return (
      <FieldGrid>
        <Field label="Driver No." value={line.driverNo} />
        <Field label="Driver Full Name" value={line.driverFullName} />
        <Field label="Driver Short Name" value={line.driverShortName} />
        <Field label="Co-Driver No." value={line.coDriverNo} />
        <Field label="Co-Driver Full Name" value={line.coDriverFullName} />
        <Field label="Co-Driver Short Name" value={line.coDriverShortName} />
        <Field label="Truck No." value={line.truckNo} />
        <Field label="Truck Description" value={line.truckDescription} />
        <Field label="Trailer No." value={line.trailerNo} />
        <Field label="Trailer Description" value={line.trailerDescription} />
        <Field label="Chassis No." value={line.chassisNo} />
        <Field label="Chassis Description" value={line.chassisDescription} />
        <Field label="Fleet No. Trailer" value={line.fleetNoTrailer} />
        <Field label="Reg. No. Trailer" value={line.registrationNoTrailer} />
        <Field label="Fleet No. Chassis" value={line.fleetNoChassis} />
        <Field label="Reg. No. Chassis" value={line.registrationNoChassis} />
        <Field label="Container No." value={line.containerNo} />
        <Field label="Container Number" value={line.containerNumber} />
        <Field label="Container No. 2" value={line.containerNo2} />
        <Field label="Container Number 2" value={line.containerNumber2} />
        <Field label="Other Equipment No." value={line.otherEquipmentNo} />
        <Field label="Other Equipment Desc." value={line.otherEquipmentDescription} />
        <Field label="Equipment Traction" value={line.equipmentTraction} />
      </FieldGrid>
    );
    if (lineTab === 5) return (
      <FieldGrid>
        <Field label="Shipping Company No." value={line.shippingCompanyNo} />
        <Field label="Shipping Company Name" value={line.shippingCompanyName} />
        <Field label="Shipping Line No." value={line.shippingLineNo} />
        <Field label="Shipping Line Name" value={line.shippingLineName} />
        <Field label="Shipping Line Diary ID" value={line.shippingLineDiaryId} />
        <Field label="Preferred Shipping Line" value={line.preferredShippingLine} />
        <Field label="SL Booked For OL" value={fmtBool(line.slBookedForOl)} />
        <Field label="Pre Book" value={fmtBool(line.preBook)} />
        <Field label="Haulier No." value={line.haulierNo} />
        <Field label="Supplier No." value={line.supplierNo} />
      </FieldGrid>
    );
    if (lineTab === 6) return (
      <FieldGrid>
        <Field label="Loaded" value={fmtBool(line.loaded)} />
        <Field label="Is Late" value={fmtBool(line.isLate)} />
        <Field label="Part Of Order" value={fmtBool(line.partOfOrder)} />
        <Field label="Plug In" value={fmtBool(line.plugIn)} />
        <Field label="Selected" value={fmtBool(line.selected)} />
        <Field label="Selected By" value={line.selectedBy} />
        <Field label="Sent To Haulier" value={fmtBool(line.sentToHaulier)} />
        <Field label="Sent To BC" value={fmtBool(line.sentToBc)} />
        <Field label="Cancelled By Driver" value={fmtBool(line.cancelledByDriver)} />
        <Field label="Needs Revision" value={fmtBool(line.needsRevision)} />
        <Field label="Booking Required" value={fmtBool(line.bookingRequired)} />
        <Field label="Check Booking" value={fmtBool(line.checkBooking)} />
        <Field label="Confirmed By" value={line.confirmedBy} />
        <Field label="Booking Change Reason" value={line.bookingChangeReason} />
        <Field label="Booking Info Validation" value={line.bookingInfoValidation} />
        <Field label="Split PB" value={line.splitPb} />
      </FieldGrid>
    );
    if (lineTab === 7) return (
      <FieldGrid>
        <Field label="Order Line ID" value={line.orderLineId} />
        <Field label="Local Order Line ID" value={line.localOrderLineId} />
        <Field label="External Order Line ID" value={line.externalOrderLineId} />
        <Field label="Order Line Ref 1" value={line.orderLineRef1} />
        <Field label="Order Line Ref 2" value={line.orderLineRef2} />
        <Field label="No. Series" value={line.noSeries} />
        <Field label="Sales Responsible" value={line.salesResponsible} />
        <Field label="Cust. Serv. Responsible" value={line.custServResponsible} />
        <Field label="Office" value={line.office} />
        <Field label="Capacity Diary ID" value={line.capacityDiaryId} />
        <Field label="Created By" value={line.createdBy} />
        <Field label="Creation DateTime" value={fmtDate(line.creationDatetime)} />
        <Field label="Last Modified By" value={line.lastModifiedBy} />
        <Field label="Last Modification" value={fmtDate(line.lastModificationDatetime)} />
      </FieldGrid>
    );
    if (lineTab === 8) {
      const cargos = cargoRowsForOrderLine(order, line, filteredCargoItems);
      return (
        <RecordTable
          columns={[
            { key: 'lineNo', label: 'Cargo row' },
            { key: 'orderLineNo', label: 'Order line no.', render: (v: number | null | undefined) => (v == null ? '—' : String(v)) },
            { key: 'externalGoodNo', label: 'External Good No.' },
            { key: 'description', label: 'Description', render: (v: string) => <span title={v}>{v ?? '—'}</span> },
            { key: 'quantity', label: 'Qty', render: (v: number) => fmtNum(v) },
            { key: 'unitOfMeasureCode', label: 'UOM' },
            { key: 'grossWeight', label: 'Gross Wt', render: (v: number) => fmtNum(v) },
            { key: 'netWeight', label: 'Net Wt', render: (v: number) => fmtNum(v) },
            { key: 'loadingMeters', label: 'Loading M.', render: (v: number) => fmtNum(v) },
            { key: 'adrDangerousForEnvironment', label: 'ADR env.', render: (v: boolean) => fmtBool(v) },
            { key: 'temperature', label: 'Temp.', render: (v: number) => fmtNum(v) },
            { key: '', label: 'Details', render: (_: unknown, row: ImportOrderCargo) => (
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  setSelectedOrderCargo(row);
                }}
                style={{ background: 'var(--bg3)', border: '1px solid var(--border)', borderRadius: 4, padding: '3px 8px', color: 'var(--text)', cursor: 'pointer', fontSize: 11 }}
              >
                Details
              </button>
            ) },
          ]}
          rows={cargos}
        />
      );
    }
    if (lineTab === 9) {
      const cfs = customFieldsForOrderLine(order, line, filteredCustomFields);
      return (
        <RecordTable
          columns={[
            { key: 'id', label: 'ID' },
            { key: 'lineNo', label: 'Line No.' },
            { key: 'fieldName', label: 'Field Name', render: (v: string) => <span title={v} style={{ wordBreak: 'break-word' }}>{v}</span> },
            { key: 'fieldValue', label: 'Field Value', render: (v: string) => <span title={v} style={{ wordBreak: 'break-word' }}>{v}</span> },
            { key: 'communicationPartner', label: 'Partner' },
            { key: 'createdBy', label: 'Created By' },
            { key: '', label: 'Details', render: (_: unknown, row: ImportOrderCustomField) => (
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  setCustomFieldDetail(row);
                }}
                style={{ background: 'var(--cyan)', border: 'none', borderRadius: 4, padding: '3px 8px', color: '#000', cursor: 'pointer', fontSize: 11 }}
              >
                Details
              </button>
            ) },
          ]}
          rows={cfs}
        />
      );
    }
    return null;
  };

  // ── JSX ───────────────────────────────────────────────────────────────

  return (
    <div style={{ padding: '24px 28px', fontFamily: "'Exo 2',sans-serif", height: '100%', display: 'flex', flexDirection: 'column' }}>

      {toast && (
        <div style={{ position: 'fixed', top: 20, right: 20, zIndex: 9999, background: toast.ok ? '#10b981' : '#ef4444', color: '#fff', padding: '10px 18px', borderRadius: 8, fontSize: 13 }}>{toast.msg}</div>
      )}

      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 18, flexShrink: 0, flexWrap: 'wrap' }}>
        <button onClick={() => navigate('/import-orders')}
          style={{ background: 'var(--bg3)', border: '1px solid var(--border)', borderRadius: 6, padding: '6px 10px', color: 'var(--text)', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 5, fontSize: 12 }}>
          <ArrowLeft size={12} /> Back
        </button>
        <h1 style={{ margin: 0, fontSize: 18, color: 'var(--text)' }}>
          Import Order #{order.entryNo}
          {activePartition ? (
            <span style={{ color: 'var(--cyan)', fontWeight: 600 }}>{' — '}{activePartition}</span>
          ) : null}
        </h1>
        {activePartition && (
          <button
            type="button"
            onClick={clearPartitionFilter}
            style={{
              background: 'var(--bg3)',
              border: '1px solid var(--border)',
              borderRadius: 6,
              padding: '4px 10px',
              color: 'var(--muted)',
              cursor: 'pointer',
              fontSize: 11,
            }}
          >
            Show all orders
          </button>
        )}
        <span style={{ color: statusColor, background: `${statusColor}18`, border: `1px solid ${statusColor}40`, borderRadius: 5, padding: '3px 10px', fontSize: 11, fontWeight: 700 }}>{order.status}</span>
        {order.tmsOrderNo && (
          <span style={{ color: '#10b981', background: 'rgba(16,185,129,.1)', border: '1px solid rgba(16,185,129,.3)', borderRadius: 5, padding: '3px 10px', fontSize: 11 }}>
            → TMS {order.tmsOrderNo}
          </span>
        )}
        <div style={{ flex: 1 }} />
        <button onClick={() => setShowHidden(v => !v)} title={showHidden ? 'Hide extra fields' : 'Show all fields'}
          style={{ background: 'var(--bg3)', border: '1px solid var(--border)', borderRadius: 6, padding: '6px 10px', color: showHidden ? 'var(--cyan)' : 'var(--muted)', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 5, fontSize: 12 }}>
          {showHidden ? <EyeOff size={12} /> : <Eye size={12} />}
        </button>
        <button onClick={handleValidate} disabled={validating}
          style={{ background: 'var(--bg3)', border: '1px solid var(--cyan)', borderRadius: 6, padding: '6px 12px', color: 'var(--cyan)', cursor: 'pointer', fontSize: 12, display: 'flex', alignItems: 'center', gap: 6 }}>
          {validating ? <Loader2 size={11} /> : <CheckCircle2 size={11} />} Validate
        </button>
        <button onClick={() => navigate(`/import-orders/${order.entryNo}/edit`)}
          style={{ background: 'var(--bg3)', border: '1px solid var(--border)', borderRadius: 6, padding: '6px 12px', color: 'var(--text)', cursor: 'pointer', fontSize: 12, display: 'flex', alignItems: 'center', gap: 6 }}>
          <Edit size={11} /> Edit
        </button>
        {(order.status === 'RECEIVED' || order.status === 'READY_TO_PROCESS' || order.status === 'ERROR') && (
          <button onClick={handleProcess} disabled={processing}
            style={{ background: 'var(--cyan)', border: 'none', borderRadius: 6, padding: '6px 14px', color: '#000', fontWeight: 700, cursor: 'pointer', fontSize: 12, display: 'flex', alignItems: 'center', gap: 6 }}>
            {processing ? <Loader2 size={11} /> : <Play size={11} />} Process
          </button>
        )}
      </div>

      {/* Validation errors */}
      {validErrors && validErrors.length > 0 && (
        <div style={{ background: 'rgba(239,68,68,.08)', border: '1px solid rgba(239,68,68,.3)', borderRadius: 8, padding: '12px 16px', marginBottom: 16, flexShrink: 0 }}>
          <div style={{ fontWeight: 700, color: '#ef4444', marginBottom: 8, fontSize: 12 }}>
            <AlertTriangle size={13} style={{ verticalAlign: 'middle', marginRight: 5 }} />Validation Errors
          </div>
          {validErrors.map((e, i) => <div key={i} style={{ fontSize: 12, color: 'var(--text2)', marginBottom: 4 }}>• {e}</div>)}
        </div>
      )}

      {/* Error message */}
      {order.errorMessage && (
        <div style={{ background: 'rgba(239,68,68,.08)', border: '1px solid rgba(239,68,68,.3)', borderRadius: 8, padding: '10px 14px', marginBottom: 16, fontSize: 12, color: '#ef4444', flexShrink: 0 }}>
          <AlertTriangle size={12} style={{ verticalAlign: 'middle', marginRight: 6 }} />{order.errorMessage}
        </div>
      )}

      {partitionRows.length >= 1 && (
        <div
          style={{
            marginBottom: 16,
            flexShrink: 0,
            background: 'var(--bg2)',
            border: '1px solid var(--border)',
            borderRadius: 10,
            overflow: 'hidden',
          }}
        >
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: 12,
              padding: '10px 14px',
              borderBottom: '1px solid var(--border)',
              flexWrap: 'wrap',
            }}
          >
            <span
              style={{
                fontSize: 11,
                fontWeight: 700,
                color: 'var(--cyan)',
                textTransform: 'uppercase',
                letterSpacing: '.8px',
              }}
            >
              Orders in this import ({partitionRows.length})
            </span>
            <Link
              to={`/tms-orders?impEntryNo=${order.entryNo}`}
              style={{ fontSize: 11, color: '#10b981', textDecoration: 'none', whiteSpace: 'nowrap' }}
            >
              All TMS orders for import #{order.entryNo} →
            </Link>
          </div>
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
              <thead>
                <tr style={{ background: 'var(--bg3)', borderBottom: '1px solid var(--border)' }}>
                  {['External order no.', 'Create date', 'Status', 'Entry no.', 'TMS order'].map((h) => (
                    <th
                      key={h}
                      style={{
                        padding: '9px 12px',
                        textAlign: 'left',
                        color: 'var(--muted)',
                        fontWeight: 700,
                        fontSize: 10,
                        textTransform: 'uppercase',
                        letterSpacing: '.5px',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {partitionRows.map((row) => {
                  const active = activePartition === row.externalOrderNo;
                  return (
                    <tr
                      key={row.externalOrderNo}
                      onClick={() => openPartitionDetail(row.externalOrderNo)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault();
                          openPartitionDetail(row.externalOrderNo);
                        }
                      }}
                      tabIndex={0}
                      role="button"
                      style={{
                        borderBottom: '1px solid var(--border)',
                        cursor: 'pointer',
                        background: active ? 'rgba(0,212,255,.1)' : 'transparent',
                        transition: 'background .1s',
                      }}
                      onMouseEnter={(e) => {
                        if (!active) e.currentTarget.style.background = 'var(--bg3)';
                      }}
                      onMouseLeave={(e) => {
                        if (!active) e.currentTarget.style.background = 'transparent';
                      }}
                    >
                      <td style={{ padding: '10px 12px', fontFamily: 'monospace', color: 'var(--text)' }}>
                        {row.externalOrderNo}
                      </td>
                      <td style={{ padding: '10px 12px', color: 'var(--text2)', fontSize: 11 }}>{fmtDate(row.createDate)}</td>
                      <td style={{ padding: '10px 12px', fontSize: 11 }}>
                        <span
                          style={{
                            color: STATUS_COLOR[order.status] || 'var(--text2)',
                            fontWeight: 600,
                          }}
                        >
                          {row.status}
                        </span>
                      </td>
                      <td style={{ padding: '10px 12px', fontFamily: 'monospace', color: 'var(--cyan)' }}>{row.entryNo}</td>
                      <td style={{ padding: '10px 12px' }}>
                        {row.tmsOrderNo ? (
                          <Link
                            to={`/tms-orders?impEntryNo=${order.entryNo}`}
                            onClick={(e) => e.stopPropagation()}
                            style={{ color: '#10b981', fontFamily: 'monospace', fontSize: 11, textDecoration: 'none' }}
                          >
                            {row.tmsOrderNo}
                          </Link>
                        ) : (
                          <span style={{ color: 'var(--muted)' }}>—</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          <div style={{ padding: '8px 14px 10px', fontSize: 10, color: 'var(--muted)' }}>
            Click a row to filter tabs to one external order (lines, cargo, equipment, remarks). Use Show all orders above to clear.
          </div>
        </div>
      )}

      {/* Tabs */}
      <div style={{ display: 'flex', gap: 2, borderBottom: '1px solid var(--border)', marginBottom: 0, flexShrink: 0, overflowX: 'auto' }}>
        {TABS.map((t, i) => (
          <button key={t} onClick={() => setActiveTab(i)} style={{
            background: activeTab === i ? 'var(--bg3)' : 'none',
            border: 'none', borderBottom: activeTab === i ? '2px solid var(--cyan)' : '2px solid transparent',
            padding: '9px 14px', color: activeTab === i ? 'var(--cyan)' : 'var(--muted)',
            cursor: 'pointer', fontSize: 12, fontWeight: activeTab === i ? 700 : 400, whiteSpace: 'nowrap',
          }}>
            {t}
            {t === 'Order Lines' && filteredLines.length > 0 && (
              <span style={{ marginLeft: 5, fontSize: 10, background: 'var(--cyan)', color: '#000', borderRadius: 4, padding: '1px 5px' }}>{filteredLines.length}</span>
            )}
            {t === 'Equipments' && filteredEquipments.length > 0 && (
              <span style={{ marginLeft: 5, fontSize: 10, background: 'var(--bg2)', color: 'var(--muted)', borderRadius: 4, padding: '1px 5px' }}>{filteredEquipments.length}</span>
            )}
            {t === 'Truck Info' && truckInfoLineCount(order, filteredLines) > 0 && (
              <span style={{ marginLeft: 5, fontSize: 10, background: 'var(--bg2)', color: 'var(--muted)', borderRadius: 4, padding: '1px 5px' }}>{truckInfoLineCount(order, filteredLines)}</span>
            )}
            {t === 'Trailer Info' && trailerInfoLineCount(order, filteredLines) > 0 && (
              <span style={{ marginLeft: 5, fontSize: 10, background: 'var(--bg2)', color: 'var(--muted)', borderRadius: 4, padding: '1px 5px' }}>{trailerInfoLineCount(order, filteredLines)}</span>
            )}
            {t === 'Remarks' && filteredRemarks.length > 0 && (
              <span style={{ marginLeft: 5, fontSize: 10, background: 'var(--bg2)', color: 'var(--muted)', borderRadius: 4, padding: '1px 5px' }}>{filteredRemarks.length}</span>
            )}
            {t === 'Custom Fields' && filteredCustomFields.length > 0 && (
              <span style={{ marginLeft: 5, fontSize: 10, background: 'var(--cyan)', color: '#000', borderRadius: 4, padding: '1px 5px' }}>{filteredCustomFields.length}</span>
            )}
          </button>
        ))}
      </div>

      {/* Tab content */}
      <div style={{ flex: 1, minHeight: 0, background: 'var(--bg2)', border: '1px solid var(--border)', borderTop: 'none', borderRadius: '0 0 10px 10px', overflow: 'auto', padding: '16px 20px' }}>
        {activeTab === 0  && renderGeneral()}
        {activeTab === 1  && renderCustomer()}
        {activeTab === 2  && renderBillTo()}
        {activeTab === 3  && renderTransport()}
        {activeTab === 4  && renderDates()}
        {activeTab === 5  && renderAdditional()}
        {activeTab === 6  && renderContainerInfo()}
        {activeTab === 7  && renderVesselInfo()}
        {activeTab === 8  && renderEquipments()}
        {activeTab === 9  && renderTruckInfo()}
        {activeTab === 10 && renderTrailerInfo()}
        {activeTab === 11 && renderRemarks()}
        {activeTab === 12 && renderCustomFields()}
        {activeTab === 13 && renderOrderLines()}
      </div>

      {/* ── Order Line Detail Dialog ─────────────────────────────────────── */}
      {selectedLine && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,.7)', zIndex: 9000, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ background: 'var(--bg1)', border: '1px solid var(--border)', borderRadius: 12, width: '90vw', maxWidth: 960, height: '85vh', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            <div style={{ display: 'flex', alignItems: 'center', padding: '14px 20px', borderBottom: '1px solid var(--border)', flexShrink: 0 }}>
              <span style={{ fontWeight: 700, fontSize: 14, color: 'var(--text)' }}>
                Order Line — Entry #{selectedLine.entryNo} | Line #{selectedLine.lineNo}
              </span>
              <div style={{ flex: 1 }} />
              <button onClick={() => { setSelectedLine(null); setSelectedOrderCargo(null); }}
                style={{ background: 'transparent', border: 'none', color: 'var(--muted)', cursor: 'pointer', padding: 4 }}>
                <X size={16} />
              </button>
            </div>
            {/* Sub-tabs */}
            <div style={{ display: 'flex', gap: 2, borderBottom: '1px solid var(--border)', padding: '0 16px', flexShrink: 0, overflowX: 'auto' }}>
              {LINE_TABS.map((t, i) => (
                <button key={t} onClick={() => setLineTab(i)} style={{
                  background: lineTab === i ? 'var(--bg3)' : 'none',
                  border: 'none', borderBottom: lineTab === i ? '2px solid var(--cyan)' : '2px solid transparent',
                  padding: '8px 12px', color: lineTab === i ? 'var(--cyan)' : 'var(--muted)',
                  cursor: 'pointer', fontSize: 11, fontWeight: lineTab === i ? 700 : 400, whiteSpace: 'nowrap',
                }}>
                  {t}
                  {t === 'Cargo' && selectedLine && cargoRowsForOrderLine(order, selectedLine, filteredCargoItems).length > 0 && (
                    <span style={{ marginLeft: 4, fontSize: 10, background: 'var(--cyan)', color: '#000', borderRadius: 4, padding: '1px 4px' }}>{cargoRowsForOrderLine(order, selectedLine, filteredCargoItems).length}</span>
                  )}
                  {t === 'Custom fields' && selectedLine && customFieldsForOrderLine(order, selectedLine, filteredCustomFields).length > 0 && (
                    <span style={{ marginLeft: 4, fontSize: 10, background: 'var(--cyan)', color: '#000', borderRadius: 4, padding: '1px 4px' }}>{customFieldsForOrderLine(order, selectedLine, filteredCustomFields).length}</span>
                  )}
                </button>
              ))}
            </div>
            <div style={{ flex: 1, minHeight: 0, overflow: 'auto', padding: '16px 20px' }}>
              {renderLineContent(selectedLine)}
            </div>
          </div>
        </div>
      )}

      {/* ── Cargo detail (order line cargo from header cargoItems) ───────── */}
      {selectedOrderCargo && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,.75)', zIndex: 9500, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ background: 'var(--bg1)', border: '1px solid var(--border)', borderRadius: 12, width: '80vw', maxWidth: 760, maxHeight: '80vh', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            <div style={{ display: 'flex', alignItems: 'center', padding: '14px 20px', borderBottom: '1px solid var(--border)', flexShrink: 0 }}>
              <span style={{ fontWeight: 700, fontSize: 14, color: 'var(--text)' }}>
                Cargo details — row #{selectedOrderCargo.lineNo}
                {selectedOrderCargo.orderLineNo != null && selectedOrderCargo.orderLineNo !== undefined
                  ? ` (order line ${selectedOrderCargo.orderLineNo})`
                  : ''}
              </span>
              <div style={{ flex: 1 }} />
              <button type="button" onClick={() => setSelectedOrderCargo(null)}
                style={{ background: 'transparent', border: 'none', color: 'var(--muted)', cursor: 'pointer', padding: 4 }}>
                <X size={16} />
              </button>
            </div>
            <div style={{ flex: 1, overflow: 'auto', padding: '16px 20px' }}>
              <FieldGrid>
                <Field label="Entry No." value={fmtNum(selectedOrderCargo.entryNo)} />
                <Field label="Cargo row (line_no)" value={fmtNum(selectedOrderCargo.lineNo)} />
                <Field label="Order line no." value={selectedOrderCargo.orderLineNo == null ? '—' : String(selectedOrderCargo.orderLineNo)} />
                <Field label="External order no." value={selectedOrderCargo.externalOrderNo} />
                <Field label="Communication partner" value={selectedOrderCargo.communicationPartner} />
                <Field label="External Good No." value={selectedOrderCargo.externalGoodNo} />
                <Field label="External good type" value={selectedOrderCargo.externalGoodType} />
                <Field label="External good sub type" value={selectedOrderCargo.externalGoodSubType} />
                <Field label="Description" value={selectedOrderCargo.description} />
                <Field label="Description 2" value={selectedOrderCargo.description2} />
                <Field label="Tracing no. 1" value={selectedOrderCargo.tracingNo1} />
                <Field label="Tracing no. 2" value={selectedOrderCargo.tracingNo2} />
                <Field label="Quantity" value={fmtNum(selectedOrderCargo.quantity)} />
                <Field label="Unit of measure" value={selectedOrderCargo.unitOfMeasureCode} />
                <Field label="Gross weight" value={fmtNum(selectedOrderCargo.grossWeight)} />
                <Field label="Net weight" value={fmtNum(selectedOrderCargo.netWeight)} />
                <Field label="Length" value={fmtNum(selectedOrderCargo.length)} />
                <Field label="Width" value={fmtNum(selectedOrderCargo.width)} />
                <Field label="Height" value={fmtNum(selectedOrderCargo.height)} />
                <Field label="Diameter" value={fmtNum(selectedOrderCargo.diameter)} />
                <Field label="Loading meters" value={fmtNum(selectedOrderCargo.loadingMeters)} />
                <Field label="Pallet places" value={fmtNum(selectedOrderCargo.palletPlaces)} />
                <Field label="Force loading meters" value={fmtBool(selectedOrderCargo.forceLoadingMeters)} />
                <Field label="Dangerous goods" value={fmtBool(selectedOrderCargo.dangerousGoods)} />
                <Field label="ADR type" value={selectedOrderCargo.adrType} />
                <Field label="ADR dangerous for environment" value={fmtBool(selectedOrderCargo.adrDangerousForEnvironment)} />
                <Field label="ADR UN no." value={selectedOrderCargo.adrUnNo} />
                <Field label="ADR hazard class" value={selectedOrderCargo.adrHazardClass} />
                <Field label="ADR packing group" value={selectedOrderCargo.adrPackingGroup} />
                <Field label="ADR tunnel restriction" value={selectedOrderCargo.adrTunnelRestrictionCode} />
                <Field label="Temperature" value={fmtNum(selectedOrderCargo.temperature)} />
                <Field label="Min temperature" value={fmtNum(selectedOrderCargo.minTemperature)} />
                <Field label="Max temperature" value={fmtNum(selectedOrderCargo.maxTemperature)} />
                <Field label="Import date/time" value={fmtDate(selectedOrderCargo.importDatetime)} />
                <Field label="Processed date/time" value={fmtDate(selectedOrderCargo.processedDatetime)} />
              </FieldGrid>
            </div>
          </div>
        </div>
      )}

      {/* ── Custom field detail (header or line context) ─────────────────── */}
      {customFieldDetail && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,.75)', zIndex: 9600, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ background: 'var(--bg1)', border: '1px solid var(--border)', borderRadius: 12, width: 'min(640px, 92vw)', maxHeight: '85vh', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            <div style={{ display: 'flex', alignItems: 'center', padding: '14px 20px', borderBottom: '1px solid var(--border)', flexShrink: 0 }}>
              <span style={{ fontWeight: 700, fontSize: 14, color: 'var(--text)' }}>Custom field details</span>
              <div style={{ flex: 1 }} />
              <button type="button" onClick={() => setCustomFieldDetail(null)}
                style={{ background: 'transparent', border: 'none', color: 'var(--muted)', cursor: 'pointer', padding: 4 }}>
                <X size={16} />
              </button>
            </div>
            <div style={{ flex: 1, overflow: 'auto', padding: '16px 20px' }}>
              <FieldGrid>
                <Field label="ID" value={fmtNum(customFieldDetail.id)} />
                <Field label="Entry no." value={fmtNum(customFieldDetail.entryNo)} />
                <Field label="Line no." value={fmtNum(customFieldDetail.lineNo)} />
                <Field label="Field name" value={<span style={{ wordBreak: 'break-word' }}>{customFieldDetail.fieldName}</span>} />
                <Field label="Field value" value={<span style={{ wordBreak: 'break-word', whiteSpace: 'pre-wrap' }}>{customFieldDetail.fieldValue ?? '—'}</span>} />
                <Field label="External order no." value={customFieldDetail.externalOrderNo} />
                <Field label="Communication partner" value={customFieldDetail.communicationPartner} />
                <Field label="Created by" value={customFieldDetail.createdBy} />
                <Field label="Creation date/time" value={fmtDate(customFieldDetail.creationDatetime)} />
                <Field label="Last modified by" value={customFieldDetail.lastModifiedBy} />
                <Field label="Last modification" value={fmtDate(customFieldDetail.lastModificationDatetime)} />
              </FieldGrid>
            </div>
          </div>
        </div>
      )}

      {truckLineDetail && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,.75)', zIndex: 9620, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ background: 'var(--bg1)', border: '1px solid var(--border)', borderRadius: 12, width: 'min(900px, 94vw)', maxHeight: '88vh', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            <div style={{ display: 'flex', alignItems: 'center', padding: '14px 20px', borderBottom: '1px solid var(--border)', flexShrink: 0 }}>
              <span style={{ fontWeight: 700, fontSize: 14, color: 'var(--text)' }}>
                Truck details — line #{truckLineDetail.lineNo}
              </span>
              <div style={{ flex: 1 }} />
              <button type="button" onClick={() => setTruckLineDetail(null)}
                style={{ background: 'transparent', border: 'none', color: 'var(--muted)', cursor: 'pointer', padding: 4 }}>
                <X size={16} />
              </button>
            </div>
            <div style={{ flex: 1, overflow: 'auto', padding: '16px 20px' }}>
              <FieldGrid>
                <Field label="Entry no." value={fmtNum(truckLineDetail.entryNo)} />
                <Field label="Line no." value={fmtNum(truckLineDetail.lineNo)} />
                <Field label="Action code" value={truckLineDetail.actionCode} />
                <Field label="Address name" value={truckLineDetail.addressName} />
                <Field label="Address city" value={truckLineDetail.addressCity} />
                <Field label="Truck no." value={truckLineDetail.truckNo} />
                <Field label="Truck description" value={truckLineDetail.truckDescription} />
                <Field label="Driver no." value={truckLineDetail.driverNo} />
                <Field label="Driver full name" value={truckLineDetail.driverFullName} />
                <Field label="Driver short name" value={truckLineDetail.driverShortName} />
                <Field label="Co-driver no." value={truckLineDetail.coDriverNo} />
                <Field label="Co-driver full name" value={truckLineDetail.coDriverFullName} />
                <Field label="Co-driver short name" value={truckLineDetail.coDriverShortName} />
                <Field label="Chassis no." value={truckLineDetail.chassisNo} />
                <Field label="Chassis description" value={truckLineDetail.chassisDescription} />
                <Field label="Fleet no. chassis" value={truckLineDetail.fleetNoChassis} />
                <Field label="Registration no. chassis" value={truckLineDetail.registrationNoChassis} />
                <Field label="Equipment traction" value={truckLineDetail.equipmentTraction} />
                <Field label="Haulier no." value={truckLineDetail.haulierNo} />
                <Field label="Transport mode" value={truckLineDetail.transportMode} />
                <Field label="Communication partner" value={truckLineDetail.communicationPartner} />
                <Field label="Created by" value={truckLineDetail.createdBy} />
                <Field label="Creation date/time" value={fmtDate(truckLineDetail.creationDatetime)} />
                <Field label="Last modified by" value={truckLineDetail.lastModifiedBy} />
                <Field label="Last modification" value={fmtDate(truckLineDetail.lastModificationDatetime)} />
              </FieldGrid>
            </div>
          </div>
        </div>
      )}

      {trailerLineDetail && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,.75)', zIndex: 9630, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ background: 'var(--bg1)', border: '1px solid var(--border)', borderRadius: 12, width: 'min(900px, 94vw)', maxHeight: '88vh', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            <div style={{ display: 'flex', alignItems: 'center', padding: '14px 20px', borderBottom: '1px solid var(--border)', flexShrink: 0 }}>
              <span style={{ fontWeight: 700, fontSize: 14, color: 'var(--text)' }}>
                Trailer details — line #{trailerLineDetail.lineNo}
              </span>
              <div style={{ flex: 1 }} />
              <button type="button" onClick={() => setTrailerLineDetail(null)}
                style={{ background: 'transparent', border: 'none', color: 'var(--muted)', cursor: 'pointer', padding: 4 }}>
                <X size={16} />
              </button>
            </div>
            <div style={{ flex: 1, overflow: 'auto', padding: '16px 20px' }}>
              <FieldGrid>
                <Field label="Entry no." value={fmtNum(trailerLineDetail.entryNo)} />
                <Field label="Line no." value={fmtNum(trailerLineDetail.lineNo)} />
                <Field label="Action code" value={trailerLineDetail.actionCode} />
                <Field label="Address name" value={trailerLineDetail.addressName} />
                <Field label="Trailer no." value={trailerLineDetail.trailerNo} />
                <Field label="Trailer description" value={trailerLineDetail.trailerDescription} />
                <Field label="Fleet no. trailer" value={trailerLineDetail.fleetNoTrailer} />
                <Field label="Registration no. trailer" value={trailerLineDetail.registrationNoTrailer} />
                <Field label="Container no." value={trailerLineDetail.containerNo} />
                <Field label="Container number" value={trailerLineDetail.containerNumber} />
                <Field label="Container no. 2" value={trailerLineDetail.containerNo2} />
                <Field label="Container number 2" value={trailerLineDetail.containerNumber2} />
                <Field label="Other equipment no." value={trailerLineDetail.otherEquipmentNo} />
                <Field label="Other equipment description" value={trailerLineDetail.otherEquipmentDescription} />
                <Field label="Chassis no." value={trailerLineDetail.chassisNo} />
                <Field label="Chassis description" value={trailerLineDetail.chassisDescription} />
                <Field label="Loaded" value={fmtBool(trailerLineDetail.loaded)} />
                <Field label="Communication partner" value={trailerLineDetail.communicationPartner} />
                <Field label="Created by" value={trailerLineDetail.createdBy} />
                <Field label="Creation date/time" value={fmtDate(trailerLineDetail.creationDatetime)} />
                <Field label="Last modified by" value={trailerLineDetail.lastModifiedBy} />
                <Field label="Last modification" value={fmtDate(trailerLineDetail.lastModificationDatetime)} />
              </FieldGrid>
            </div>
          </div>
        </div>
      )}

      {remarkDetail && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,.75)', zIndex: 9640, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ background: 'var(--bg1)', border: '1px solid var(--border)', borderRadius: 12, width: 'min(720px, 92vw)', maxHeight: '85vh', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            <div style={{ display: 'flex', alignItems: 'center', padding: '14px 20px', borderBottom: '1px solid var(--border)', flexShrink: 0 }}>
              <span style={{ fontWeight: 700, fontSize: 14, color: 'var(--text)' }}>Remark details</span>
              <div style={{ flex: 1 }} />
              <button type="button" onClick={() => setRemarkDetail(null)}
                style={{ background: 'transparent', border: 'none', color: 'var(--muted)', cursor: 'pointer', padding: 4 }}>
                <X size={16} />
              </button>
            </div>
            <div style={{ flex: 1, overflow: 'auto', padding: '16px 20px' }}>
              <FieldGrid>
                <Field label="ID" value={fmtNum(remarkDetail.id)} />
                <Field label="Entry no." value={fmtNum(remarkDetail.entryNo)} />
                <Field label="Line no." value={fmtNum(remarkDetail.lineNo)} />
                <Field label="Order line no." value={remarkDetail.orderLineNo == null ? '—' : String(remarkDetail.orderLineNo)} />
                <Field label="Remark type" value={remarkDetail.remarkType} />
                <Field label="Remarks" value={<span style={{ wordBreak: 'break-word', whiteSpace: 'pre-wrap' }}>{remarkDetail.remarks ?? '—'}</span>} />
                <Field label="External remark code" value={remarkDetail.externalRemarkCode} />
                <Field label="External order no." value={remarkDetail.externalOrderNo} />
                <Field label="External order line id" value={remarkDetail.externalOrderLineId} />
                <Field label="Communication partner" value={remarkDetail.communicationPartner} />
                <Field label="Import date/time" value={fmtDate(remarkDetail.importDatetime)} />
                <Field label="Processed date/time" value={fmtDate(remarkDetail.processedDatetime)} />
                <Field label="Created by" value={remarkDetail.createdBy} />
                <Field label="Creation date/time" value={fmtDate(remarkDetail.creationDatetime)} />
                <Field label="Last modified by" value={remarkDetail.lastModifiedBy} />
                <Field label="Last modification" value={fmtDate(remarkDetail.lastModificationDatetime)} />
              </FieldGrid>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
