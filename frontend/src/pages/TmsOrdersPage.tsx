import { useState, useEffect, useCallback, Fragment } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { tmsOrdersApi, TmsOrder } from '../api/tmsOrders';
import { Truck, RefreshCw, Loader2, Package, MapPin, Hash, ChevronDown, ChevronRight, X } from 'lucide-react';

export default function TmsOrdersPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const impEntryNoRaw = searchParams.get('impEntryNo');
  const impEntryNo =
    impEntryNoRaw != null && impEntryNoRaw !== '' ? parseInt(impEntryNoRaw, 10) : undefined;
  const impFilterActive = impEntryNo != null && !Number.isNaN(impEntryNo);

  const [orders, setOrders]   = useState<TmsOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage]       = useState(0);
  const [totalPages, setTotal]= useState(0);
  const [expanded, setExpanded] = useState<string | null>(null);
  const [detail, setDetail]   = useState<TmsOrder | null>(null);
  const [detailLoading, setDL]= useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const r = await tmsOrdersApi.list(page, 20, impFilterActive ? impEntryNo : undefined);
      setOrders(r.content);
      setTotal(r.totalPages);
    } finally { setLoading(false); }
  }, [page, impFilterActive, impEntryNo]);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    setPage(0);
  }, [impEntryNoRaw]);

  const toggleExpand = async (orderNo: string) => {
    if (expanded === orderNo) { setExpanded(null); setDetail(null); return; }
    setExpanded(orderNo);
    setDL(true);
    try {
      const d = await tmsOrdersApi.get(orderNo);
      setDetail(d);
    } finally { setDL(false); }
  };

  return (
    <div style={{ padding: '24px 28px', fontFamily: "'Exo 2',sans-serif" }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 20, flexWrap: 'wrap' }}>
        <Truck size={20} color="var(--green)" />
        <h1 style={{ margin: 0, fontSize: 20, color: 'var(--text)', fontWeight: 700 }}>TMS Orders</h1>
        <div style={{ flex: 1 }} />
        <button onClick={load} style={{ background: 'var(--bg3)', border: '1px solid var(--border)', borderRadius: 6, padding: '6px 12px', color: 'var(--text)', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6, fontSize: 12 }}>
          <RefreshCw size={12} /> Refresh
        </button>
      </div>

      {impFilterActive && (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 12,
            marginBottom: 14,
            padding: '10px 14px',
            background: 'rgba(16,185,129,.08)',
            border: '1px solid rgba(16,185,129,.35)',
            borderRadius: 8,
            fontSize: 12,
          }}
        >
          <span style={{ color: 'var(--text)' }}>
            Showing TMS orders linked to import entry{' '}
            <Link to={`/import-orders/${impEntryNo}`} style={{ color: 'var(--cyan)', fontFamily: 'monospace', fontWeight: 600 }}>
              #{impEntryNo}
            </Link>
          </span>
          <button
            type="button"
            onClick={() => {
              const next = new URLSearchParams(searchParams);
              next.delete('impEntryNo');
              setSearchParams(next, { replace: true });
            }}
            style={{
              marginLeft: 'auto',
              display: 'inline-flex',
              alignItems: 'center',
              gap: 4,
              background: 'var(--bg3)',
              border: '1px solid var(--border)',
              borderRadius: 6,
              padding: '4px 10px',
              color: 'var(--muted)',
              cursor: 'pointer',
              fontSize: 11,
            }}
          >
            <X size={12} /> Show all TMS orders
          </button>
        </div>
      )}

      {/* Table */}
      <div style={{ background: 'var(--bg2)', border: '1px solid var(--border)', borderRadius: 10, overflow: 'hidden' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <thead>
            <tr style={{ background: 'var(--bg3)', borderBottom: '1px solid var(--border)' }}>
              {['', 'Order No.', 'Customer', 'Transport Type', 'Office', 'Partner', 'Source', 'Status', 'Order Date', 'Imp. entry#', 'Imp. ext. id'].map(h => (
                <th key={h} style={{ padding: '10px 12px', textAlign: 'left', color: 'var(--muted)', fontSize: 10, textTransform: 'uppercase', letterSpacing: '.5px', whiteSpace: 'nowrap' }}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={11} style={{ textAlign: 'center', padding: 40, color: 'var(--muted)' }}><Loader2 size={20} /></td></tr>
            ) : orders.length === 0 ? (
              <tr><td colSpan={11} style={{ textAlign: 'center', padding: 40, color: 'var(--muted)', fontSize: 13 }}>No TMS orders yet. Process import orders first.</td></tr>
            ) : orders.map(o => (
              <Fragment key={o.orderNo}>
                <tr
                  onClick={() => toggleExpand(o.orderNo)}
                  style={{ borderBottom: expanded === o.orderNo ? 'none' : '1px solid var(--border)', cursor: 'pointer', background: expanded === o.orderNo ? 'rgba(16,185,129,.05)' : 'transparent', transition: 'background .1s' }}
                  onMouseEnter={e => { if (expanded !== o.orderNo) e.currentTarget.style.background = 'var(--bg3)'; }}
                  onMouseLeave={e => { if (expanded !== o.orderNo) e.currentTarget.style.background = 'transparent'; }}>
                  <td style={{ padding: '9px 12px', color: 'var(--muted)' }}>
                    {expanded === o.orderNo ? <ChevronDown size={12} /> : <ChevronRight size={12} />}
                  </td>
                  <td style={{ padding: '9px 12px', color: '#10b981', fontFamily: "'Fira Code',monospace", fontWeight: 700 }}>{o.orderNo}</td>
                  <td style={{ padding: '9px 12px', color: 'var(--cyan)' }}>{o.customerNo || '—'}</td>
                  <td style={{ padding: '9px 12px' }}>{o.transportType || '—'}</td>
                  <td style={{ padding: '9px 12px' }}>{o.office || '—'}</td>
                  <td style={{ padding: '9px 12px', color: 'var(--text2)' }}>{o.communicationPartner || '—'}</td>
                  <td style={{ padding: '9px 12px' }}>
                    <span style={{ fontSize: 10, background: 'var(--bg3)', padding: '2px 7px', borderRadius: 4, color: 'var(--text2)' }}>{o.source}</span>
                  </td>
                  <td style={{ padding: '9px 12px' }}>
                    <span style={{ fontSize: 10, color: '#10b981', background: 'rgba(16,185,129,.1)', border: '1px solid rgba(16,185,129,.3)', padding: '2px 7px', borderRadius: 4 }}>{o.status}</span>
                  </td>
                  <td style={{ padding: '9px 12px', color: 'var(--muted)', fontSize: 11 }}>{o.orderDate ? new Date(o.orderDate).toLocaleString() : '—'}</td>
                  <td style={{ padding: '9px 12px', fontFamily: "'Fira Code',monospace", color: 'var(--cyan)', fontSize: 11 }}>{o.impEntryNo ?? '—'}</td>
                  <td style={{ padding: '9px 12px', fontFamily: "'Fira Code',monospace", color: 'var(--text2)', fontSize: 11 }} title="Linked import staging external id / partition">{o.importExternalOrderNo ?? '—'}</td>
                </tr>

                {/* Expanded Detail Row */}
                {expanded === o.orderNo && (
                  <tr key={`${o.orderNo}-detail`} style={{ borderBottom: '1px solid var(--border)' }}>
                    <td colSpan={11} style={{ padding: '0 0 0 24px', background: 'rgba(16,185,129,.03)' }}>
                      {detailLoading ? (
                        <div style={{ padding: 20, color: 'var(--muted)' }}><Loader2 size={16} /></div>
                      ) : detail && (
                        <div style={{ padding: '12px 16px 16px' }}>
                          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 16 }}>
                            {/* Lines */}
                            <div>
                              <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8, color: 'var(--orange)', fontSize: 11, fontWeight: 700 }}>
                                <MapPin size={12} /> ORDER LINES ({detail.lines?.length ?? 0})
                              </div>
                              {(detail.lines || []).map(l => (
                                <div key={l.id} style={{ background: 'var(--bg3)', borderRadius: 6, padding: '8px 10px', marginBottom: 6, fontSize: 11 }}>
                                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                    <span style={{ color: 'var(--cyan)', fontFamily: "'Fira Code',monospace" }}>Line {l.lineNo}</span>
                                    <span style={{ color: 'var(--orange)' }}>{l.actionCode}</span>
                                  </div>
                                  <div style={{ color: 'var(--text2)', marginTop: 3 }}>Address: <span style={{ fontFamily: "'Fira Code',monospace" }}>{l.addressNo || '—'}</span></div>
                                  {l.initialDatetimeFrom && <div style={{ color: 'var(--muted)', fontSize: 10 }}>From: {new Date(l.initialDatetimeFrom).toLocaleString()}</div>}
                                </div>
                              ))}
                            </div>
                            {/* Cargo */}
                            <div>
                              <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8, color: 'var(--green)', fontSize: 11, fontWeight: 700 }}>
                                <Package size={12} /> CARGO ({detail.cargoItems?.length ?? 0})
                              </div>
                              {(detail.cargoItems || []).map(c => (
                                <div key={c.id} style={{ background: 'var(--bg3)', borderRadius: 6, padding: '8px 10px', marginBottom: 6, fontSize: 11 }}>
                                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                    <span style={{ fontFamily: "'Fira Code',monospace", color: 'var(--text)' }}>{c.goodNo}</span>
                                    <span style={{ color: 'var(--orange)' }}>{c.quantity} {c.unitOfMeasureCode}</span>
                                  </div>
                                  {c.description && <div style={{ color: 'var(--text2)', marginTop: 3 }}>{c.description}</div>}
                                  <div style={{ color: 'var(--muted)', marginTop: 3, fontSize: 10 }}>Type: {c.goodTypeCode || '—'} / Sub: {c.goodSubTypeCode || '—'}</div>
                                  {c.dangerousGoods && <div style={{ color: '#ef4444', fontSize: 10, marginTop: 3 }}>⚠ Dangerous: {c.adrType}</div>}
                                </div>
                              ))}
                            </div>
                            {/* References */}
                            <div>
                              <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8, color: 'var(--purple)', fontSize: 11, fontWeight: 700 }}>
                                <Hash size={12} /> REFERENCES ({detail.references?.length ?? 0})
                              </div>
                              {(detail.references || []).map(r => (
                                <div key={r.id} style={{ background: 'var(--bg3)', borderRadius: 6, padding: '8px 10px', marginBottom: 6, fontSize: 11 }}>
                                  <div style={{ color: 'var(--orange)' }}>{r.referenceCode}</div>
                                  <div style={{ fontFamily: "'Fira Code',monospace", color: 'var(--text)', marginTop: 2 }}>{r.reference}</div>
                                  <div style={{ color: 'var(--muted)', fontSize: 10, marginTop: 2 }}>{r.orderLineNo === 0 ? 'Order-level' : `Line ${r.orderLineNo}`}</div>
                                </div>
                              ))}
                            </div>
                          </div>
                        </div>
                      )}
                    </td>
                  </tr>
                )}
              </Fragment>
            ))}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'center', gap: 8, marginTop: 16 }}>
          <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
            style={{ background: 'var(--bg3)', border: '1px solid var(--border)', borderRadius: 5, padding: '5px 12px', color: 'var(--text)', cursor: 'pointer' }}>←</button>
          <span style={{ color: 'var(--muted)', fontSize: 12, padding: '5px 8px' }}>Page {page + 1} / {totalPages}</span>
          <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}
            style={{ background: 'var(--bg3)', border: '1px solid var(--border)', borderRadius: 5, padding: '5px 12px', color: 'var(--text)', cursor: 'pointer' }}>→</button>
        </div>
      )}
    </div>
  );
}
