import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useFiles, useUploadFile, useDeleteFile } from '../hooks/useFiles';
import { usePartners } from '../hooks/usePartners';
import { fileApi } from '../api/fileApi';
import PageHeader from '../components/common/PageHeader';
import Btn from '../components/common/Btn';
import Modal from '../components/common/Modal';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import { StatusBadge, ModeBadge, FileTypeBadge } from '../components/common/StatusBadge';
import { fmtDate, fmtBytes } from '../utils/format';
import type { ProcessingMode } from '../types';
import { Upload, Search, RefreshCw, Eye, Trash2, Download, ChevronLeft, ChevronRight } from 'lucide-react';
import toast from 'react-hot-toast';

export default function FilesPage() {
  const navigate    = useNavigate();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [uploadOpen, setUploadOpen] = useState(false);

  const { data, isLoading, refetch } = useFiles(page, 50);
  const files = data?.content ?? [];

  const filtered = search.trim()
    ? files.filter((f) =>
        f.fileName.toLowerCase().includes(search.toLowerCase()) ||
        f.partnerCode.toLowerCase().includes(search.toLowerCase()) ||
        f.status.toLowerCase().includes(search.toLowerCase())
      )
    : files;

  return (
    <div className="fade-in">
      <PageHeader
        title="EDI"
        highlight="Files"
        subtitle="Manage all received EDI files. Upload manually, trigger processing, or monitor status."
        actions={
          <>
            <Btn icon={<RefreshCw size={13}/>} onClick={() => refetch()} size="sm">Refresh</Btn>
            <Btn variant="primary" icon={<Upload size={13}/>} onClick={() => setUploadOpen(true)} size="sm">
              Upload File
            </Btn>
          </>
        }
      />

      <div style={{ padding: '20px 32px' }}>
        {/* Search */}
        <div style={{ position:'relative', maxWidth:400, marginBottom:20 }}>
          <Search size={14} style={{ position:'absolute', left:12, top:'50%', transform:'translateY(-50%)', color:'var(--muted)' }}/>
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by name, partner, status…"
            style={{ paddingLeft:36 }}
          />
        </div>

        {/* Table */}
        <div style={{ background:'var(--card)', border:'1px solid var(--border)', borderRadius:12, overflow:'hidden' }}>
          {isLoading ? (
            <div style={{ padding:60, display:'flex', justifyContent:'center' }}><Spinner/></div>
          ) : filtered.length === 0 ? (
            <EmptyState icon="📭" title="No files found" message="Upload a file or adjust your search filter." />
          ) : (
            <>
              <div style={{ overflowX:'auto' }}>
                <table style={{ width:'100%', borderCollapse:'collapse', fontSize:12 }}>
                  <thead>
                    <tr style={{ background:'var(--bg2)' }}>
                      {['Entry #','File Name','Partner','Type','Size','Mode','Status','Received','Actions'].map((h) => (
                        <th key={h} style={{
                          padding:'11px 14px', textAlign:'left',
                          fontSize:10, fontWeight:700, letterSpacing:'.6px',
                          color:'var(--cyan)', textTransform:'uppercase',
                          fontFamily:"'Exo 2',sans-serif",
                          borderBottom:'1px solid var(--border)',
                          whiteSpace:'nowrap',
                        }}>{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {filtered.map((f) => (
                      <FileRow key={f.entryNo} file={f} onView={() => navigate(`/files/${f.entryNo}`)} />
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Pagination */}
              {(data?.totalPages ?? 0) > 1 && (
                <div style={{
                  padding:'12px 16px', borderTop:'1px solid var(--border)',
                  display:'flex', alignItems:'center', justifyContent:'space-between',
                }}>
                  <span style={{ fontSize:11, color:'var(--muted)' }}>
                    Page {page+1} of {data?.totalPages} — {data?.totalElements} total files
                  </span>
                  <div style={{ display:'flex', gap:8 }}>
                    <Btn size="sm" icon={<ChevronLeft size={13}/>} disabled={data?.first}
                      onClick={() => setPage((p) => p-1)}>Prev</Btn>
                    <Btn size="sm" icon={<ChevronRight size={13}/>} disabled={data?.last}
                      onClick={() => setPage((p) => p+1)}>Next</Btn>
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </div>

      <UploadModal open={uploadOpen} onClose={() => setUploadOpen(false)} />
    </div>
  );
}

// ── File Row ──────────────────────────────────────────────────────────────────

function FileRow({ file: f, onView }: { file: import('../types').TmsFile; onView: () => void }) {
  const { mutate: deleteFile, isPending: deleting } = useDeleteFile();
  const [downloading, setDownloading] = useState(false);

  const handleDownload = async () => {
    setDownloading(true);
    try {
      await fileApi.download(f.entryNo, f.fileName);
      toast.success(`Downloaded ${f.fileName}`);
    } catch {
      toast.error('Download failed');
    } finally {
      setDownloading(false);
    }
  };

  const tdStyle = {
    padding:'10px 14px', borderBottom:'1px solid var(--border)',
    verticalAlign:'middle' as const,
  };

  return (
    <tr
      onMouseEnter={(e) => (e.currentTarget.style.background='rgba(255,255,255,.02)')}
      onMouseLeave={(e) => (e.currentTarget.style.background='transparent')}
    >
      <td style={{...tdStyle, fontFamily:"'Fira Code',monospace", color:'var(--muted)', fontSize:11}}>{f.entryNo}</td>
      <td style={{...tdStyle, maxWidth:200}}>
        <div style={{ overflow:'hidden',textOverflow:'ellipsis',whiteSpace:'nowrap', fontWeight:600 }}>
          {f.fileName}
        </div>
        {f.errorMessage && (
          <div style={{ fontSize:10, color:'var(--red)', marginTop:2, overflow:'hidden',textOverflow:'ellipsis',whiteSpace:'nowrap' }}>
            ⚠ {f.errorMessage}
          </div>
        )}
      </td>
      <td style={{...tdStyle, color:'var(--text2)'}}>{f.partnerCode}</td>
      <td style={tdStyle}><FileTypeBadge type={f.fileType}/></td>
      <td style={{...tdStyle, fontFamily:"'Fira Code',monospace", color:'var(--muted)', fontSize:11}}>{fmtBytes(f.fileSize)}</td>
      <td style={tdStyle}><ModeBadge mode={f.processingMode}/></td>
      <td style={tdStyle}><StatusBadge status={f.status}/></td>
      <td style={{...tdStyle, fontSize:11, fontFamily:"'Fira Code',monospace", color:'var(--muted)', whiteSpace:'nowrap'}}>
        {fmtDate(f.receivedTimestamp)}
      </td>
      <td style={{...tdStyle}}>
        <div style={{ display:'flex', gap:6 }}>
          <Btn size="sm" icon={<Eye size={12}/>} onClick={onView}>View</Btn>
          <Btn size="sm" variant="ghost" icon={downloading ? undefined : <Download size={12}/>}
            loading={downloading}
            onClick={handleDownload}
            title="Download raw file">
            {downloading ? '' : 'DL'}
          </Btn>
          <Btn size="sm" variant="danger" icon={deleting ? undefined : <Trash2 size={12}/>}
            loading={deleting}
            onClick={() => { if (confirm(`Delete file #${f.entryNo}?`)) deleteFile(f.entryNo); }}>
            Del
          </Btn>
        </div>
      </td>
    </tr>
  );
}

// ── Upload Modal ──────────────────────────────────────────────────────────────

function UploadModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const fileRef = useRef<HTMLInputElement>(null);
  const [partnerId, setPartnerId] = useState('');
  const [mode, setMode] = useState<ProcessingMode>('MANUAL');
  const [selectedFile, setSelectedFile] = useState<File|null>(null);

  const { data: partners = [] } = usePartners();
  const { mutate: upload, isPending } = useUploadFile();

  const handleSubmit = () => {
    if (!selectedFile || !partnerId) return;
    upload(
      { file: selectedFile, partnerId: Number(partnerId), mode },
      { onSuccess: () => { onClose(); setSelectedFile(null); setPartnerId(''); } }
    );
  };

  const labelStyle = {
    display:'block' as const, fontSize:11, fontWeight:700 as const, color:'var(--text2)',
    fontFamily:"'Exo 2',sans-serif", letterSpacing:'.5px' as const,
    textTransform:'uppercase' as const, marginBottom:6,
  };

  return (
    <Modal open={open} onClose={onClose} title="Upload EDI File">
      <div style={{ display:'flex', flexDirection:'column', gap:16 }}>

        {/* File picker */}
        <div>
          <label style={labelStyle}>File</label>
          <div
            onClick={() => fileRef.current?.click()}
            style={{
              border:'2px dashed var(--border)', borderRadius:9,
              padding:'24px 16px', textAlign:'center', cursor:'pointer',
              background: selectedFile ? 'rgba(16,185,129,.06)' : 'var(--bg2)',
              borderColor: selectedFile ? 'var(--green)' : 'var(--border)',
              transition:'all .2s',
            }}
          >
            {selectedFile ? (
              <div>
                <div style={{ color:'var(--green)', fontWeight:700, fontSize:13 }}>✓ {selectedFile.name}</div>
                <div style={{ fontSize:11, color:'var(--muted)', marginTop:4 }}>{fmtBytes(selectedFile.size)}</div>
              </div>
            ) : (
              <div style={{ color:'var(--muted)', fontSize:12 }}>
                <div style={{ fontSize:28, marginBottom:8 }}>📁</div>
                Click to browse or drag & drop
                <div style={{ fontSize:10, marginTop:4 }}>XML, JSON, CSV, XLSX, EDI, X12</div>
              </div>
            )}
          </div>
          <input ref={fileRef} type="file" style={{ display:'none' }}
            accept=".xml,.json,.csv,.txt,.xlsx,.xls,.edi,.x12"
            onChange={(e) => setSelectedFile(e.target.files?.[0] ?? null)} />
        </div>

        {/* Partner */}
        <div>
          <label style={labelStyle}>Partner</label>
          <select value={partnerId} onChange={(e) => setPartnerId(e.target.value)}>
            <option value="">Select partner…</option>
            {partners.map((p) => (
              <option key={p.partnerId} value={p.partnerId}>{p.partnerCode} — {p.partnerName}</option>
            ))}
          </select>
        </div>

        {/* Mode */}
        <div>
          <label style={labelStyle}>Processing Mode</label>
          <select value={mode} onChange={(e) => setMode(e.target.value as ProcessingMode)}>
            <option value="AUTO">AUTO — Process immediately</option>
            <option value="MANUAL">MANUAL — Wait for operator trigger</option>
            <option value="SCHEDULED">SCHEDULED — Include in next batch run</option>
          </select>
        </div>

        <div style={{ display:'flex', gap:10, justifyContent:'flex-end' }}>
          <Btn variant="ghost" onClick={onClose}>Cancel</Btn>
          <Btn variant="primary" loading={isPending} disabled={!selectedFile || !partnerId}
            onClick={handleSubmit}>
            Upload File
          </Btn>
        </div>
      </div>
    </Modal>
  );
}
