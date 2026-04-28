import { useParams, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { useFile, useFileStructure, useProcessFile, useRetryFile } from '../hooks/useFiles';
import { useFileErrors } from '../hooks/useErrors';
import { fileApi } from '../api/fileApi';
import PageHeader from '../components/common/PageHeader';
import Card from '../components/common/Card';
import Btn from '../components/common/Btn';
import Spinner from '../components/common/Spinner';
import { StatusBadge, ModeBadge, FileTypeBadge, Badge } from '../components/common/StatusBadge';
import { fmtDate, fmtBytes } from '../utils/format';
import SchemaTreeViewer from '../components/mapping/SchemaTreeViewer';
import {
  ArrowLeft, Play, RefreshCw, Download, FileText,
  GitBranch, AlertTriangle, Info,
} from 'lucide-react';
import type { SchemaNode } from '../types';
import toast from 'react-hot-toast';

const TAB_ITEMS = ['Overview', 'Structure', 'Errors'] as const;
type Tab = typeof TAB_ITEMS[number];

export default function FileDetailPage() {
  const { entryNo }   = useParams<{ entryNo: string }>();
  const navigate      = useNavigate();
  const id            = entryNo ? Number(entryNo) : null;
  const [tab, setTab] = useState<Tab>('Overview');

  const { data: file, isLoading } = useFile(id);
  const { mutate: processFile, isPending: processing } = useProcessFile();
  const { mutate: retryFile,   isPending: retrying }   = useRetryFile();
  const [downloading, setDownloading] = useState(false);

  const handleDownload = async () => {
    if (!file) return;
    setDownloading(true);
    try {
      await fileApi.download(file.entryNo, file.fileName);
      toast.success(`Downloaded ${file.fileName}`);
    } catch {
      toast.error('Download failed');
    } finally {
      setDownloading(false);
    }
  };

  if (isLoading) return (
    <div style={{ display:'flex', justifyContent:'center', padding:80 }}><Spinner size={36}/></div>
  );
  if (!file) return (
    <div style={{ padding:40, color:'var(--muted)' }}>File not found.</div>
  );

  const canProcess =
    file.status === 'PENDING' ||
    file.status === 'RECEIVED' ||
    file.status === 'ERROR';
  /** Reset stuck PROCESSING or clear an ERROR before re-run. */
  const canRetry = file.status === 'ERROR' || file.status === 'PROCESSING';

  return (
    <div className="fade-in">
      <PageHeader
        title={`File`}
        highlight={`#${file.entryNo}`}
        subtitle={file.fileName}
        actions={
          <>
            <Btn size="sm" icon={<ArrowLeft size={13}/>} variant="ghost" onClick={() => navigate('/files')}>
              Back
            </Btn>
            {canProcess && (
              <Btn size="sm" variant="success" icon={<Play size={13}/>} loading={processing}
                onClick={() => processFile({ entryNo: file.entryNo })}>
                Process
              </Btn>
            )}
            {canRetry && (
              <Btn size="sm" variant="warning" icon={<RefreshCw size={13}/>} loading={retrying}
                onClick={() => retryFile(file.entryNo)}
                title={file.status === 'PROCESSING' ? 'Clear stuck PROCESSING so you can run Process again' : 'Reset failed file to try again'}>
                {file.status === 'PROCESSING' ? 'Reset' : 'Retry'}
              </Btn>
            )}
            <Btn size="sm" icon={<Download size={13}/>} loading={downloading} onClick={handleDownload}>
              Download
            </Btn>
          </>
        }
      />

      {/* Tab bar */}
      <div style={{
        display:'flex', borderBottom:'1px solid var(--border)',
        padding:'0 32px', background:'var(--bg1)',
      }}>
        {TAB_ITEMS.map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            style={{
              padding:'13px 20px',
              fontSize:12, fontFamily:"'Exo 2',sans-serif", fontWeight:700,
              letterSpacing:'.5px', textTransform:'uppercase' as const,
              color: tab === t ? 'var(--cyan)' : 'var(--muted)',
              background:'none', border:'none', borderBottom: tab===t ? '2px solid var(--cyan)' : '2px solid transparent',
              cursor:'pointer', transition:'all .15s',
              display:'flex', alignItems:'center', gap:7,
            }}
          >
            {t === 'Overview'  && <Info size={13}/>}
            {t === 'Structure' && <GitBranch size={13}/>}
            {t === 'Errors'    && <AlertTriangle size={13}/>}
            {t}
          </button>
        ))}
      </div>

      <div style={{ padding:'24px 32px' }}>
        {tab === 'Overview'  && <OverviewTab  file={file}/>}
        {tab === 'Structure' && <StructureTab entryNo={file.entryNo}/>}
        {tab === 'Errors'    && <ErrorsTab    entryNo={file.entryNo}/>}
      </div>
    </div>
  );
}

// ── Overview tab ──────────────────────────────────────────────────────────────

