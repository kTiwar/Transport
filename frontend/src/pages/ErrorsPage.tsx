import { useState } from 'react';
import { useErrors, useResolveError } from '../hooks/useErrors';
import PageHeader from '../components/common/PageHeader';
import Btn from '../components/common/Btn';
import Modal from '../components/common/Modal';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import { Badge } from '../components/common/StatusBadge';
import { fmtDate } from '../utils/format';
import type { EdiError } from '../types';
import { CheckCircle, RefreshCw, AlertTriangle, ChevronLeft, ChevronRight } from 'lucide-react';

const ERROR_COLORS: Record<string, string> = {
  MISSING_MANDATORY_FIELD: 'var(--red)',
  TRANSFORMATION_FAILURE:  'var(--orange)',
  INVALID_DATA_FORMAT:     'var(--yellow)',
  LOOKUP_VALUE_NOT_FOUND:  'var(--purple)',
  DUPLICATE_ORDER_ID:      'var(--pink)',
  SCHEMA_PARSE_ERROR:      'var(--cyan)',
  DB_INSERT_FAILURE:       'var(--red)',
  VALIDATION_RULE_FAIL:    'var(--orange)',
  TIMEOUT:                 'var(--muted)',
  CHECKSUM_MISMATCH:       'var(--red)',
};

export default function ErrorsPage() {
  const [page, setPage] = useState(0);
  const [resolveTarget, setResolveTarget] = useState<EdiError|null>(null);
  const [showResolved, setShowResolved]   = useState(false);

  const { data, isLoading, refetch } = useErrors(page, 50, showResolved);
  const errors = data?.content ?? [];

  return (
    <div className="fade-in">
      <PageHeader
        title="Error"
        highlight="Log"
        subtitle="View, investigate, and resolve EDI processing errors. Retry failed files directly."
        actions={
          <>
            <label style={{ display:'flex', alignItems:'center', gap:8, fontSize:12, color:'var(--text2)', cursor:'pointer' }}>
              <input type="checkbox" checked={showResolved} onChange={(e) => setShowResolved(e.target.checked)} style={{ width:'auto' }}/>
              Show resolved
            </label>
            <Btn size="sm" icon={<RefreshCw size={13}/>} onClick={() => refetch()}>Refresh</Btn>
          </>
        }
      />

      <div style={{ padding:'24px 32px' }}>
        {isLoading ? (
          <div style={{ display:'flex', justifyContent:'center', padding:60 }}><Spinner size={36}/></div>
        ) : errors.length === 0 ? (
          <EmptyState icon="✅" title="No active errors" message="All EDI files processed successfully." />
        ) : (
          <>
            <div style={{ display:'flex', flexDirection:'column', gap:10 }}>
              {errors.map((e) => (
                <ErrorCard key={e.errorId} error={e} onResolve={() => setResolveTarget(e)}/>
              ))}
            </div>

            {(data?.totalPages ?? 0) > 1 && (
              <div style={{ marginTop:20, display:'flex', justifyContent:'flex-end', gap:8 }}>
                <Btn size="sm" icon={<ChevronLeft size={13}/>} disabled={data?.first} onClick={() => setPage((p) => p-1)}>Prev</Btn>
                <Btn size="sm" icon={<ChevronRight size={13}/>} disabled={data?.last} onClick={() => setPage((p) => p+1)}>Next</Btn>
              </div>
            )}
          </>
        )}
      </div>

      <ResolveModal
        error={resolveTarget}
        onClose={() => setResolveTarget(null)}
      />
    </div>
  );
}

// ── Error card ─────────────────────────────────────────────────────────────────

