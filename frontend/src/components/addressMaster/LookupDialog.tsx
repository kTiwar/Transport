import { useEffect, useRef, useState } from 'react';
import { LookupOption } from '../../api/addressLookupApi';

type Props = {
  title: string;
  open: boolean;
  options: LookupOption[];
  loading: boolean;
  onClose: () => void;
  onSearch: (q: string) => void;
  onSelect: (option: LookupOption) => void;
};

export default function LookupDialog({ title, open, options, loading, onClose, onSearch, onSelect }: Props) {
  const [q, setQ] = useState('');
  const onSearchRef = useRef(onSearch);
  onSearchRef.current = onSearch;

  useEffect(() => {
    if (!open) return;
    setQ('');
    onSearchRef.current('');
  }, [open]);

  if (!open) return null;

  return (
    <div style={overlay} onClick={onClose}>
      <div style={dialog} onClick={(e) => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <h3 style={{ margin: 0, fontSize: 16 }}>{title}</h3>
          <button type="button" style={closeBtn} onClick={onClose}>Close</button>
        </div>
        <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
          <input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && onSearch(q.trim())}
            placeholder="Search code or name"
            style={searchInput}
          />
          <button type="button" style={actionBtn} onClick={() => onSearch(q.trim())}>Search</button>
        </div>
        <div style={tableWrap}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
            <thead>
              <tr style={{ background: '#f4f8f2' }}>
                <th style={th}>Code</th>
                <th style={th}>Name</th>
                <th style={th}>Description</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={3} style={td}>Loading...</td></tr>
              ) : options.length === 0 ? (
                <tr><td colSpan={3} style={td}>No records found</td></tr>
              ) : (
                options.map((opt) => (
                  <tr key={`${opt.category}-${opt.code}`} onDoubleClick={() => onSelect(opt)} style={{ cursor: 'pointer' }}>
                    <td style={td}>{opt.code}</td>
                    <td style={td}>{opt.name}</td>
                    <td style={td}>{opt.description || '-'}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

const overlay: React.CSSProperties = {
  position: 'fixed',
  inset: 0,
  background: 'rgba(15, 23, 42, 0.45)',
  display: 'flex',
  justifyContent: 'center',
  alignItems: 'center',
  zIndex: 70,
};

const dialog: React.CSSProperties = {
  width: 'min(840px, 92vw)',
  maxHeight: '80vh',
  background: 'var(--bg2)',
  border: '1px solid var(--border)',
  borderRadius: 8,
  padding: 14,
  color: 'var(--text)',
};

const tableWrap: React.CSSProperties = {
  maxHeight: '58vh',
  overflow: 'auto',
  border: '1px solid var(--border)',
  borderRadius: 6,
};

const th: React.CSSProperties = {
  textAlign: 'left',
  padding: '8px 10px',
  borderBottom: '1px solid var(--border)',
};

const td: React.CSSProperties = {
  padding: '8px 10px',
  borderBottom: '1px solid var(--border)',
};

const actionBtn: React.CSSProperties = {
  border: '1px solid var(--border)',
  background: 'var(--bg3)',
  color: 'var(--text)',
  borderRadius: 6,
  padding: '6px 10px',
};

const closeBtn: React.CSSProperties = {
  ...actionBtn,
  padding: '4px 8px',
};

const searchInput: React.CSSProperties = {
  flex: 1,
  border: '1px solid var(--border)',
  background: 'var(--bg3)',
  color: 'var(--text)',
  borderRadius: 6,
  padding: '8px 10px',
};