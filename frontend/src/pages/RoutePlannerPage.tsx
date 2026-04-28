import { useCallback, useEffect, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { MapContainer, TileLayer, Marker, Popup, Polyline, useMap } from 'react-leaflet';
import L from 'leaflet';
import toast from 'react-hot-toast';
import { Navigation, RefreshCw, Play, Download } from 'lucide-react';
import {
  routingApi,
  type RoutingOrder,
  type RoutingRoute,
  type RoutingStop,
  type RoutingVehicle,
} from '../api/routingApi';

function FitBounds({ positions }: { positions: [number, number][] }) {
  const map = useMap();
  useEffect(() => {
    if (positions.length === 0) return;
    if (positions.length === 1) {
      map.setView(positions[0], 11);
      return;
    }
    map.fitBounds(L.latLngBounds(positions), { padding: [40, 40], maxZoom: 12 });
  }, [map, positions]);
  return null;
}

function stopLabel(s: RoutingStop) {
  return `${s.sequenceNumber}. ${s.stopType}${s.orderId != null ? ` · order #${s.orderId}` : ''}`;
}

export default function RoutePlannerPage() {
  const qc = useQueryClient();
  const [selectedRouteId, setSelectedRouteId] = useState<number | null>(null);
  const [selectedOrders, setSelectedOrders] = useState<Set<number>>(new Set());
  const [selectedVehicles, setSelectedVehicles] = useState<Set<number>>(new Set());
  const [routeDate, setRouteDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [tmsImportNo, setTmsImportNo] = useState('');

  const ordersQ = useQuery({ queryKey: ['routing-orders'], queryFn: routingApi.listOrders });
  const vehiclesQ = useQuery({ queryKey: ['routing-vehicles'], queryFn: routingApi.listVehicles });
  const routesQ = useQuery({ queryKey: ['routing-routes'], queryFn: routingApi.listRoutes });

  const selectedRoute = useMemo(() => {
    if (selectedRouteId == null) return null;
    return routesQ.data?.find((r) => r.routeId === selectedRouteId) ?? null;
  }, [routesQ.data, selectedRouteId]);

  const linePositions = useMemo((): [number, number][] => {
    if (!selectedRoute?.stops?.length) return [];
    return selectedRoute.stops
      .filter((s) => s.latitude != null && s.longitude != null)
      .sort((a, b) => a.sequenceNumber - b.sequenceNumber)
      .map((s) => [s.latitude!, s.longitude!] as [number, number]);
  }, [selectedRoute]);

  const mapCenter = useMemo((): [number, number] => {
    if (linePositions.length) return linePositions[0];
    return [55.6761, 12.5683];
  }, [linePositions]);

  const optimizeMut = useMutation({
    mutationFn: routingApi.optimize,
    onSuccess: (routes) => {
      toast.success(`Planned ${routes.length} route(s).`);
      qc.invalidateQueries({ queryKey: ['routing-routes'] });
      qc.invalidateQueries({ queryKey: ['routing-orders'] });
      if (routes[0]) setSelectedRouteId(routes[0].routeId);
    },
    onError: (e: unknown) => {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message;
      toast.error(msg ?? 'Optimization failed');
    },
  });

  const importMut = useMutation({
    mutationFn: routingApi.importFromTms,
    onSuccess: (o) => {
      toast.success(`Imported routing order #${o.id} from TMS`);
      qc.invalidateQueries({ queryKey: ['routing-orders'] });
      setSelectedOrders((prev) => new Set(prev).add(o.id));
    },
    onError: () => toast.error('TMS import failed (need 2 lines + addresses)'),
  });

  const toggleOrder = useCallback((id: number) => {
    setSelectedOrders((prev) => {
      const n = new Set(prev);
      if (n.has(id)) n.delete(id);
      else n.add(id);
      return n;
    });
  }, []);

  const toggleVehicle = useCallback((id: number) => {
    setSelectedVehicles((prev) => {
      const n = new Set(prev);
      if (n.has(id)) n.delete(id);
      else n.add(id);
      return n;
    });
  }, []);

  const runOptimize = () => {
    if (selectedOrders.size === 0 || selectedVehicles.size === 0) {
      toast.error('Select at least one order and one vehicle.');
      return;
    }
    optimizeMut.mutate({
      routeDate,
      orderIds: [...selectedOrders],
      vehicleIds: [...selectedVehicles],
      depotLatitude: 55.6761,
      depotLongitude: 12.5683,
    });
  };

  return (
    <div className="fade-in mx-auto max-w-[1400px] px-4 py-6">
      <header className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div
            className="flex h-11 w-11 items-center justify-center rounded-lg"
            style={{ background: 'linear-gradient(135deg, var(--cyan), var(--purple))' }}
          >
            <Navigation className="h-5 w-5 text-white" />
          </div>
          <div>
            <h1 className="font-['Exo_2',sans-serif] text-xl font-bold text-[var(--text)]">Route planner</h1>
            <p className="text-sm text-[var(--muted)]">
              OSM + OR-Tools routes — map stops, run optimization, Kafka hook on backend
            </p>
          </div>
        </div>
        <button
          type="button"
          onClick={() => {
            qc.invalidateQueries({ queryKey: ['routing-routes'] });
            qc.invalidateQueries({ queryKey: ['routing-orders'] });
            qc.invalidateQueries({ queryKey: ['routing-vehicles'] });
          }}
          className="inline-flex items-center gap-2 rounded-lg border border-[var(--border)] bg-[var(--card)] px-3 py-2 text-sm text-[var(--text)] hover:border-[var(--cyan)]"
        >
          <RefreshCw className="h-4 w-4" />
          Refresh data
        </button>
      </header>

      <div className="grid gap-6 lg:grid-cols-[minmax(280px,360px)_1fr]">
        <div className="space-y-4">
          <section
            className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-4"
            style={{ boxShadow: '0 4px 24px rgba(0,0,0,.2)' }}
          >
            <h2 className="mb-2 font-['Exo_2',sans-serif] text-sm font-semibold uppercase tracking-wide text-[var(--cyan)]">
              Import from TMS order
            </h2>
            <div className="flex gap-2">
              <input
                value={tmsImportNo}
                onChange={(e) => setTmsImportNo(e.target.value)}
                placeholder="Order no. e.g. ORD-…"
                className="min-w-0 flex-1 rounded-lg border border-[var(--border)] bg-[var(--bg2)] px-3 py-2 text-sm text-[var(--text)]"
              />
              <button
                type="button"
                disabled={importMut.isPending || !tmsImportNo.trim()}
                onClick={() => importMut.mutate(tmsImportNo.trim())}
                className="inline-flex shrink-0 items-center gap-1 rounded-lg bg-[var(--cyan)] px-3 py-2 text-sm font-semibold text-[var(--bg0)] disabled:opacity-50"
              >
                <Download className="h-4 w-4" />
                Import
              </button>
            </div>
          </section>

          <section
            className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-4"
            style={{ boxShadow: '0 4px 24px rgba(0,0,0,.2)' }}
          >
            <h2 className="mb-2 font-['Exo_2',sans-serif] text-sm font-semibold uppercase tracking-wide text-[var(--cyan)]">
              Optimize
            </h2>
            <label className="mb-2 block text-xs text-[var(--muted)]">Route date</label>
            <input
              type="date"
              value={routeDate}
              onChange={(e) => setRouteDate(e.target.value)}
              className="mb-3 w-full rounded-lg border border-[var(--border)] bg-[var(--bg2)] px-3 py-2 text-sm text-[var(--text)]"
            />
            <p className="mb-1 text-xs text-[var(--muted)]">Orders ({selectedOrders.size} selected)</p>
            <div className="mb-3 max-h-36 space-y-1 overflow-y-auto rounded-lg border border-[var(--border)] bg-[var(--bg0)] p-2">
              {ordersQ.isLoading && <p className="text-xs text-[var(--muted)]">Loading…</p>}
              {ordersQ.data?.map((o: RoutingOrder) => (
                <label key={o.id} className="flex cursor-pointer items-center gap-2 text-sm text-[var(--text)]">
                  <input
                    type="checkbox"
                    checked={selectedOrders.has(o.id)}
                    onChange={() => toggleOrder(o.id)}
                  />
                  <span className="truncate">
                    #{o.id} {o.tmsOrderNo ? `(${o.tmsOrderNo})` : ''} — {o.weightKg} kg
                  </span>
                </label>
              ))}
            </div>
            <p className="mb-1 text-xs text-[var(--muted)]">Vehicles ({selectedVehicles.size} selected)</p>
            <div className="mb-3 max-h-28 space-y-1 overflow-y-auto rounded-lg border border-[var(--border)] bg-[var(--bg0)] p-2">
              {vehiclesQ.isLoading && <p className="text-xs text-[var(--muted)]">Loading…</p>}
              {vehiclesQ.data?.map((v: RoutingVehicle) => (
                <label key={v.vehicleId} className="flex cursor-pointer items-center gap-2 text-sm text-[var(--text)]">
                  <input
                    type="checkbox"
                    checked={selectedVehicles.has(v.vehicleId)}
                    onChange={() => toggleVehicle(v.vehicleId)}
                  />
                  <span>
                    {v.code} · {v.capacityWeightKg} kg
                  </span>
                </label>
              ))}
            </div>
            <button
              type="button"
              disabled={optimizeMut.isPending}
              onClick={runOptimize}
              className="flex w-full items-center justify-center gap-2 rounded-lg bg-[var(--green)] py-2.5 text-sm font-bold text-white hover:opacity-95 disabled:opacity-50"
            >
              <Play className="h-4 w-4" />
              {optimizeMut.isPending ? 'Optimizing…' : 'Run OR-Tools optimize'}
            </button>
          </section>

          <section
            className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-4"
            style={{ boxShadow: '0 4px 24px rgba(0,0,0,.2)' }}
          >
            <h2 className="mb-2 font-['Exo_2',sans-serif] text-sm font-semibold uppercase tracking-wide text-[var(--cyan)]">
              Planned routes
            </h2>
            <ul className="max-h-48 space-y-1 overflow-y-auto text-sm">
              {routesQ.data?.map((r: RoutingRoute) => (
                <li key={r.routeId}>
                  <button
                    type="button"
                    onClick={() => setSelectedRouteId(r.routeId)}
                    className={`w-full rounded-lg px-2 py-1.5 text-left transition-colors ${
                      selectedRouteId === r.routeId
                        ? 'bg-[rgba(0,212,255,.15)] text-[var(--cyan)]'
                        : 'text-[var(--text2)] hover:bg-[var(--bg2)]'
                    }`}
                  >
                    #{r.routeId} · {r.vehicleCode} · {(r.totalDistanceM ?? 0).toFixed(0)} m
                  </button>
                </li>
              ))}
              {!routesQ.data?.length && !routesQ.isLoading && (
                <li className="text-xs text-[var(--muted)]">No routes yet — optimize to create.</li>
              )}
            </ul>
          </section>
        </div>

        <div
          className="overflow-hidden rounded-xl border border-[var(--border)] bg-[var(--card)]"
          style={{ boxShadow: '0 4px 24px rgba(0,0,0,.25)', minHeight: 520 }}
        >
          <div className="border-b border-[var(--border)] px-4 py-2">
            <p className="font-['Exo_2',sans-serif] text-sm font-semibold text-[var(--text)]">
              {selectedRoute
                ? `Route #${selectedRoute.routeId} · ${selectedRoute.vehicleCode}`
                : 'Select a route'}
            </p>
            {selectedRoute && (
              <p className="text-xs text-[var(--muted)]">
                {selectedRoute.stops?.length ?? 0} stops · run {selectedRoute.optimizerRunId ?? '—'}
              </p>
            )}
          </div>
          <div className="relative h-[480px] w-full">
            <MapContainer center={mapCenter} zoom={linePositions.length ? 9 : 7} className="h-full w-full" scrollWheelZoom>
              <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              />
              {linePositions.length > 1 && (
                <Polyline positions={linePositions} pathOptions={{ color: '#00d4ff', weight: 4, opacity: 0.85 }} />
              )}
              <FitBounds positions={linePositions} />
              {selectedRoute?.stops
                ?.filter((s) => s.latitude != null && s.longitude != null)
                .map((s) => (
                  <Marker key={s.stopId} position={[s.latitude!, s.longitude!]}>
                    <Popup>
                      <div className="text-xs">
                        <strong>{stopLabel(s)}</strong>
                        <br />
                        {s.arrivalTime && <>Arr: {new Date(s.arrivalTime).toLocaleString()}</>}
                      </div>
                    </Popup>
                  </Marker>
                ))}
            </MapContainer>
          </div>
          <p className="border-t border-[var(--border)] px-3 py-2 text-[10px] text-[var(--muted)]">
            Map tiles © OpenStreetMap contributors. Use your own tile server for heavy traffic.
          </p>
        </div>
      </div>
    </div>
  );
}
