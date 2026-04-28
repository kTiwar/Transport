import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  importOrdersApi,
  ImportOrder,
  ImportStats,
} from '../api/importOrders';
import {
  Play,
  RefreshCw,
  Plus,
  Search,
  CheckCircle,
  AlertCircle,
  Clock,
  Loader2,
  BarChart3,
  Trash2,
  Eye,
  Edit,
  ChevronLeft,
  ChevronRight,
  EyeOff,
} from 'lucide-react';

// ── Constants ──────────────────────────────────────────────────────────────

const STATUS_COLOR: Record<string, string> = {
  RECEIVED:          '#00d4ff',
  READY_TO_PROCESS:  '#00d4ff',
  PENDING:           '#f59e0b',
  PROCESSING:        '#8b5cf6',
  PROCESSED:         '#10b981',
  ERROR:             '#ef4444',
  VOID:              '#6b7280',
  SKIPPED:           '#6b7280',
};

const STATUS_ICON: Record<string, JSX.Element> = {
  RECEIVED:          <Clock size={11} />,
  READY_TO_PROCESS:  <Clock size={11} />,
  PENDING:           <Clock size={11} />,
  PROCESSING:        <Loader2 size={11} className="animate-spin" />,
  PROCESSED:         <CheckCircle size={11} />,
  ERROR:             <AlertCircle size={11} />,
  VOID:              <Clock size={11} />,
  SKIPPED:           <Clock size={11} />,
};

// Fields hidden by default (matching reference AL page)
const HIDDEN_COLUMNS = new Set([
  'shortcutReference3Code', 'description2', 'tripTypeNo', 'collectionDate',
  'deliveryDate', 'office', 'salesResponsible', 'originInfo', 'countryOfOrigin',
  'destinationInfo', 'countryOfDestination', 'neutralShipment', 'cashOnDeliveryType',
  'cashOnDeliveryAmount', 'userId', 'carrierNo', 'carrierName', 'vesselNameImport',
  'vesselNameExport', 'custServResponsible', 'tractionOrder',
]);

// ── Helpers ────────────────────────────────────────────────────────────────

const cell = (children: React.ReactNode, extra: React.CSSProperties = {}) => (
  <td style={{ padding: '9px 12px', fontSize: 12, ...extra }}>{children}</td>
);

const fmtDate = (v?: string | null) =>
  v ? new Date(v).toLocaleString() : '—';

// ── Component ──────────────────────────────────────────────────────────────

