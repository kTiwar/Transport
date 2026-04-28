import { useState, useCallback, useEffect, useRef } from 'react';
import { useMappings, useMapping, useCreateMapping, useActivateMapping, useAiSuggest, useSaveMappingLines, useVersionHistory } from '../hooks/useMappings';
import { usePartners } from '../hooks/usePartners';
import { useFiles, useFileStructure } from '../hooks/useFiles';
import PageHeader from '../components/common/PageHeader';
import Card from '../components/common/Card';
import Btn from '../components/common/Btn';
import Modal from '../components/common/Modal';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import { Badge, FileTypeBadge } from '../components/common/StatusBadge';
import SchemaTreeViewer from '../components/mapping/SchemaTreeViewer';
import { fmtDate } from '../utils/format';
import { useLayoutStore } from '../store/layoutStore';
import type { MappingLine, FileType, SchemaNode } from '../types';
import {
  Plus, Wand2, Check, Zap, Trash2,
  Maximize2, Minimize2, ArrowRight, Map,
  PanelLeftClose, PanelLeftOpen,
  ChevronDown, ChevronsDownUp, ChevronsUpDown,
  Search, X, RotateCcw, RotateCw, History,
  CloudOff, CheckCircle2, RefreshCw,
} from 'lucide-react';

// ── Canonical target fields — derived from TMS XML Order Import Standard ────────

const CANONICAL_FIELDS = [
  // ─── Order Header ─────────────────────────────────────────────────────────
  { group:'Order Header',              field:'communication_partner',                          required:true  },
  { group:'Order Header',              field:'transaction_type',                               required:false },
  { group:'Order Header',              field:'external_order_number',                          required:false },
  { group:'Order Header',              field:'customer_no',                                    required:true  },
  { group:'Order Header',              field:'customer_name',                                  required:false },
  { group:'Order Header',              field:'financial_status',                               required:false },
  { group:'Order Header',              field:'operational_status',                             required:false },
  { group:'Order Header',              field:'reference1',                                     required:false },
  { group:'Order Header',              field:'reference2',                                     required:false },
  { group:'Order Header',              field:'reference3',                                     required:false },
  // ─── Order Totals ─────────────────────────────────────────────────────────
  { group:'Order Totals',              field:'total_gross_weight',                             required:false },
  { group:'Order Totals',              field:'total_net_weight',                               required:false },
  { group:'Order Totals',              field:'total_volume',                                   required:false },
  { group:'Order Totals',              field:'total_quantity',                                 required:false },
  { group:'Order Totals',              field:'total_loading_meters',                           required:false },
  // ─── Order References (array) ─────────────────────────────────────────────
  { group:'Order References[]',        field:'references[].reference_code',                   required:false },
  { group:'Order References[]',        field:'references[].reference_value',                  required:false },
  // ─── Truck Info ───────────────────────────────────────────────────────────
  { group:'Truck Info',                field:'truck.external_truck_id',                       required:false },
  { group:'Truck Info',                field:'truck.registration_number',                     required:false },
  // ─── Driver Info ──────────────────────────────────────────────────────────
  { group:'Driver Info',               field:'driver.external_driver_id',                     required:false },
  { group:'Driver Info',               field:'driver.driver_name',                            required:false },
  // ─── Trailer Info ─────────────────────────────────────────────────────────
  { group:'Trailer Info',              field:'trailer.external_trailer_id',                   required:false },
  { group:'Trailer Info',              field:'trailer.registration_number',                   required:false },
  // ─── Container Info ───────────────────────────────────────────────────────
  { group:'Container Info',            field:'container.container_number',                    required:false },
  { group:'Container Info',            field:'container.container_type',                      required:false },
  { group:'Container Info',            field:'container.carrier_id',                          required:false },
  { group:'Container Info',            field:'container.carrier_name',                        required:false },
  { group:'Container Info',            field:'container.seal_number',                         required:false },
  { group:'Container Info',            field:'container.import_or_export',                    required:false },
  { group:'Container Info',            field:'container.pickup_pincode',                      required:false },
  { group:'Container Info',            field:'container.pickup_reference',                    required:false },
  { group:'Container Info',            field:'container.dropoff_pincode',                     required:false },
  { group:'Container Info',            field:'container.dropoff_reference',                   required:false },
  // ─── Vessel Info ──────────────────────────────────────────────────────────
  { group:'Vessel Info',               field:'vessel.vessel_name',                            required:false },
  { group:'Vessel Info',               field:'vessel.eta',                                    required:false },
  { group:'Vessel Info',               field:'vessel.etd',                                    required:false },
  { group:'Vessel Info',               field:'vessel.origin_country',                         required:false },
  { group:'Vessel Info',               field:'vessel.origin_port_name',                       required:false },
  { group:'Vessel Info',               field:'vessel.origin_info',                            required:false },
  { group:'Vessel Info',               field:'vessel.destination_country',                    required:false },
  { group:'Vessel Info',               field:'vessel.destination_port_name',                  required:false },
  { group:'Vessel Info',               field:'vessel.destination_info',                       required:false },
  // ─── Order Equipments (array) ─────────────────────────────────────────────
  { group:'Order Equipments[]',        field:'equipments[].material_type',                    required:true  },
  { group:'Order Equipments[]',        field:'equipments[].equipment_type',                   required:true  },
  // ─── Order Cargos (array) ─────────────────────────────────────────────────
  { group:'Order Cargos[]',            field:'cargos[].external_good_id',                     required:false },
  { group:'Order Cargos[]',            field:'cargos[].good_description',                     required:false },
  { group:'Order Cargos[]',            field:'cargos[].tracing_number1',                      required:false },
  { group:'Order Cargos[]',            field:'cargos[].tracing_number2',                      required:false },
  { group:'Order Cargos[]',            field:'cargos[].quantity',                             required:true  },
  { group:'Order Cargos[]',            field:'cargos[].unit_of_measure',                      required:true  },
  { group:'Order Cargos[]',            field:'cargos[].gross_weight',                         required:false },
  { group:'Order Cargos[]',            field:'cargos[].net_weight',                           required:false },
  { group:'Order Cargos[]',            field:'cargos[].volume',                               required:false },
  { group:'Order Cargos[]',            field:'cargos[].length',                               required:false },
  { group:'Order Cargos[]',            field:'cargos[].width',                                required:false },
  { group:'Order Cargos[]',            field:'cargos[].height',                               required:false },
  { group:'Order Cargos[]',            field:'cargos[].loading_meters',                       required:false },
  { group:'Order Cargos[]',            field:'cargos[].diameter',                             required:false },
  { group:'Order Cargos[]',            field:'cargos[].dangerous_goods',                      required:false },
  { group:'Order Cargos[]',            field:'cargos[].adr_un_number',                        required:false },
  { group:'Order Cargos[]',            field:'cargos[].adr_classification',                   required:false },
  { group:'Order Cargos[]',            field:'cargos[].set_temperature',                      required:false },
  { group:'Order Cargos[]',            field:'cargos[].temperature',                          required:false },
  // ─── Order Remarks (array) ────────────────────────────────────────────────
  { group:'Order Remarks[]',           field:'remarks[].line_no',                             required:false },
  { group:'Order Remarks[]',           field:'remarks[].remark_type',                         required:false },
  { group:'Order Remarks[]',           field:'remarks[].remark',                              required:false },
  // ─── Order Stop Lines (array) ─────────────────────────────────────────────
  { group:'Order Stop Lines[]',        field:'stop_lines[].external_order_line_id',           required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].action',                           required:true  },
  { group:'Order Stop Lines[]',        field:'stop_lines[].initial_dt_from',                  required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].initial_dt_until',                 required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].booked_dt_from',                   required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].booked_dt_until',                  required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].external_address_id',              required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].address_name',                     required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].address_street',                   required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].address_house_number',             required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].address_street2',                  required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].address_postal_code',              required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].address_city',                     required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].address_country_code',             required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].address_contact_person',           required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].address_telephone',                required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].address_fax',                      required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].address_email',                    required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].order_line_ref1',                  required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].order_line_ref2',                  required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].part_of_order',                    required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].mileage',                          required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].order_line_status',                required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].requested_time_from',              required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].requested_time_until',             required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].planned_time_from',                required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].planned_time_until',               required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].driver_full_name',                 required:false },
  { group:'Order Stop Lines[]',        field:'stop_lines[].truck_description',                required:false },
  // ─── Order Line Cargos (nested array under stop lines) ───────────────────
  { group:'Order Line Cargos[]',       field:'stop_lines[].line_cargos[].external_good_id',  required:false },
  { group:'Order Line Cargos[]',       field:'stop_lines[].line_cargos[].good_description',  required:false },
  { group:'Order Line Cargos[]',       field:'stop_lines[].line_cargos[].good_type',         required:false },
  { group:'Order Line Cargos[]',       field:'stop_lines[].line_cargos[].good_sub_type',     required:false },
  { group:'Order Line Cargos[]',       field:'stop_lines[].line_cargos[].tracing_number1',   required:false },
  { group:'Order Line Cargos[]',       field:'stop_lines[].line_cargos[].tracing_number2',   required:false },
  { group:'Order Line Cargos[]',       field:'stop_lines[].line_cargos[].quantity',          required:false },
  { group:'Order Line Cargos[]',       field:'stop_lines[].line_cargos[].unit_of_measure',   required:false },
  { group:'Order Line Cargos[]',       field:'stop_lines[].line_cargos[].gross_weight',      required:false },
  { group:'Order Line Cargos[]',       field:'stop_lines[].line_cargos[].net_weight',        required:false },
  { group:'Order Line Cargos[]',       field:'stop_lines[].line_cargos[].volume',            required:false },
  { group:'Order Line Cargos[]',       field:'stop_lines[].line_cargos[].length',            required:false },
  { group:'Order Line Cargos[]',       field:'stop_lines[].line_cargos[].width',             required:false },
  { group:'Order Line Cargos[]',       field:'stop_lines[].line_cargos[].height',            required:false },
  { group:'Order Line Cargos[]',       field:'stop_lines[].line_cargos[].loading_meters',    required:false },
  { group:'Order Line Cargos[]',       field:'stop_lines[].line_cargos[].diameter',          required:false },
  { group:'Order Line Cargos[]',       field:'stop_lines[].line_cargos[].dangerous_goods',   required:false },
  { group:'Order Line Cargos[]',       field:'stop_lines[].line_cargos[].adr_code',          required:false },
  { group:'Order Line Cargos[]',       field:'stop_lines[].line_cargos[].set_temperature',   required:false },
  { group:'Order Line Cargos[]',       field:'stop_lines[].line_cargos[].temperature',       required:false },
  // ─── Order Line References (nested array under stop lines) ───────────────
  { group:'Order Line References[]',   field:'stop_lines[].line_references[].reference_code',  required:false },
  { group:'Order Line References[]',   field:'stop_lines[].line_references[].reference_value', required:false },
  // ─── Order Line Remarks (nested array under stop lines) ──────────────────
  { group:'Order Line Remarks[]',      field:'stop_lines[].line_remarks[].line_no',           required:false },
  { group:'Order Line Remarks[]',      field:'stop_lines[].line_remarks[].remark_type',       required:false },
  { group:'Order Line Remarks[]',      field:'stop_lines[].line_remarks[].remark',            required:false },
];

