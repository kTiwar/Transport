import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { BrainCircuit, Play, RefreshCw, Truck } from 'lucide-react';
import { routingApi, type RoutingRoute, type RoutingVehicle } from '../api/routingApi';
import { tmsOrdersApi, type TmsOrderLine } from '../api/tmsOrders';

type SelectableTmsLine = TmsOrderLine & { orderNo: string };

function fmtDistance(meters: number | null) {
  if (meters == null) return '-';
  return meters >= 1000 ? `${(meters / 1000).toFixed(1)} km` : `${meters.toFixed(0)} m`;
}

function fmtDuration(seconds: number | null) {
  if (seconds == null) return '-';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  return h > 0 ? `${h}h ${m}m` : `${m}m`;
}

function toDateOnly(v: string | null | undefined) {
  if (!v) return '';
  return v.slice(0, 10);
}

export default function OrderLinePlanningPage() {
  const qc = useQueryClient();

  const [routeDate, setRouteDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [selectedLineIds, setSelectedLineIds] = useState<Set<number>>(new Set());
  const [selectedVehicles, setSelectedVehicles] = useState<Set<number>>(new Set());

  // filters
  const [q, setQ] = useState('');
  const [orderNoFilter, setOrderNoFilter] = useState('');
  const [actionFilter, setActionFilter] = useState('');
  const [fromDateFilter, setFromDateFilter] = useState('');
  const [toDateFilter, setToDateFilter] = useState('');

  // pagination
  const [pageSize, setPageSize] = useState(25);
  const [pageNo, setPageNo] = useState(1);

  const tmsLinesQ = useQuery<SelectableTmsLine[]>({
    queryKey: ['planning-tms-order-lines'],
    queryFn: async () => {
      const page = await tmsOrdersApi.list(0, 200);
      const details = await Promise.all(page.content.map((o) => tmsOrdersApi.get(o.orderNo)));
      return details
        .flatMap((o) => (o.lines ?? []).map((line) => ({ ...line, orderNo: o.orderNo })))
        .sort((a, b) => (a.orderNo === b.orderNo ? a.lineNo - b.lineNo : a.orderNo.localeCompare(b.orderNo)));
    },
  });

  const vehiclesQ = useQuery({ queryKey: ['routing-vehicles'], queryFn: routingApi.listVehicles });
  const routesQ = useQuery({ queryKey: ['routing-routes'], queryFn: routingApi.listRoutes });

  const filteredLines = useMemo(() => {
    const rows = tmsLinesQ.data ?? [];
    const term = q.trim().toLowerCase();
    const orderTerm = orderNoFilter.trim().toLowerCase();
    const actionTerm = actionFilter.trim().toLowerCase();

    return rows.filter((line) => {
      const reqFrom = toDateOnly(line.requestedDatetimeFrom);
      const reqUntil = toDateOnly(line.requestedDatetimeUntil);

      const matchesQ =
        !term ||
        line.orderNo.toLowerCase().includes(term) ||
        String(line.lineNo).includes(term) ||
        (line.actionCode ?? '').toLowerCase().includes(term) ||
        (line.addressNo ?? '').toLowerCase().includes(term);

      const matchesOrder = !orderTerm || line.orderNo.toLowerCase().includes(orderTerm);
      const matchesAction = !actionTerm || (line.actionCode ?? '').toLowerCase().includes(actionTerm);
      const matchesFrom = !fromDateFilter || (reqFrom !== '' && reqFrom >= fromDateFilter);
      const matchesTo = !toDateFilter || (reqUntil !== '' && reqUntil <= toDateFilter);

      return matchesQ && matchesOrder && matchesAction && matchesFrom && matchesTo;
    });
  }, [tmsLinesQ.data, q, orderNoFilter, actionFilter, fromDateFilter, toDateFilter]);

  const totalPages = Math.max(1, Math.ceil(filteredLines.length / pageSize));
  const safePageNo = Math.min(pageNo, totalPages);
  const pageStart = (safePageNo - 1) * pageSize;
  const pagedLines = filteredLines.slice(pageStart, pageStart + pageSize);

  const optimizeMut = useMutation({
    mutationFn: async () => {
      const allLines = tmsLinesQ.data ?? [];
      const pickedLines = allLines.filter((l) => selectedLineIds.has(l.id));
      const uniqueOrderNos = [...new Set(pickedLines.map((l) => l.orderNo))];
      if (uniqueOrderNos.length === 0) throw new Error('Select at least one TMS order line');

      const importedOrders = await Promise.all(uniqueOrderNos.map((orderNo) => routingApi.importFromTms(orderNo)));
      const importedOrderIds = importedOrders.map((o) => o.id);
      return routingApi.optimize({ routeDate, orderIds: importedOrderIds, vehicleIds: [...selectedVehicles] });
    },
    onSuccess: (routes) => {
      toast.success(`Planned ${routes.length} route(s)`);
      qc.invalidateQueries({ queryKey: ['routing-routes'] });
      qc.invalidateQueries({ queryKey: ['routing-orders'] });
    },
    onError: (e: unknown) => {
      const msg = (e as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message
        ?? (e as { message?: string })?.message
        ?? 'Optimization failed';
      toast.error(msg);
    },
  });

  const toggleLine = (id: number) => {
    setSelectedLineIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  };

  const toggleVehicle = (id: number) => {
    setSelectedVehicles((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  };

  const clearFilters = () => {
    setQ('');
    setOrderNoFilter('');
    setActionFilter('');
    setFromDateFilter('');
    setToDateFilter('');
    setPageNo(1);
  };

  const canOptimize = selectedLineIds.size > 0 && selectedVehicles.size > 0 && !optimizeMut.isPending;
  const allVehicleIds = (vehiclesQ.data ?? []).map((v) => v.vehicleId);

  return (
    <div className="fade-in mx-auto max-w-[1500px] px-4 py-6">
      <header className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="flex h-11 w-11 items-center justify-center rounded-lg" style={{ background: 'linear-gradient(135deg, var(--cyan), var(--purple))' }}>
            <BrainCircuit className="h-5 w-5 text-white" />
          </div>
          <div>
            <h1 className="font-['Exo_2',sans-serif] text-xl font-bold text-[var(--text)]">OR-Tools Planning</h1>
            <p className="text-sm text-[var(--muted)]">Select TMS order lines and vehicles, then run route optimization</p>
          </div>
        </div>
        <button
          type="button"
          onClick={() => {
            qc.invalidateQueries({ queryKey: ['planning-tms-order-lines'] });
            qc.invalidateQueries({ queryKey: ['routing-vehicles'] });
            qc.invalidateQueries({ queryKey: ['routing-routes'] });
          }}
          className="inline-flex items-center gap-2 rounded-lg border border-[var(--border)] bg-[var(--card)] px-3 py-2 text-sm text-[var(--text)] hover:border-[var(--cyan)]"
        >
          <RefreshCw className="h-4 w-4" /> Refresh
        </button>
      </header>

      <div className="grid gap-6 lg:grid-cols-[1.35fr_.65fr]">
        <section className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-4" style={{ boxShadow: '0 4px 24px rgba(0,0,0,.2)' }}>
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-sm font-semibold uppercase tracking-wide text-[var(--cyan)]">TMS Order Lines</h2>
            <div className="flex items-center gap-3">
              <button type="button" onClick={() => setSelectedLineIds(new Set(pagedLines.map((l) => l.id)))} className="text-xs text-[var(--cyan)] hover:underline">Select page</button>
              <button type="button" onClick={clearFilters} className="text-xs text-[var(--muted)] hover:text-[var(--text)] hover:underline">Clear filters</button>
            </div>
          </div>

          <div className="mb-3 grid gap-2 md:grid-cols-5">
            <input value={q} onChange={(e) => { setQ(e.target.value); setPageNo(1); }} placeholder="Quick search" className="rounded-lg border border-[var(--border)] bg-[var(--bg2)] px-2 py-1.5 text-xs text-[var(--text)]" />
            <input value={orderNoFilter} onChange={(e) => { setOrderNoFilter(e.target.value); setPageNo(1); }} placeholder="Order no" className="rounded-lg border border-[var(--border)] bg-[var(--bg2)] px-2 py-1.5 text-xs text-[var(--text)]" />
            <input value={actionFilter} onChange={(e) => { setActionFilter(e.target.value); setPageNo(1); }} placeholder="Action" className="rounded-lg border border-[var(--border)] bg-[var(--bg2)] px-2 py-1.5 text-xs text-[var(--text)]" />
            <input type="date" value={fromDateFilter} onChange={(e) => { setFromDateFilter(e.target.value); setPageNo(1); }} className="rounded-lg border border-[var(--border)] bg-[var(--bg2)] px-2 py-1.5 text-xs text-[var(--text)]" />
            <input type="date" value={toDateFilter} onChange={(e) => { setToDateFilter(e.target.value); setPageNo(1); }} className="rounded-lg border border-[var(--border)] bg-[var(--bg2)] px-2 py-1.5 text-xs text-[var(--text)]" />
          </div>

          <div className="mb-2 flex items-center justify-between text-xs text-[var(--muted)]">
            <span>Rows: {filteredLines.length}</span>
            <div className="flex items-center gap-2">
              <span>Page size</span>
              <select
                value={pageSize}
                onChange={(e) => { setPageSize(Number(e.target.value)); setPageNo(1); }}
                className="rounded border border-[var(--border)] bg-[var(--bg2)] px-1 py-0.5 text-xs text-[var(--text)]"
              >
                <option value={10}>10</option>
                <option value={25}>25</option>
                <option value={50}>50</option>
              </select>
            </div>
          </div>

          <div className="max-h-[560px] overflow-auto rounded-lg border border-[var(--border)]">
            <table className="min-w-full text-sm">
              <thead className="sticky top-0 z-10 bg-[var(--bg2)] text-[var(--muted)]">
                <tr>
                  <th className="sticky left-0 z-20 bg-[var(--bg2)] px-3 py-2 text-left">Select</th>
                  <th className="sticky left-[88px] z-20 bg-[var(--bg2)] px-3 py-2 text-left">Order No</th>
                  <th className="px-3 py-2 text-left">Line No</th>
                  <th className="px-3 py-2 text-left">Action</th>
                  <th className="px-3 py-2 text-left">Address No</th>
                  <th className="px-3 py-2 text-left">Requested From</th>
                  <th className="px-3 py-2 text-left">Requested Until</th>
                </tr>
              </thead>
              <tbody>
                {tmsLinesQ.isLoading && <tr><td colSpan={7} className="px-3 py-4 text-center text-[var(--muted)]">Loading order lines...</td></tr>}
                {pagedLines.map((line) => (
                  <tr key={line.id} className="border-t border-[var(--border)] hover:bg-[var(--bg2)]/50">
                    <td className="sticky left-0 bg-[var(--card)] px-3 py-2"><input type="checkbox" checked={selectedLineIds.has(line.id)} onChange={() => toggleLine(line.id)} /></td>
                    <td className="sticky left-[88px] bg-[var(--card)] px-3 py-2 font-medium text-[var(--text)]">{line.orderNo}</td>
                    <td className="px-3 py-2">{line.lineNo}</td>
                    <td className="px-3 py-2">{line.actionCode ?? '-'}</td>
                    <td className="px-3 py-2">{line.addressNo ?? '-'}</td>
                    <td className="px-3 py-2">{line.requestedDatetimeFrom ?? '-'}</td>
                    <td className="px-3 py-2">{line.requestedDatetimeUntil ?? '-'}</td>
                  </tr>
                ))}
                {!tmsLinesQ.isLoading && pagedLines.length === 0 && <tr><td colSpan={7} className="px-3 py-4 text-center text-[var(--muted)]">No order lines match filters</td></tr>}
              </tbody>
            </table>
          </div>

          <div className="mt-2 flex items-center justify-between text-xs text-[var(--muted)]">
            <span>Page {safePageNo} of {totalPages}</span>
            <div className="flex items-center gap-2">
              <button type="button" onClick={() => setPageNo((p) => Math.max(1, p - 1))} disabled={safePageNo <= 1} className="rounded border border-[var(--border)] px-2 py-1 disabled:opacity-50">Prev</button>
              <button type="button" onClick={() => setPageNo((p) => Math.min(totalPages, p + 1))} disabled={safePageNo >= totalPages} className="rounded border border-[var(--border)] px-2 py-1 disabled:opacity-50">Next</button>
            </div>
          </div>
        </section>

        <section className="space-y-4">
          <div className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-4" style={{ boxShadow: '0 4px 24px rgba(0,0,0,.2)' }}>
            <div className="mb-2 flex items-center justify-between">
              <h2 className="text-sm font-semibold uppercase tracking-wide text-[var(--cyan)]">Vehicles</h2>
              <button type="button" onClick={() => setSelectedVehicles(new Set(allVehicleIds))} className="text-xs text-[var(--cyan)] hover:underline">Select all</button>
            </div>

            <div className="max-h-56 space-y-1 overflow-y-auto rounded-lg border border-[var(--border)] bg-[var(--bg0)] p-2">
              {vehiclesQ.isLoading && <p className="text-xs text-[var(--muted)]">Loading vehicles...</p>}
              {vehiclesQ.data?.map((v: RoutingVehicle) => (
                <label key={v.vehicleId} className="flex cursor-pointer items-center gap-2 text-sm text-[var(--text)]">
                  <input type="checkbox" checked={selectedVehicles.has(v.vehicleId)} onChange={() => toggleVehicle(v.vehicleId)} />
                  <span>{v.code} - {v.capacityWeightKg} kg</span>
                </label>
              ))}
            </div>

            <label className="mt-3 block text-xs text-[var(--muted)]">Route date</label>
            <input type="date" value={routeDate} onChange={(e) => setRouteDate(e.target.value)} className="mt-1 mb-3 w-full rounded-lg border border-[var(--border)] bg-[var(--bg2)] px-3 py-2 text-sm text-[var(--text)]" />

            <button type="button" onClick={() => optimizeMut.mutate()} disabled={!canOptimize} className="flex w-full items-center justify-center gap-2 rounded-lg bg-[var(--green)] py-2.5 text-sm font-bold text-white hover:opacity-95 disabled:opacity-50">
              <Play className="h-4 w-4" /> {optimizeMut.isPending ? 'Optimizing...' : 'Run OR-Tools'}
            </button>

            <p className="mt-2 text-xs text-[var(--muted)]">Selected lines: {selectedLineIds.size} | Selected vehicles: {selectedVehicles.size}</p>
          </div>

          <div className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-4" style={{ boxShadow: '0 4px 24px rgba(0,0,0,.2)' }}>
            <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-[var(--cyan)]">Latest planned routes</h2>
            <ul className="space-y-2">
              {routesQ.data?.slice(0, 8).map((r: RoutingRoute) => (
                <li key={r.routeId} className="rounded-lg border border-[var(--border)] bg-[var(--bg0)] p-3">
                  <div className="mb-1 flex items-center gap-2"><Truck className="h-4 w-4 text-[var(--cyan)]" /><span className="font-semibold text-[var(--text)]">Route #{r.routeId} - {r.vehicleCode}</span></div>
                  <p className="text-sm text-[var(--muted)]">{fmtDistance(r.totalDistanceM)} - {fmtDuration(r.totalDurationS)} - {r.stops?.length ?? 0} stops</p>
                </li>
              ))}
              {!routesQ.data?.length && <li className="text-sm text-[var(--muted)]">No routes yet.</li>}
            </ul>
          </div>
        </section>
      </div>
    </div>
  );
}