function OverviewTab({ file }: { file: import('../types').TmsFile }) {
  const rows: [string, React.ReactNode][] = [
    ['Entry No',        <span style={{ fontFamily:"'Fira Code',monospace" }}>{file.entryNo}</span>],
    ['File Name',       file.fileName],
    ['File Type',       <FileTypeBadge type={file.fileType}/>],
    ['File Size',       fmtBytes(file.fileSize)],
    ['Checksum (SHA256)', <span style={{ fontFamily:"'Fira Code',monospace", fontSize:11 }}>{file.checksum}</span>],
    ['Partner',         `${file.partnerCode} — ${file.partnerName}`],
    ['Processing Mode', <ModeBadge mode={file.processingMode}/>],
    ['Status',          <StatusBadge status={file.status}/>],
    ['Retry Count',     file.retryCount],
    ['Order Count',     file.orderCount ?? '—'],
    ['Received',        fmtDate(file.receivedTimestamp)],
    ['Processed',       fmtDate(file.processedTimestamp)],
    ['Storage Path',    <span style={{ fontFamily:"'Fira Code',monospace", fontSize:11 }}>{file.storagePath}</span>],
  ];

  return (
    <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:20 }}>
      <Card title="File Details" icon={<FileText size={13}/>}>
        <div style={{ display:'grid', gap:0 }}>
          {rows.map(([label, val]) => (
            <div key={String(label)} style={{
              display:'grid', gridTemplateColumns:'140px 1fr', gap:12,
              padding:'10px 0', borderBottom:'1px solid var(--border)',
              alignItems:'center', fontSize:12,
            }}>
              <span style={{ color:'var(--muted)', fontWeight:600 }}>{label}</span>
              <span>{val}</span>
            </div>
          ))}
        </div>
      </Card>

      {file.status === 'ERROR' && file.errorMessage && (
        <Card title="Error Details" icon={<AlertTriangle size={13}/>} accent="var(--red)">
          <div style={{
            background:'rgba(239,68,68,.06)', border:'1px solid rgba(239,68,68,.2)',
            borderRadius:8, padding:'12px 16px',
            fontSize:12, color:'var(--text2)', lineHeight:1.8,
            fontFamily:"'Fira Code',monospace",
          }}>
            {file.errorMessage}
          </div>
          <div style={{ marginTop:14, display:'flex', gap:10 }}>
            <Btn size="sm" variant="warning">Retry Processing</Btn>
            <Btn size="sm" variant="secondary">View Error Log</Btn>
          </div>
        </Card>
      )}
    </div>
  );
}

// ── Structure tab ─────────────────────────────────────────────────────────────

function StructureTab({ entryNo }: { entryNo: number }) {
  const { data: schema, isLoading } = useFileStructure(entryNo);

  if (isLoading) return <div style={{ display:'flex', justifyContent:'center', padding:40 }}><Spinner/></div>;
  if (!schema)  return <div style={{ color:'var(--muted)', padding:20 }}>No structure available yet.</div>;

  return (
    <Card title="File Structure Explorer" icon={<GitBranch size={13}/>}>
      <p style={{ fontSize:11, color:'var(--muted)', marginBottom:16 }}>
        Hierarchical schema tree with field paths. These paths are used in the Mapping Designer.
      </p>
      <SchemaTreeViewer node={schema}/>
    </Card>
  );
}

// ── Errors tab ────────────────────────────────────────────────────────────────

function ErrorsTab({ entryNo }: { entryNo: number }) {
  const { data: errors = [], isLoading } = useFileErrors(entryNo);
  if (isLoading) return <div style={{ display:'flex', justifyContent:'center', padding:40 }}><Spinner/></div>;
  if (!errors.length) return (
    <div style={{ textAlign:'center', padding:40, color:'var(--green)', fontSize:14 }}>
      ✓ No errors for this file.
    </div>
  );

  return (
    <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
      {errors.map((e) => (
        <div key={e.errorId} style={{
          background:'var(--card)', border:'1px solid var(--border)', borderLeft:'3px solid var(--red)',
          borderRadius:10, padding:'14px 18px',
        }}>
          <div style={{ display:'flex', alignItems:'center', gap:10, marginBottom:8 }}>
            <Badge color="var(--red)">{e.errorType.replace(/_/g,' ')}</Badge>
            {e.errorCode && <span style={{ fontFamily:"'Fira Code',monospace", fontSize:11, color:'var(--muted)' }}>{e.errorCode}</span>}
            {e.resolvedFlag && <Badge color="var(--green)">✓ Resolved</Badge>}
          </div>
          <div style={{ fontSize:12, color:'var(--text2)', marginBottom:8 }}>{e.errorMessage}</div>
          {e.fieldPath && (
            <div style={{ fontFamily:"'Fira Code',monospace", fontSize:11, color:'var(--cyan)',
              background:'rgba(0,212,255,.06)', padding:'4px 10px', borderRadius:5, display:'inline-block' }}>
              {e.fieldPath}
            </div>
          )}
          <div style={{ fontSize:10, color:'var(--muted)', marginTop:8 }}>{fmtDate(e.timestamp)}</div>
        </div>
      ))}
    </div>
  );
}