const TRANSFORMS = [
  'DIRECT','CONSTANT','UPPER','LOWER','TRIM',
  'SUBSTRING','CONCAT','REPLACE','DATE_FORMAT','DATE_NOW',
  'ROUND','MATH','TO_NUMBER','IF','IF_NULL','LOOKUP',
];

// ── Main page ─────────────────────────────────────────────────────────────────

export default function MappingDesignerPage() {
  const [newOpen, setNewOpen]       = useState(false);
  const [activeMapping, setActive]  = useState<number|null>(null);
  const [partnerId, setPartnerId]   = useState<number|null>(null);
  const { sidebarCollapsed, toggleSidebar } = useLayoutStore();

  const { data: partners = [] }               = usePartners();
  const { data: mappings = [], isLoading }    = useMappings(partnerId ?? undefined);
  const { mutate: activate }                  = useActivateMapping();

  return (
    <div className="fade-in">
      <PageHeader
        title="Mapping"
        highlight="Designer"
        subtitle="Create and manage field mappings between partner EDI formats and the canonical data model."
        actions={
          <div style={{ display:'flex', gap:8, alignItems:'center' }}>
            {/* Sidebar toggle shortcut */}
            <Btn
              variant="ghost"
              icon={sidebarCollapsed ? <PanelLeftOpen size={14}/> : <PanelLeftClose size={14}/>}
              onClick={toggleSidebar}
              title={sidebarCollapsed ? 'Show sidebar' : 'Hide sidebar'}
            >
              {sidebarCollapsed ? 'Show Nav' : 'Hide Nav'}
            </Btn>
            <Btn variant="primary" icon={<Plus size={13}/>} onClick={() => setNewOpen(true)}>
              New Mapping
            </Btn>
          </div>
        }
      />

      <div
        className="mapping-desktop-grid"
        style={{ padding:'16px 24px', display:'grid', gridTemplateColumns:'280px 1fr', gap:16 }}
      >
        {/* ── Mapping list sidebar ── */}
        <div>
          <div style={{ marginBottom:10 }}>
            <select
              value={partnerId ?? ''}
              onChange={(e) => setPartnerId(e.target.value ? Number(e.target.value) : null)}
              style={{ fontSize:12 }}
            >
              <option value="">All Partners</option>
              {partners.map((p) => (
                <option key={p.partnerId} value={p.partnerId}>{p.partnerCode}</option>
              ))}
            </select>
          </div>

          <div style={{ display:'flex', flexDirection:'column', gap:6 }}>
            {isLoading ? (
              <div style={{ display:'flex', justifyContent:'center', padding:30 }}><Spinner/></div>
            ) : mappings.length === 0 ? (
              <EmptyState icon="🗺️" title="No mappings" message="Create your first mapping."/>
            ) : mappings.map((m) => (
              <MappingCard
                key={m.mappingId}
                mapping={m}
                active={activeMapping === m.mappingId}
                onSelect={() => setActive(m.mappingId)}
                onActivate={() => activate(m.mappingId)}
              />
            ))}
          </div>
        </div>

        {/* ── Mapping editor ── */}
        {activeMapping ? (
          <MappingEditor mappingId={activeMapping}/>
        ) : (
          <EmptyState
            icon={<Map size={40}/>}
            title="Select a mapping"
            message="Choose a mapping from the list to view and edit its configuration, or create a new one."
          />
        )}
      </div>

      <NewMappingModal open={newOpen} onClose={() => setNewOpen(false)}/>
    </div>
  );
}