function ErrorCard({ error: e, onResolve }: { error: EdiError; onResolve: () => void }) {
  const [expanded, setExpanded] = useState(false);
  const color = ERROR_COLORS[e.errorType] ?? 'var(--red)';

  return (
    <div style={{
      background:'var(--card)', border:'1px solid var(--border)',
      borderLeft:`3px solid ${color}`, borderRadius:10,
      padding:'14px 18px', transition:'border-color .2s',
    }}>
      <div style={{ display:'flex', alignItems:'flex-start', justifyContent:'space-between', gap:12 }}>
        <div style={{ flex:1 }}>
          <div style={{ display:'flex', alignItems:'center', gap:10, marginBottom:8, flexWrap:'wrap' }}>
            <Badge color={color}>{e.errorType.replace(/_/g,' ')}</Badge>
            {e.errorCode && (
              <span style={{ fontFamily:"'Fira Code',monospace", fontSize:10, color:'var(--muted)' }}>
                {e.errorCode}
              </span>
            )}
            <Badge color="var(--purple)">File #{e.entryNo}</Badge>
            {e.resolvedFlag && <Badge color="var(--green)">✓ Resolved</Badge>}
          </div>

          <div style={{ fontSize:12, color:'var(--text2)', marginBottom:6 }}>{e.errorMessage}</div>

          {e.fieldPath && (
            <div style={{
              fontFamily:"'Fira Code',monospace", fontSize:11, color:'var(--cyan)',
              background:'rgba(0,212,255,.06)', padding:'3px 10px', borderRadius:5,
              display:'inline-block', marginBottom:8,
            }}>
              {e.fieldPath}
            </div>
          )}

          <div style={{ fontSize:10, color:'var(--muted)', fontFamily:"'Fira Code',monospace" }}>
            {fmtDate(e.timestamp)} · {e.fileName}
          </div>

          {expanded && e.resolvedFlag && (
            <div style={{
              marginTop:10, padding:'8px 12px',
              background:'rgba(16,185,129,.06)', border:'1px solid rgba(16,185,129,.2)', borderRadius:7,
              fontSize:11, color:'var(--green)',
            }}>
              Resolved by {e.resolvedBy} on {fmtDate(e.resolvedAt)}<br/>
              {e.resolutionNote && <span style={{ color:'var(--text2)' }}>{e.resolutionNote}</span>}
            </div>
          )}
        </div>

        <div style={{ display:'flex', gap:8, alignItems:'flex-start', flexShrink:0 }}>
          {e.resolvedFlag && (
            <Btn size="sm" variant="ghost"
              onClick={() => setExpanded(!expanded)}>
              Details
            </Btn>
          )}
          {!e.resolvedFlag && (
            <Btn size="sm" variant="success" icon={<CheckCircle size={12}/>}
              onClick={onResolve}>
              Resolve
            </Btn>
          )}
        </div>
      </div>
    </div>
  );
}

// ── Resolve Modal ─────────────────────────────────────────────────────────────

function ResolveModal({ error, onClose }: { error: EdiError|null; onClose: () => void }) {
  const [note, setNote] = useState('');
  const { mutate: resolve, isPending } = useResolveError();

  const handleResolve = () => {
    if (!error) return;
    resolve(
      { errorId: error.errorId, note },
      { onSuccess: () => { onClose(); setNote(''); } }
    );
  };

  return (
    <Modal open={!!error} onClose={onClose} title="Resolve Error">
      {error && (
        <div style={{ display:'flex', flexDirection:'column', gap:14 }}>
          <div style={{
            background:'rgba(239,68,68,.06)', border:'1px solid rgba(239,68,68,.2)',
            borderRadius:8, padding:'10px 14px', fontSize:12, color:'var(--text2)',
          }}>
            <div style={{ fontWeight:700, color:'var(--red)', marginBottom:6 }}>
              {error.errorType.replace(/_/g,' ')}
            </div>
            {error.errorMessage}
          </div>

          <div>
            <label style={{ display:'block', fontSize:11, fontWeight:700, color:'var(--text2)',
              fontFamily:"'Exo 2',sans-serif", letterSpacing:'.5px', textTransform:'uppercase', marginBottom:6 }}>
              Resolution Note
            </label>
            <textarea
              value={note}
              onChange={(e) => setNote(e.target.value)}
              rows={3}
              placeholder="Describe the resolution action taken…"
              style={{ resize:'vertical' }}
            />
          </div>

          <div style={{ display:'flex', gap:10, justifyContent:'flex-end' }}>
            <Btn variant="ghost" onClick={onClose}>Cancel</Btn>
            <Btn variant="success" loading={isPending}
              icon={<CheckCircle size={13}/>} onClick={handleResolve}>
              Mark Resolved
            </Btn>
          </div>
        </div>
      )}
    </Modal>
  );
}