export default function ImportOrdersPage() {
  const navigate = useNavigate();

  const [orders, setOrders]     = useState<ImportOrder[]>([]);
  const [stats, setStats]       = useState<ImportStats | null>(null);
  const [loading, setLoading]   = useState(true);
  const [page, setPage]         = useState(0);
  const [totalPages, setTotal]  = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [processing, setProc]   = useState<number | null>(null);
  const [deleting, setDeleting] = useState<number | null>(null);
  const [bulkRunning, setBulk]  = useState(false);
  const [search, setSearch]     = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [showHidden, setShowHidden]   = useState(false);
  const [toast, setToast]       = useState<{ msg: string; ok: boolean } | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<ImportOrder | null>(null);

  const showToast = (msg: string, ok = true) => {
    setToast({ msg, ok });
    setTimeout(() => setToast(null), 3500);
  };

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [res, s] = await Promise.all([
        importOrdersApi.list(page, 20, search || undefined),
        importOrdersApi.getStats(),
      ]);
      setOrders(res.content);
      setTotal(res.totalPages);
      setTotalElements(res.totalElements);
      setStats(s);
    } catch { /* swallow */ }
    finally { setLoading(false); }
  }, [page, search]);

  useEffect(() => { load(); }, [load]);

  const handleSearch = () => {
    setSearch(searchInput);
    setPage(0);
  };

  const handleProcess = async (entryNo: number, e: React.MouseEvent) => {
    e.stopPropagation();
    setProc(entryNo);
    try {
      const result = await importOrdersApi.process(entryNo);
      const nos = result.tmsOrderNos?.length ? result.tmsOrderNos : result.tmsOrderNo ? [result.tmsOrderNo] : [];
      if (nos.length === 0) showToast('✓ Processed');
      else if (nos.length === 1) showToast(`✓ Processed → TMS ${nos[0]}`);
      else showToast(`✓ Processed → ${nos.length} TMS: ${nos.join(', ')}`);
      load();
    } catch (err: any) {
      showToast(err?.response?.data?.error || 'Processing failed', false);
    } finally { setProc(null); }
  };

  const handleDelete = async (order: ImportOrder, e: React.MouseEvent) => {
    e.stopPropagation();
    setConfirmDelete(order);
  };

  const confirmDeleteOrder = async () => {
    if (!confirmDelete) return;
    setDeleting(confirmDelete.entryNo);
    try {
      await importOrdersApi.delete(confirmDelete.entryNo);
      showToast(`✓ Order "${confirmDelete.externalOrderNo}" deleted`);
      load();
    } catch (err: any) {
      showToast(err?.response?.data?.error || 'Delete failed', false);
    } finally {
      setDeleting(null);
      setConfirmDelete(null);
    }
  };

  const handleBulk = async () => {
    setBulk(true);
    try {
      const r = await importOrdersApi.processAll();
      showToast(`✓ Bulk processed ${r.processedCount} orders`);
      load();
    } catch { showToast('Bulk processing failed', false); }
    finally { setBulk(false); }
  };

  const shouldShow = (col: string) => showHidden || !HIDDEN_COLUMNS.has(col);

  // ── Render ─────────────────────────────────────────────────────────────

  return (
    <div style={{ padding: '24px 28px', fontFamily: "'Exo 2',sans-serif", height: '100%', display: 'flex', flexDirection: 'column' }}>

      {/* Toast */}
      {toast && (
        <div style={{
          position: 'fixed', top: 20, right: 20, zIndex: 9999,
          background: toast.ok ? '#10b981' : '#ef4444',
          color: '#fff', padding: '10px 18px', borderRadius: 8,
          fontSize: 13, boxShadow: '0 4px 20px rgba(0,0,0,.4)',
        }}>{toast.msg}</div>
      )}

      {/* Delete Confirm Dialog */}
      {confirmDelete && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,.6)', zIndex: 9999,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <div style={{ background: 'var(--bg2)', border: '1px solid var(--border)', borderRadius: 12, padding: 28, maxWidth: 420, width: '90%' }}>
            <div style={{ fontSize: 16, fontWeight: 700, color: 'var(--text)', marginBottom: 12 }}>Delete Order</div>
            <div style={{ fontSize: 13, color: 'var(--text2)', marginBottom: 20 }}>
              Are you sure you want to delete order <strong style={{ color: '#ef4444' }}>&ldquo;{confirmDelete.externalOrderNo}&rdquo;</strong>? This action cannot be undone.
            </div>
            <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
              <button onClick={() => setConfirmDelete(null)}
                style={{ background: 'var(--bg3)', border: '1px solid var(--border)', borderRadius: 6, padding: '7px 16px', color: 'var(--text)', cursor: 'pointer', fontSize: 12 }}>
                Cancel
              </button>
              <button onClick={confirmDeleteOrder} disabled={!!deleting}
                style={{ background: '#ef4444', border: 'none', borderRadius: 6, padding: '7px 16px', color: '#fff', fontWeight: 700, cursor: 'pointer', fontSize: 12, display: 'flex', alignItems: 'center', gap: 6 }}>
                {deleting ? <Loader2 size={12} className="animate-spin" /> : <Trash2 size={12} />} Delete
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 20, flexShrink: 0 }}>
        <BarChart3 size={20} color="var(--cyan)" />
        <h1 style={{ margin: 0, fontSize: 20, color: 'var(--text)', fontWeight: 700 }}>Import Orders</h1>
        {totalElements > 0 && (
          <span style={{ fontSize: 11, color: 'var(--muted)', background: 'var(--bg3)', padding: '2px 8px', borderRadius: 4 }}>
            {totalElements.toLocaleString()} total
          </span>
        )}
        <div style={{ flex: 1 }} />
        <button onClick={() => setShowHidden(v => !v)}
          style={{ background: 'var(--bg3)', border: '1px solid var(--border)', borderRadius: 6, padding: '6px 12px', color: showHidden ? 'var(--cyan)' : 'var(--muted)', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6, fontSize: 12 }}
          title={showHidden ? 'Hide extra columns' : 'Show all columns'}>
          {showHidden ? <EyeOff size={12} /> : <Eye size={12} />}
          {showHidden ? 'Less' : 'More'}
        </button>
        <button onClick={load} style={{ background: 'var(--bg3)', border: '1px solid var(--border)', borderRadius: 6, padding: '6px 12px', color: 'var(--text)', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6, fontSize: 12 }}>
          <RefreshCw size={12} /> Refresh
        </button>
        <button onClick={handleBulk} disabled={bulkRunning}
          style={{ background: 'var(--bg3)', border: '1px solid var(--border)', borderRadius: 6, padding: '6px 12px', color: 'var(--orange)', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6, fontSize: 12 }}>
          {bulkRunning ? <Loader2 size={12} className="animate-spin" /> : <Play size={12} />} Process All
        </button>
        <button onClick={() => navigate('/import-orders/new')}
          style={{ background: 'var(--cyan)', border: 'none', borderRadius: 6, padding: '6px 14px', color: '#000', fontWeight: 700, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6, fontSize: 12 }}>
          <Plus size={12} /> New Order
        </button>
      </div>

      {/* Stats */}
      {stats && (
        <div style={{ display: 'flex', gap: 12, marginBottom: 20, flexShrink: 0 }}>
          {([
            ['Received',   stats.received,   '#00d4ff'],
            ['Processing', stats.processing, '#8b5cf6'],
            ['Processed',  stats.processed,  '#10b981'],
            ['Errors',     stats.error,      '#ef4444'],
          ] as [string, number, string][]).map(([label, value, color]) => (
            <div key={label} style={{
              background: 'var(--bg2)', border: `1px solid ${color}40`,
              borderRadius: 10, padding: '12px 18px', flex: 1,
            }}>
              <div style={{ fontSize: 22, fontWeight: 800, color }}>{value ?? '—'}</div>
              <div style={{ fontSize: 10, color: 'var(--muted)', textTransform: 'uppercase', letterSpacing: '.5px' }}>{label}</div>
            </div>
          ))}
        </div>
      )}

      {/* Search bar */}
      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexShrink: 0 }}>
        <div style={{ flex: 1, position: 'relative' }}>
          <Search size={13} style={{ position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)', color: 'var(--muted)' }} />
          <input
            value={searchInput}
            onChange={e => setSearchInput(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleSearch()}
            placeholder="Search orders (ext. order no, customer, partner…)"
            style={{
              width: '100%', boxSizing: 'border-box',
              background: 'var(--bg2)', border: '1px solid var(--border)', borderRadius: 6,
              padding: '7px 12px 7px 32px', color: 'var(--text)', fontSize: 12, outline: 'none',
            }}
          />
        </div>
        <button onClick={handleSearch}
          style={{ background: 'var(--bg3)', border: '1px solid var(--border)', borderRadius: 6, padding: '7px 14px', color: 'var(--text)', cursor: 'pointer', fontSize: 12 }}>
          Search
        </button>
      </div>

      {/* Table */}
      <div style={{ flex: 1, minHeight: 0, background: 'var(--bg2)', border: '1px solid var(--border)', borderRadius: 10, overflow: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <thead>
            <tr style={{ background: 'var(--bg3)', borderBottom: '1px solid var(--border)', position: 'sticky', top: 0, zIndex: 1 }}>
              {[
                ['#',               true],
                ['Partner',         true],
                ['External Order No.', true],
                ['Customer',        true],
                ['Tx Type',         true],
                ['Status',          true],
                ['TMS Order No.',   true],
                ['Import Date',     true],
                ['Order Date',      shouldShow('orderDate')],
                ['Collection Date', shouldShow('collectionDate')],
                ['Delivery Date',   shouldShow('deliveryDate')],
                ['Office',          shouldShow('office')],
                ['Transport Type',  shouldShow('transportType')],
                ['Carrier',         shouldShow('carrierName')],
                ['Error',           shouldShow('errorMessage')],
                ['Actions',         true],
              ].filter(([, show]) => show).map(([h]) => (
                <th key={h as string} style={{ padding: '10px 12px', textAlign: 'left', color: 'var(--muted)', fontWeight: 700, fontSize: 10, textTransform: 'uppercase', letterSpacing: '.5px', whiteSpace: 'nowrap' }}>
                  {h as string}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={20} style={{ textAlign: 'center', padding: 48, color: 'var(--muted)' }}>
                <Loader2 size={22} className="animate-spin" style={{ display: 'inline-block' }} />
              </td></tr>
            ) : orders.length === 0 ? (
              <tr><td colSpan={20} style={{ textAlign: 'center', padding: 48, color: 'var(--muted)', fontSize: 13 }}>
                No import orders found
              </td></tr>
            ) : orders.map(o => {
              const color = STATUS_COLOR[o.status] || 'var(--text2)';
              return (
                <tr key={o.entryNo}
                  onClick={() => navigate(`/import-orders/${o.entryNo}`)}
                  style={{ borderBottom: '1px solid var(--border)', cursor: 'pointer', transition: 'background .1s' }}
                  onMouseEnter={e => (e.currentTarget.style.background = 'var(--bg3)')}
                  onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}>

                  {cell(<span style={{ fontFamily: "'Fira Code',monospace", color: 'var(--muted)' }}>{o.entryNo}</span>)}
                  {cell(<span style={{ color: 'var(--cyan)' }}>{o.communicationPartner}</span>)}
                  {cell(<span style={{ fontFamily: "'Fira Code',monospace" }}>{o.externalOrderNo || '—'}</span>)}
                  {cell(o.customerName || o.externalCustomerNo || '—', { color: 'var(--text2)' })}
                  {cell(
                    <span style={{ fontSize: 10, background: 'var(--bg3)', padding: '2px 7px', borderRadius: 4, color: 'var(--text2)', fontFamily: "'Fira Code',monospace" }}>
                      {o.transactionType?.replace('_ORDER', '') || '—'}
                    </span>
                  )}
                  {cell(
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4, color, fontSize: 11, background: `${color}18`, border: `1px solid ${color}40`, padding: '2px 7px', borderRadius: 4 }}>
                      {STATUS_ICON[o.status]} {o.status}
                    </span>
                  )}
                  {cell(<span style={{ fontFamily: "'Fira Code',monospace", color: '#10b981' }}>{o.tmsOrderNo || '—'}</span>)}
                  {cell(<span style={{ color: 'var(--muted)', fontSize: 11 }}>{fmtDate(o.receivedAt)}</span>)}
                  {shouldShow('orderDate') && cell(<span style={{ color: 'var(--muted)', fontSize: 11 }}>{fmtDate(o.orderDate)}</span>)}
                  {shouldShow('collectionDate') && cell(<span style={{ color: 'var(--muted)', fontSize: 11 }}>{fmtDate(o.collectionDate)}</span>)}
                  {shouldShow('deliveryDate') && cell(<span style={{ color: 'var(--muted)', fontSize: 11 }}>{fmtDate(o.deliveryDate)}</span>)}
                  {shouldShow('office') && cell(o.office || '—', { color: 'var(--text2)' })}
                  {shouldShow('transportType') && cell(o.transportType || '—', { color: 'var(--text2)' })}
                  {shouldShow('carrierName') && cell(o.carrierName || o.carrierNo || '—', { color: 'var(--text2)' })}
                  {shouldShow('errorMessage') && cell(
                    o.errorMessage
                      ? <span style={{ color: '#ef4444', fontSize: 11 }} title={o.errorMessage}>{o.errorMessage.slice(0, 40)}{o.errorMessage.length > 40 ? '…' : ''}</span>
                      : '—'
                  )}
                  <td style={{ padding: '9px 12px' }} onClick={e => e.stopPropagation()}>
                    <div style={{ display: 'flex', gap: 4 }}>
                      <button onClick={() => navigate(`/import-orders/${o.entryNo}`)} title="View"
                        style={{ background: 'transparent', border: '1px solid var(--border)', borderRadius: 5, padding: '4px 7px', color: 'var(--text2)', cursor: 'pointer', display: 'inline-flex' }}>
                        <Eye size={11} />
                      </button>
                      <button onClick={() => navigate(`/import-orders/${o.entryNo}/edit`)} title="Edit"
                        style={{ background: 'transparent', border: '1px solid var(--border)', borderRadius: 5, padding: '4px 7px', color: 'var(--cyan)', cursor: 'pointer', display: 'inline-flex' }}>
                        <Edit size={11} />
                      </button>
                      {(o.status === 'RECEIVED' || o.status === 'READY_TO_PROCESS' || o.status === 'ERROR') && (
                        <button onClick={e => handleProcess(o.entryNo, e)} disabled={processing === o.entryNo} title="Process"
                          style={{ background: 'var(--cyan)', border: 'none', borderRadius: 5, padding: '4px 7px', color: '#000', cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: 3, fontSize: 11, fontWeight: 700 }}>
                          {processing === o.entryNo ? <Loader2 size={10} className="animate-spin" /> : <Play size={10} />}
                        </button>
                      )}
                      <button onClick={e => handleDelete(o, e)} title="Delete"
                        style={{ background: 'transparent', border: '1px solid #ef444440', borderRadius: 5, padding: '4px 7px', color: '#ef4444', cursor: 'pointer', display: 'inline-flex' }}>
                        <Trash2 size={11} />
                      </button>
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 8, marginTop: 14, flexShrink: 0 }}>
          <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
            style={{ background: 'var(--bg3)', border: '1px solid var(--border)', borderRadius: 5, padding: '5px 10px', color: 'var(--text)', cursor: 'pointer', display: 'flex', alignItems: 'center' }}>
            <ChevronLeft size={14} />
          </button>
          <span style={{ color: 'var(--muted)', fontSize: 12 }}>Page {page + 1} / {totalPages}</span>
          <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}
            style={{ background: 'var(--bg3)', border: '1px solid var(--border)', borderRadius: 5, padding: '5px 10px', color: 'var(--text)', cursor: 'pointer', display: 'flex', alignItems: 'center' }}>
            <ChevronRight size={14} />
          </button>
        </div>
      )}
    </div>
  );
}