// ── Mapping card ──────────────────────────────────────────────────────────────

function MappingCard({
  mapping, active, onSelect, onActivate,
}: {
  mapping: any; active: boolean;
  onSelect: () => void; onActivate: () => void;
}) {
  const [hovered, setHovered] = useState(false);
  return (
    <div
      onClick={onSelect}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        background: active
          ? 'rgba(0,212,255,.1)'
          : hovered
            ? 'rgba(0,212,255,.04)'
            : 'var(--card)',
        border: `1px solid ${active ? 'var(--cyan)' : hovered ? 'rgba(0,212,255,.25)' : 'var(--border)'}`,
        borderRadius: 10,
        padding: '12px 14px',
        cursor: 'pointer',
        transition: 'all .15s',
        transform: hovered && !active ? 'translateX(2px)' : 'none',
      }}
    >
      <div style={{ display:'flex', alignItems:'center', gap:8, marginBottom:6 }}>
        <FileTypeBadge type={mapping.fileType}/>
        {mapping.activeFlag && <Badge color="var(--green)">● Active</Badge>}
        <span style={{ marginLeft:'auto', fontSize:10, color:'var(--muted)', fontFamily:"'Fira Code',monospace" }}>
          v{mapping.version}
        </span>
      </div>
      <div style={{ fontFamily:"'Exo 2',sans-serif", fontWeight:700, fontSize:13, marginBottom:2 }}>
        {mapping.mappingName}
      </div>
      <div style={{ fontSize:11, color:'var(--muted)' }}>{mapping.partnerCode}</div>
      {!mapping.activeFlag && (
        <div style={{ display:'flex', justifyContent:'flex-end', marginTop:8 }}>
          <Btn size="sm" variant="success" icon={<Check size={11}/>}
            onClick={(e) => { e.stopPropagation(); onActivate(); }}>
            Activate
          </Btn>
        </div>
      )}
    </div>
  );
}

// ── Mapping Editor ────────────────────────────────────────────────────────────

type PanelId = 'source' | 'target' | 'lines';
type SaveStatus = 'idle' | 'unsaved' | 'saving' | 'saved' | 'autosaved';

function MappingEditor({ mappingId }: { mappingId: number }) {
  // ── FIX: use useMapping (singular) — loads the full mapping WITH lines ──
  const { data: m, isLoading } = useMapping(mappingId);

  // ── Core state ──────────────────────────────────────────────────────────
  const [lines, setLines]               = useState<MappingLine[]>([]);
  const [selectedSource, setSource]     = useState<string|null>(null);
  const [hoveredTarget, setHoveredTarget] = useState<string|null>(null);
  const [fileEntryNo, setFileEntryNo]   = useState<number|null>(null);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [selectedLine, setSelectedLine] = useState<string|null>(null);

  // ── Enterprise: dirty state + save status ───────────────────────────────
  const [isDirty, setIsDirty]           = useState(false);
  const [saveStatus, setSaveStatus]     = useState<SaveStatus>('idle');
  const [showVersions, setShowVersions] = useState(false);
  const [showDraftModal, setShowDraftModal] = useState(false);
  const [draftLines, setDraftLines]     = useState<MappingLine[]>([]);
  const DRAFT_KEY                       = `mapping-draft-${mappingId}`;

  // ── Version history ─────────────────────────────────────────────────────
  const { data: versionHistory = [] } = useVersionHistory(showVersions ? mappingId : null);

  // ── Search ──────────────────────────────────────────────────────────────
  const [searchSource, setSearchSource] = useState('');
  const [searchTarget, setSearchTarget] = useState('');
  const [searchLines,  setSearchLines]  = useState('');
  const sourceSearchRef = useRef<HTMLInputElement>(null);
  const targetSearchRef = useRef<HTMLInputElement>(null);

  // ── Panel expand ────────────────────────────────────────────────────────
  const [expandedPanel, setExpandedPanel] = useState<PanelId|null>(null);
  const togglePanel = (p: PanelId) => setExpandedPanel(prev => prev === p ? null : p);

  // ── Resizable bottom panel ───────────────────────────────────────────────
  const [bottomH, setBottomH] = useState(220);
  const divDragging = useRef(false);
  const divStartY   = useRef(0);
  const divStartH   = useRef(0);
  const startDividerDrag = (e: React.MouseEvent) => {
    divDragging.current = true;
    divStartY.current   = e.clientY;
    divStartH.current   = bottomH;
    document.body.style.cursor = 'row-resize';
    document.body.style.userSelect = 'none';
  };
  useEffect(() => {
    const onMove = (e: MouseEvent) => {
      if (!divDragging.current) return;
      const delta = divStartY.current - e.clientY;
      setBottomH(Math.min(600, Math.max(80, divStartH.current + delta)));
    };
    const onUp = () => {
      if (!divDragging.current) return;
      divDragging.current = false;
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    };
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
    return () => { window.removeEventListener('mousemove', onMove); window.removeEventListener('mouseup', onUp); };
  }, []);

  // ── Undo / Redo ─────────────────────────────────────────────────────────
  const [undoStack, setUndoStack] = useState<MappingLine[][]>([]);
  const [redoStack, setRedoStack] = useState<MappingLine[][]>([]);
  const pushLines = useCallback((newLines: MappingLine[]) => {
    setUndoStack(prev => [...prev.slice(-49), lines]);
    setRedoStack([]);
    setLines(newLines);
    setIsDirty(true);
    setSaveStatus('unsaved');
    // Persist draft to localStorage so refresh doesn't lose work
    try { localStorage.setItem(DRAFT_KEY, JSON.stringify(newLines)); } catch { /* ignore quota */ }
  }, [lines, DRAFT_KEY]);
  const undo = useCallback(() => {
    if (!undoStack.length) return;
    const prev = undoStack[undoStack.length - 1];
    setRedoStack(s => [...s, lines]);
    setUndoStack(s => s.slice(0, -1));
    setLines(prev);
  }, [undoStack, lines]);
  const redo = useCallback(() => {
    if (!redoStack.length) return;
    const next = redoStack[redoStack.length - 1];
    setUndoStack(s => [...s, lines]);
    setRedoStack(s => s.slice(0, -1));
    setLines(next);
  }, [redoStack, lines]);

  // ── Sort (for bottom table) ──────────────────────────────────────────────
  const [sortCol, setSortCol] = useState<'source'|'target'|'transform'|'seq'>('seq');
  const [sortDir, setSortDir] = useState<'asc'|'desc'>('asc');
  const cycleSort = (col: typeof sortCol) => {
    if (sortCol === col) setSortDir(d => d === 'asc' ? 'desc' : 'asc');
    else { setSortCol(col); setSortDir('asc'); }
  };

  // ── Canonical target tree collapse state ────────────────────────────────
  const ALL_GROUPS = [...new Set(CANONICAL_FIELDS.map((f) => f.group))];
  const [collapsedGroups, setCollapsedGroups] = useState<Set<string>>(
    () => new Set(ALL_GROUPS.slice(1))
  );
  const toggleGroup       = useCallback((g: string) =>
    setCollapsedGroups((prev) => { const n = new Set(prev); n.has(g) ? n.delete(g) : n.add(g); return n; }), []);
  const expandAllGroups   = useCallback(() => setCollapsedGroups(new Set()), []);
  const collapseAllGroups = useCallback(() => setCollapsedGroups(new Set(ALL_GROUPS)), []);

  const { data: filesPage }     = useFiles(0, 200);
  const allFiles                = filesPage?.content ?? [];
  const { data: aiSuggestions } = useAiSuggest(fileEntryNo);
  const { data: schema }        = useFileStructure(fileEntryNo);
  const { mutate: createMapping, isPending: _creatingMapping } = useCreateMapping();
  const { mutate: saveLinesApi, isPending: saving } = useSaveMappingLines();
  const { setSidebarCollapsed, sidebarCollapsed } = useLayoutStore();
  const prevSidebarState = useRef(sidebarCollapsed);

  // ── FIX: Initialize lines from server via useEffect (not useState) ──────
  const initializedRef = useRef(false);
  useEffect(() => {
    if (!m || initializedRef.current) return;
    initializedRef.current = true;

    // Check localStorage for an unsaved draft
    try {
      const raw = localStorage.getItem(DRAFT_KEY);
      if (raw) {
        const parsed: MappingLine[] = JSON.parse(raw);
        if (parsed.length > 0) {
          setDraftLines(parsed);
          setShowDraftModal(true);
          return; // wait for user to decide — don't overwrite with server data yet
        }
      }
    } catch { /* ignore corrupt draft */ }

    setLines(m.lines ?? []);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [m?.mappingId]);

  // ── Auto-save every 30 seconds when dirty ───────────────────────────────
  const autoSaveRef = useRef<ReturnType<typeof setInterval> | null>(null);
  useEffect(() => {
    if (autoSaveRef.current) clearInterval(autoSaveRef.current);
    autoSaveRef.current = setInterval(() => {
      setIsDirty(dirty => {
        if (dirty) {
          setLines(currentLines => {
            saveLinesApi(
              { mappingId, lines: currentLines },
              {
                onSuccess: () => {
                  setIsDirty(false);
                  setSaveStatus('autosaved');
                  localStorage.removeItem(DRAFT_KEY);
                  setTimeout(() => setSaveStatus(s => s === 'autosaved' ? 'idle' : s), 3000);
                },
              }
            );
            return currentLines;
          });
        }
        return dirty;
      });
    }, 30_000);
    return () => { if (autoSaveRef.current) clearInterval(autoSaveRef.current); };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mappingId, DRAFT_KEY]);

  // ── Before-unload warning when there are unsaved changes ────────────────
  useEffect(() => {
    const handler = (e: BeforeUnloadEvent) => {
      if (isDirty) { e.preventDefault(); e.returnValue = ''; }
    };
    window.addEventListener('beforeunload', handler);
    return () => window.removeEventListener('beforeunload', handler);
  }, [isDirty]);

  // ── Reset initialized flag when mapping changes ──────────────────────────
  useEffect(() => {
    initializedRef.current = false;
    setIsDirty(false);
    setSaveStatus('idle');
  }, [mappingId]);

  // ── Save handler ────────────────────────────────────────────────────────
  const handleSave = useCallback(() => {
    if (!mappingId) return;
    setSaveStatus('saving');
    saveLinesApi(
      { mappingId, lines },
      {
        onSuccess: () => {
          setIsDirty(false);
          setSaveStatus('saved');
          localStorage.removeItem(DRAFT_KEY);
          setTimeout(() => setSaveStatus(s => s === 'saved' ? 'idle' : s), 3000);
        },
        onError: () => setSaveStatus('unsaved'),
      }
    );
  }, [mappingId, lines, saveLinesApi, DRAFT_KEY]);

  // ── Fullscreen enter / exit ─────────────────────────────────────────────
  const enterFullscreen = useCallback(() => {
    prevSidebarState.current = sidebarCollapsed;
    setSidebarCollapsed(true);
    setIsFullscreen(true);
  }, [sidebarCollapsed, setSidebarCollapsed]);
  const exitFullscreen = useCallback(() => {
    setSidebarCollapsed(prevSidebarState.current);
    setIsFullscreen(false);
  }, [setSidebarCollapsed]);

  // ── Keyboard shortcuts ──────────────────────────────────────────────────
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        if (expandedPanel) { setExpandedPanel(null); return; }
        if (isFullscreen)  { exitFullscreen(); return; }
      }
      if (e.ctrlKey && e.key === 'z') { e.preventDefault(); undo(); }
      if (e.ctrlKey && e.key === 'y') { e.preventDefault(); redo(); }
      if (e.ctrlKey && e.key === 'f') {
        e.preventDefault();
        (sourceSearchRef.current ?? targetSearchRef.current)?.focus();
      }
      if (e.key === 'Delete' && selectedLine) {
        removeLine(selectedLine); setSelectedLine(null);
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [expandedPanel, isFullscreen, exitFullscreen, undo, redo, selectedLine]);

  // ── Mapping operations ──────────────────────────────────────────────────
  const addLine = (targetField: string) => {
    if (!selectedSource) return;
    const exists = lines.find((l) => l.targetField === targetField);
    const newLines = exists
      ? lines.map((l) => l.targetField === targetField ? { ...l, sourceFieldPath: selectedSource } : l)
      : [...lines, {
          sourceFieldPath: selectedSource, targetField,
          transformationRule: 'DIRECT',
          isRequired: CANONICAL_FIELDS.find((f) => f.field === targetField)?.required ?? false,
          sequence: lines.length,
        }];
    pushLines(newLines);
    setSource(null);
  };

  const removeLine = useCallback((targetField: string) =>
    pushLines(lines.filter((l) => l.targetField !== targetField)), [lines, pushLines]);

  const applyAiSuggestions = () => {
    if (!aiSuggestions) return;
    pushLines(aiSuggestions.suggestions.map((s, i) => ({
      sourceFieldPath: s.sourcePath, targetField: s.targetField,
      transformationRule: s.suggestedTransform ?? 'DIRECT',
      isRequired: CANONICAL_FIELDS.find((f) => f.field === s.targetField)?.required ?? false,
      sequence: i,
    })));
  };

  // ── Derived data ────────────────────────────────────────────────────────
  const mappedTargets = new Set(lines.map((l) => l.targetField));
  const groups        = [...new Set(CANONICAL_FIELDS.map((f) => f.group))];

  // Filtered + sorted bottom table lines
  const filteredLines = lines
    .filter((l) => !searchLines || [l.sourceFieldPath, l.targetField, l.transformationRule]
      .some((v) => v?.toLowerCase().includes(searchLines.toLowerCase())))
    .sort((a, b) => {
      const val = (l: MappingLine) =>
        sortCol === 'source'    ? (l.sourceFieldPath ?? '')
        : sortCol === 'target'  ? l.targetField
        : sortCol === 'transform' ? (l.transformationRule ?? '')
        : String(l.sequence ?? 0);
      return sortDir === 'asc' ? val(a).localeCompare(val(b)) : val(b).localeCompare(val(a));
    });

  // Filtered canonical target fields
  const targetMatchesSearch = (field: string) =>
    !searchTarget || field.toLowerCase().includes(searchTarget.toLowerCase());
  const groupMatchesSearch  = (group: string) =>
    !searchTarget || group.toLowerCase().includes(searchTarget.toLowerCase())
    || CANONICAL_FIELDS.some((f) => f.group === group && targetMatchesSearch(f.field));

  if (isLoading) return (
    <div style={{ display:'flex', justifyContent:'center', padding:60 }}><Spinner/></div>
  );

  // ── Enterprise 3-panel render ───────────────────────────────────────────
  return (
    <>
      {isFullscreen && <div className="fullscreen-backdrop"/>}

      {/* ── Draft Recovery Modal ──────────────────────────────────────── */}
      {showDraftModal && (
        <div style={{
          position:'fixed', inset:0, zIndex:9999,
          background:'rgba(0,0,0,.6)', display:'flex', alignItems:'center', justifyContent:'center',
        }}>
          <div style={{
            background:'var(--card)', border:'1px solid var(--border)', borderRadius:12,
            padding:'28px 32px', maxWidth:440, width:'90%', boxShadow:'0 20px 60px rgba(0,0,0,.5)',
          }}>
            <div style={{ fontSize:22, marginBottom:8 }}>🔄</div>
            <div style={{ fontFamily:"'Exo 2',sans-serif", fontWeight:700, fontSize:15, marginBottom:8 }}>
              Unsaved Draft Detected
            </div>
            <p style={{ fontSize:13, color:'var(--muted)', marginBottom:20, lineHeight:1.5 }}>
              A local draft was found with <strong style={{ color:'var(--cyan)' }}>{draftLines.length} mapping line{draftLines.length !== 1 ? 's' : ''}</strong>.
              Would you like to restore it or discard it and load the last saved version?
            </p>
            <div style={{ display:'flex', gap:10, justifyContent:'flex-end' }}>
              <Btn variant="ghost" onClick={() => {
                localStorage.removeItem(DRAFT_KEY);
                setShowDraftModal(false);
                setLines(m?.lines ?? []);
              }}>Discard Draft</Btn>
              <Btn variant="primary" icon={<RotateCcw size={13}/>} onClick={() => {
                setLines(draftLines);
                setIsDirty(true);
                setSaveStatus('unsaved');
                setShowDraftModal(false);
              }}>Restore Draft</Btn>
            </div>
          </div>
        </div>
      )}

      {/* ── Version History Side Panel ────────────────────────────────── */}
      {showVersions && (
        <div style={{
          position:'fixed', top:0, right:0, bottom:0, width:300, zIndex:1000,
          background:'var(--card)', borderLeft:'1px solid var(--border)',
          display:'flex', flexDirection:'column', boxShadow:'-4px 0 20px rgba(0,0,0,.3)',
        }}>
          <div style={{
            padding:'14px 16px', borderBottom:'1px solid var(--border)',
            display:'flex', alignItems:'center', gap:8,
          }}>
            <History size={14} color="var(--cyan)"/>
            <span style={{ fontFamily:"'Exo 2',sans-serif", fontWeight:700, fontSize:13 }}>Version History</span>
            <button className="mp-icon-btn" style={{ marginLeft:'auto' }} onClick={() => setShowVersions(false)}>
              <X size={14}/>
            </button>
          </div>
          <div style={{ flex:1, overflowY:'auto', padding:'8px 0' }}>
            {/* Current (unsaved) version */}
            <div style={{
              margin:'4px 8px', padding:'10px 12px', borderRadius:8,
              background: isDirty ? 'rgba(245,158,11,.08)' : 'rgba(16,185,129,.06)',
              border: isDirty ? '1px solid rgba(245,158,11,.3)' : '1px solid rgba(16,185,129,.25)',
            }}>
              <div style={{ display:'flex', alignItems:'center', gap:6, marginBottom:4 }}>
                <span style={{ fontSize:10, fontWeight:700, fontFamily:"'Fira Code',monospace",
                  color: isDirty ? 'var(--yellow)' : 'var(--green)' }}>
                  v{(m?.version ?? 1)} {isDirty ? '● UNSAVED' : '● CURRENT'}
                </span>
              </div>
              <div style={{ fontSize:11, color:'var(--muted)' }}>{lines.length} mapping lines</div>
            </div>

            {versionHistory.length === 0 ? (
              <div style={{ textAlign:'center', color:'var(--muted)', fontSize:12, padding:'24px 16px' }}>
                No previous versions yet.<br/>Save to create version history.
              </div>
            ) : (
              versionHistory.map((v) => (
                <div key={v.id} style={{
                  margin:'4px 8px', padding:'10px 12px', borderRadius:8,
                  background:'var(--bg2)', border:'1px solid var(--border)',
                }}>
                  <div style={{ display:'flex', alignItems:'center', gap:6, marginBottom:4 }}>
                    <span style={{ fontSize:10, fontWeight:700, fontFamily:"'Fira Code',monospace",
                      color:'var(--muted)' }}>v{v.version}</span>
                    <span style={{ fontSize:10, color:'var(--muted)', marginLeft:'auto' }}>
                      {new Date(v.savedAt).toLocaleString()}
                    </span>
                  </div>
                  <div style={{ fontSize:11, color:'var(--text2)', marginBottom:2 }}>{v.changeSummary}</div>
                  <div style={{ fontSize:10, color:'var(--muted)' }}>
                    {v.lineCount} lines · by {v.savedBy ?? 'unknown'}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}

      <div className={isFullscreen ? 'mapping-fullscreen' : 'mapping-editor-root'}>
        {/* ── Enterprise Toolbar ─────────────────────────────────── */}
        <div className="mapping-toolbar">
          <span style={{ fontFamily:"'Exo 2',sans-serif", fontWeight:700, fontSize:13, color:'var(--text)' }}>
            {m?.mappingName ?? 'Mapping'}
          </span>
          <span style={{ color:'var(--muted)', fontSize:11 }}>
            {lines.length} mappings
          </span>
          <div style={{ width:1, height:18, background:'var(--border)', margin:'0 4px' }}/>
          <select value={fileEntryNo ?? ''} onChange={(e) => setFileEntryNo(e.target.value ? Number(e.target.value) : null)}
            style={{ width:200, fontSize:11 }}>
            <option value="">Load source schema from file…</option>
            {allFiles.map((f) => (
              <option key={f.entryNo} value={f.entryNo}>#{f.entryNo} {f.fileName}</option>
            ))}
          </select>
          {aiSuggestions && (
            <Btn size="sm" variant="warning" icon={<Wand2 size={13}/>} onClick={applyAiSuggestions}>
              AI Suggestions ({aiSuggestions.suggestions.length})
            </Btn>
          )}
          <div style={{ marginLeft:'auto', display:'flex', gap:6, alignItems:'center' }}>
            {/* ── Save status indicator ── */}
            {saveStatus === 'saving' && (
              <span style={{ fontSize:10, color:'var(--muted)', display:'flex', alignItems:'center', gap:3 }}>
                <RefreshCw size={10} style={{ animation:'spin 1s linear infinite' }}/> Saving…
              </span>
            )}
            {saveStatus === 'autosaved' && (
              <span style={{ fontSize:10, color:'var(--green)', display:'flex', alignItems:'center', gap:3 }}>
                <CheckCircle2 size={10}/> Auto Saved
              </span>
            )}
            {saveStatus === 'saved' && (
              <span style={{ fontSize:10, color:'var(--green)', display:'flex', alignItems:'center', gap:3 }}>
                <CheckCircle2 size={10}/> Saved
              </span>
            )}
            {(saveStatus === 'unsaved' || (isDirty && saveStatus === 'idle')) && (
              <span style={{ fontSize:10, color:'var(--yellow)', display:'flex', alignItems:'center', gap:3 }}>
                <CloudOff size={10}/> Unsaved Changes
              </span>
            )}

            {/* ── Version info ── */}
            {m && (
              <span style={{ fontSize:10, color:'var(--muted)', fontFamily:"'Fira Code',monospace",
                background:'var(--bg3)', padding:'2px 6px', borderRadius:4, border:'1px solid var(--border)' }}>
                v{m.version}
              </span>
            )}

            {/* ── Undo / Redo ── */}
            {undoStack.length > 0 && (
              <button className="mp-icon-btn" onClick={undo} title="Undo (Ctrl+Z)" style={{ color:'var(--yellow)' }}>
                <RotateCcw size={12}/>
              </button>
            )}
            {redoStack.length > 0 && (
              <button className="mp-icon-btn" onClick={redo} title="Redo (Ctrl+Y)" style={{ color:'var(--cyan)' }}>
                <RotateCw size={12}/>
              </button>
            )}

            {/* ── Version history button ── */}
            <button className="mp-icon-btn" title="Version History"
              onClick={() => setShowVersions(v => !v)}
              style={{ color: showVersions ? 'var(--cyan)' : 'var(--muted)' }}>
              <History size={12}/>
            </button>

            <Btn size="sm" variant="ghost"
              icon={isFullscreen ? <Minimize2 size={13}/> : <Maximize2 size={13}/>}
              onClick={isFullscreen ? exitFullscreen : enterFullscreen}
              title={isFullscreen ? 'Exit fullscreen (Esc)' : 'Expand to fullscreen'}>
              {isFullscreen ? 'Exit Full' : 'Full Screen'}
            </Btn>
            <Btn size="sm" variant="primary" loading={saving} icon={<Zap size={13}/>}
              onClick={() => { handleSave(); if (isFullscreen) exitFullscreen(); }}>
              {isFullscreen ? 'Save & Close' : 'Save Mapping'}
            </Btn>
          </div>
        </div>


        {/* ── Enterprise workspace: show/hide panels based on expandedPanel ── */}
        {expandedPanel !== 'lines' && (
          <div className="mapping-workspace">
            {/* ─── LEFT: Source Schema ─────────────────────────────────── */}
            {(!expandedPanel || expandedPanel === 'source') && (
              <div className={`mp-panel${expandedPanel === 'source' ? ' mp-panel-full' : ''}`}>
                <div className="mp-panel-hdr">
                  <span className="mp-hdr-icon">📂</span>
                  <span style={{ color:'var(--cyan)' }}>Source Schema</span>
                  {fileEntryNo && <Badge color="var(--cyan)">{fileEntryNo}</Badge>}
                  <div className="mp-hdr-actions">
                    <button className="mp-icon-btn"
                      title={expandedPanel === 'source' ? 'Restore (Esc)' : 'Expand panel'}
                      onClick={() => togglePanel('source')}>
                      {expandedPanel === 'source' ? <Minimize2 size={12}/> : <Maximize2 size={12}/>}
                    </button>
                  </div>
                </div>
                <div className="mp-search">
                  <Search size={11}/>
                  <input ref={sourceSearchRef} value={searchSource}
                    onChange={e => setSearchSource(e.target.value)}
                    placeholder="Search fields… (Ctrl+F)"/>
                  {searchSource && <button className="mp-search-clear" onClick={() => setSearchSource('')}><X size={10}/></button>}
                </div>
                <div className="mp-panel-body">
                  {schema ? (
                    <SchemaTreeViewer node={schema}
                      onSelect={(path) => { setSource(path); setSelectedLine(null); }}
                      selectedPath={selectedSource}
                      searchTerm={searchSource}/>
                  ) : (
                    <div style={{ textAlign:'center', color:'var(--muted)', fontSize:12, padding:'40px 12px' }}>
                      <div style={{ fontSize:28, marginBottom:8 }}>🔍</div>
                      Select a source file in the toolbar to load the schema
                    </div>
                  )}
                  {selectedSource && (
                    <div className="mp-source-selected">
                      <div style={{ fontSize:9, color:'var(--muted)', fontFamily:"'Exo 2',sans-serif",
                        fontWeight:700, textTransform:'uppercase', letterSpacing:'.5px', marginBottom:4 }}>✓ Selected Field</div>
                      <div style={{ fontSize:11, color:'var(--cyan)', fontFamily:"'Fira Code',monospace",
                        wordBreak:'break-all' }}>{selectedSource}</div>
                      <div style={{ fontSize:10, color:'var(--muted)', marginTop:4 }}>→ Click a canonical target field to map</div>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* ─── RIGHT: Canonical Target ─────────────────────────────── */}
            {(!expandedPanel || expandedPanel === 'target') && (
              <div className={`mp-panel${expandedPanel === 'target' ? ' mp-panel-full' : ''}`}
                style={{ borderRight:'none', borderLeft: expandedPanel === 'target' ? 'none' : '1px solid var(--border)' }}>
                <div className="mp-panel-hdr">
                  <span className="mp-hdr-icon">⭐</span>
                  <span style={{ color:'var(--orange)' }}>Canonical Target</span>
                  <span style={{ fontSize:9, color:'var(--muted)', fontFamily:"'Fira Code',monospace",
                    background:'var(--bg3)', padding:'1px 5px', borderRadius:3 }}>
                    {mappedTargets.size}/{CANONICAL_FIELDS.length}
                  </span>
                  <div className="mp-hdr-actions">
                    <button className="mp-icon-btn" title="Expand All" onClick={expandAllGroups}><ChevronsUpDown size={11}/></button>
                    <button className="mp-icon-btn" title="Collapse All" onClick={collapseAllGroups}><ChevronsDownUp size={11}/></button>
                    <button className="mp-icon-btn"
                      title={expandedPanel === 'target' ? 'Restore (Esc)' : 'Expand panel'}
                      onClick={() => togglePanel('target')}>
                      {expandedPanel === 'target' ? <Minimize2 size={12}/> : <Maximize2 size={12}/>}
                    </button>
                  </div>
                </div>
                <div className="mp-search">
                  <Search size={11}/>
                  <input ref={targetSearchRef} value={searchTarget}
                    onChange={e => setSearchTarget(e.target.value)}
                    placeholder="Search canonical fields…"/>
                  {searchTarget && <button className="mp-search-clear" onClick={() => setSearchTarget('')}><X size={10}/></button>}
                </div>
                <div className="mp-panel-body mp-target-body">
                  {groups.filter(groupMatchesSearch).map((group) => {
                    const isCollapsed = collapsedGroups.has(group) && !searchTarget;
                    const groupFields = CANONICAL_FIELDS.filter(f => f.group === group && targetMatchesSearch(f.field));
                    const mappedCount = groupFields.filter(f => mappedTargets.has(f.field)).length;
                    const hasMapped   = mappedCount > 0;
                    return (
                      <div key={group} style={{ marginBottom: isCollapsed ? 2 : 8 }}>
                        {/* Group header */}
                        <div role="button" tabIndex={0} onClick={() => toggleGroup(group)}
                          onKeyDown={e => e.key === 'Enter' && toggleGroup(group)}
                          style={{
                            display:'flex', alignItems:'center', gap:6, padding:'5px 7px',
                            borderRadius:7, cursor:'pointer', userSelect:'none', marginBottom: isCollapsed ? 0 : 4,
                            border: hasMapped ? '1px solid rgba(16,185,129,.25)' : '1px solid transparent',
                            background: hasMapped ? 'rgba(16,185,129,.04)' : 'transparent',
                            transition:'background .15s, border-color .15s',
                          }}
                          onMouseEnter={e => (e.currentTarget.style.background = hasMapped ? 'rgba(16,185,129,.09)' : 'rgba(255,107,53,.06)')}
                          onMouseLeave={e => (e.currentTarget.style.background = hasMapped ? 'rgba(16,185,129,.04)' : 'transparent')}
                        >
                          <span style={{ display:'inline-flex', flexShrink:0,
                            color: hasMapped ? 'var(--green)' : 'var(--orange)',
                            transform: isCollapsed ? 'rotate(-90deg)' : 'rotate(0deg)',
                            transition:'transform .2s ease' }}>
                            <ChevronDown size={13}/>
                          </span>
                          <span style={{ fontSize:10, fontWeight:700, flex:1,
                            color: hasMapped ? 'var(--green)' : 'var(--orange)',
                            textTransform:'uppercase', letterSpacing:'.5px',
                            fontFamily:"'Exo 2',sans-serif" }}>{group}</span>
                          <span style={{ fontSize:9,
                            color: hasMapped ? 'var(--green)' : 'var(--muted)',
                            background: hasMapped ? 'rgba(16,185,129,.12)' : 'var(--bg3)',
                            border: hasMapped ? '1px solid rgba(16,185,129,.3)' : '1px solid var(--border)',
                            padding:'1px 5px', borderRadius:3, fontFamily:"'Fira Code',monospace",
                            transition:'all .2s' }}>
                            {mappedCount}/{groupFields.length}
                          </span>
                        </div>
                        {/* Field rows (only when expanded) */}
                        {!isCollapsed && (
                          <div style={{ animation:'expandIn .15s ease forwards' }}>
                            {groupFields.map(cf => {
                              const line = lines.find(l => l.targetField === cf.field);
                              const mapped = !!line;
                              const isHov = hoveredTarget === cf.field;
                              const isLineSel = selectedLine === cf.field;
                              return (
                                <div key={cf.field}
                                  className={`canonical-field-row${selectedSource ? ' mappable' : ''}${mapped ? ' mapped' : ''}`}
                                  onClick={() => selectedSource && addLine(cf.field)}
                                  onMouseEnter={() => setHoveredTarget(cf.field)}
                                  onMouseLeave={() => setHoveredTarget(null)}
                                  style={{
                                    border: isLineSel ? '1px solid var(--purple)'
                                      : mapped ? '1px solid rgba(16,185,129,.4)'
                                      : selectedSource && isHov ? '1px solid rgba(0,212,255,.6)'
                                      : selectedSource ? '1px dashed rgba(0,212,255,.3)'
                                      : '1px dashed var(--border)',
                                    background: isLineSel ? 'rgba(139,92,246,.07)'
                                      : mapped ? 'rgba(16,185,129,.06)'
                                      : selectedSource && isHov ? 'rgba(0,212,255,.1)'
                                      : 'transparent',
                                  }}>
                                  {cf.required && <span style={{ color:'var(--red)', fontSize:13, flexShrink:0 }}>*</span>}
                                  <span style={{ fontFamily:"'Fira Code',monospace", fontSize:11, flex:1,
                                    color: mapped ? 'var(--green)' : isHov && selectedSource ? 'var(--cyan)' : 'var(--text2)',
                                    transition:'color .1s' }}>{cf.field}</span>
                                  {mapped ? (
                                    <>
                                      <span style={{ fontSize:10, color:'var(--muted)',
                                        fontFamily:"'Fira Code',monospace",
                                        maxWidth:110, overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' }}>
                                        {line.sourceFieldPath}
                                      </span>
                                      {line.transformationRule && line.transformationRule !== 'DIRECT' && (
                                        <Badge color="var(--yellow)">{line.transformationRule}</Badge>
                                      )}
                                      <button onClick={e => { e.stopPropagation(); removeLine(cf.field); }}
                                        className="mp-icon-btn" style={{ color:'var(--red)', padding:2 }}>
                                        <Trash2 size={10}/>
                                      </button>
                                    </>
                                  ) : (
                                    selectedSource && isHov && (
                                      <span style={{ fontSize:10, color:'var(--cyan)',
                                        fontFamily:"'Fira Code',monospace", flexShrink:0 }}>← map</span>
                                    )
                                  )}
                                </div>
                              );
                            })}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
        )}

        {/* ── Resizable divider ──────────────────────────────────────── */}
        {!expandedPanel && (
          <div className="mp-divider" onMouseDown={startDividerDrag}/>
        )}

        {/* ── BOTTOM: Configured Mapping Lines ──────────────────────── */}
        {(!expandedPanel || expandedPanel === 'lines') && (
          <div className="mp-bottom"
            style={{ height: expandedPanel === 'lines' ? '100%' : bottomH, flex: expandedPanel === 'lines' ? '1' : 'none' }}>
            <div className="mp-panel-hdr" style={{ color:'var(--text2)' }}>
              <span className="mp-hdr-icon">⚡</span>
              <span>Configured Mapping Lines</span>
              <span style={{ fontSize:9, color:'var(--muted)', fontFamily:"'Fira Code',monospace",
                background:'var(--bg3)', padding:'1px 5px', borderRadius:3 }}>
                {filteredLines.length}{searchLines ? ` / ${lines.length}` : ''}
              </span>
              <div className="mp-hdr-actions">
                <button className="mp-icon-btn"
                  title={expandedPanel === 'lines' ? 'Restore (Esc)' : 'Expand panel'}
                  onClick={() => togglePanel('lines')}>
                  {expandedPanel === 'lines' ? <Minimize2 size={12}/> : <Maximize2 size={12}/>}
                </button>
              </div>
            </div>
            <div className="mp-search">
              <Search size={11}/>
              <input value={searchLines} onChange={e => setSearchLines(e.target.value)}
                placeholder="Search mappings… (source · target · transform)"/>
              {searchLines && <button className="mp-search-clear" onClick={() => setSearchLines('')}><X size={10}/></button>}
            </div>
            <div className="mp-bottom-table">
              {filteredLines.length === 0 ? (
                <div style={{ textAlign:'center', color:'var(--muted)', padding:'24px', fontSize:12 }}>
                  {lines.length === 0 ? 'No field mappings configured yet.' : 'No results for your search.'}
                </div>
              ) : (
                <table style={{ width:'100%', borderCollapse:'collapse', fontSize:11 }}>
                  <thead>
                    <tr style={{ background:'var(--bg2)', position:'sticky', top:0, zIndex:1 }}>
                      {([
                        ['#', null], ['Source', 'source'], ['→', null],
                        ['Target', 'target'], ['Transform', 'transform'],
                        ['Default', null], ['Req', null], ['', null],
                      ] as [string, 'source'|'target'|'transform'|'seq'|null][]).map(([label, col]) => (
                        <th key={label} onClick={col ? () => cycleSort(col) : undefined}
                          className={col ? 'mp-th-sort' : ''}
                          style={{ padding:'7px 10px', textAlign:'left',
                            fontFamily:"'Exo 2',sans-serif", fontSize:10, fontWeight:700,
                            letterSpacing:'.5px',
                            color: col && sortCol === col ? 'var(--cyan)' : 'var(--text2)',
                            textTransform:'uppercase', borderBottom:'1px solid var(--border)',
                            whiteSpace:'nowrap' }}>
                          {label}{col && sortCol === col ? (sortDir === 'asc' ? ' ↑' : ' ↓') : ''}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {filteredLines.map((l, i) => (
                      <MappingTableRow key={l.targetField} line={l} index={i}
                        selected={selectedLine === l.targetField}
                        searchTerm={searchLines}
                        onChange={updated => pushLines(lines.map(x => x.targetField === updated.targetField ? updated : x))}
                        onRemove={() => removeLine(l.targetField)}
                        onSelect={() => setSelectedLine(selectedLine === l.targetField ? null : l.targetField)}
                      />
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        )}
      </div>
    </>
  );
}

// ── Mapping table row ─────────────────────────────────────────────────────────

function MappingTableRow({
  line, index, onChange, onRemove, onSelect, selected = false, searchTerm = '',
}: {
  line: MappingLine; index: number;
  onChange: (l: MappingLine) => void;
  onRemove: () => void;
  onSelect?: () => void;
  selected?: boolean;
  searchTerm?: string;
}) {
  const [hovered, setHovered] = useState(false);

  const HL = ({ text }: { text: string }) => {
    if (!searchTerm) return <>{text}</>;
    const idx = text.toLowerCase().indexOf(searchTerm.toLowerCase());
    if (idx === -1) return <>{text}</>;
    return (
      <>
        {text.slice(0, idx)}
        <mark style={{ background:'rgba(245,158,11,.28)', color:'var(--yellow)',
          borderRadius:2, padding:'0 1px', fontWeight:700 }}>
          {text.slice(idx, idx + searchTerm.length)}
        </mark>
        {text.slice(idx + searchTerm.length)}
      </>
    );
  };

  return (
    <tr
      onClick={onSelect}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        background: selected ? 'rgba(139,92,246,.08)' : hovered ? 'rgba(255,255,255,.02)' : 'transparent',
        borderLeft: selected ? '2px solid var(--purple)' : '2px solid transparent',
        cursor: 'pointer',
        transition:'background .12s',
      }}
    >
      <td style={{ padding:'7px 10px', borderBottom:'1px solid var(--border)', color:'var(--muted)' }}>{index+1}</td>
      <td style={{ padding:'7px 10px', borderBottom:'1px solid var(--border)',
        fontFamily:"'Fira Code',monospace", maxWidth:160,
        overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap', color:'var(--cyan)' }}>
        {line.sourceFieldPath ? <HL text={line.sourceFieldPath}/> : <span style={{ color:'var(--muted)' }}>—</span>}
      </td>
      <td style={{ padding:'7px 10px', borderBottom:'1px solid var(--border)', color:'var(--muted)' }}>→</td>
      <td style={{ padding:'7px 10px', borderBottom:'1px solid var(--border)',
        fontFamily:"'Fira Code',monospace", color:'var(--green)' }}>
        <HL text={line.targetField}/>
      </td>
      <td style={{ padding:'7px 10px', borderBottom:'1px solid var(--border)' }}>
        <select value={line.transformationRule ?? 'DIRECT'}
          onChange={e => onChange({ ...line, transformationRule: e.target.value })}
          onClick={e => e.stopPropagation()}
          style={{ width:'auto', padding:'2px 6px', fontSize:11 }}>
          {TRANSFORMS.map(t => <option key={t}>{t}</option>)}
        </select>
      </td>
      <td style={{ padding:'7px 10px', borderBottom:'1px solid var(--border)' }}>
        <input value={line.defaultValue ?? ''} placeholder="—"
          onChange={e => onChange({ ...line, defaultValue: e.target.value })}
          onClick={e => e.stopPropagation()}
          style={{ width:80, padding:'2px 6px', fontSize:11 }}/>
      </td>
      <td style={{ padding:'7px 10px', borderBottom:'1px solid var(--border)', textAlign:'center' }}>
        <input type="checkbox" checked={line.isRequired ?? false}
          onChange={e => onChange({ ...line, isRequired: e.target.checked })}
          onClick={e => e.stopPropagation()}
          style={{ width:'auto', cursor:'pointer' }}/>
      </td>
      <td style={{ padding:'7px 10px', borderBottom:'1px solid var(--border)' }}>
        <button onClick={e => { e.stopPropagation(); onRemove(); }}
          style={{ background: hovered ? 'rgba(239,68,68,.15)' : 'none',
            border:'none', color:'var(--red)', cursor:'pointer',
            padding:'3px 5px', borderRadius:4, transition:'background .1s' }}>
          <Trash2 size={11}/>
        </button>
      </td>
    </tr>
  );
}

// ── New Mapping Modal ─────────────────────────────────────────────────────────

function NewMappingModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const [name, setName]           = useState('');
  const [partnerId, setPartnerId] = useState('');
  const [fileType, setFileType]   = useState<FileType>('XML');
  const [desc, setDesc]           = useState('');
  const { data: partners = [] }   = usePartners();
  const { mutate: create, isPending } = useCreateMapping();

  const handleCreate = () => {
    if (!name.trim() || !partnerId) return;
    create(
      { mappingName: name, partnerId: Number(partnerId), fileType, description: desc },
      { onSuccess: () => { onClose(); setName(''); setPartnerId(''); setDesc(''); } }
    );
  };

  const labelStyle: React.CSSProperties = {
    display:'block', fontSize:11, fontWeight:700, color:'var(--text2)',
    fontFamily:"'Exo 2',sans-serif", letterSpacing:'.5px',
    textTransform:'uppercase', marginBottom:6,
  };

  return (
    <Modal open={open} onClose={onClose} title="Create New Mapping">
      <div style={{ display:'flex', flexDirection:'column', gap:16 }}>
        <div>
          <label style={labelStyle}>Mapping Name</label>
          <input value={name} onChange={(e) => setName(e.target.value)}
            placeholder="e.g. ACME XML Order Mapping v1"/>
        </div>
        <div>
          <label style={labelStyle}>Partner</label>
          <select value={partnerId} onChange={(e) => setPartnerId(e.target.value)}>
            <option value="">Select partner…</option>
            {partners.map((p) => (
              <option key={p.partnerId} value={p.partnerId}>{p.partnerCode} — {p.partnerName}</option>
            ))}
          </select>
        </div>
        <div>
          <label style={labelStyle}>Source File Type</label>
          <select value={fileType} onChange={(e) => setFileType(e.target.value as FileType)}>
            {['XML','JSON','CSV','TXT','EDIFACT','X12','EXCEL'].map((t) => (
              <option key={t}>{t}</option>
            ))}
          </select>
        </div>
        <div>
          <label style={labelStyle}>Description</label>
          <textarea value={desc} onChange={(e) => setDesc(e.target.value)}
            rows={2} placeholder="Optional description…" style={{ resize:'vertical' }}/>
        </div>
        <div style={{ display:'flex', gap:10, justifyContent:'flex-end' }}>
          <Btn variant="ghost" onClick={onClose}>Cancel</Btn>
          <Btn variant="primary" loading={isPending} disabled={!name.trim() || !partnerId}
            onClick={handleCreate} icon={<Plus size={13}/>}>
            Create Mapping
          </Btn>
        </div>
      </div>
    </Modal>
  );
}