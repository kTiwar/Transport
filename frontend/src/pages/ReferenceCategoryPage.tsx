import { useEffect, useState, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Library, Loader2, ChevronLeft } from 'lucide-react';
import { mastersApi, ReferenceMasterRow } from '../api/mastersApi';

export default function ReferenceCategoryPage() {
  const { category } = useParams<{ category: string }>();
  const [rows, setRows] = useState<ReferenceMasterRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const load = useCallback(async () => {
    if (!category) return;
    setLoading(true);
    try {
      const r = await mastersApi.referenceList(category, page, 50, true);
      setRows(r.content);
      setTotalPages(r.totalPages);
    } finally {
      setLoading(false);
    }
  }, [category, page]);

  useEffect(() => {
    load();
  }, [load]);

  if (!category) return null;

  return (
    <div style={{ padding: '24px 28px', fontFamily: "'Exo 2',sans-serif" }}>
      <div style={{ marginBottom: 16 }}>
        <Link
          to="/masters"
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: 6,
            fontSize: 12,
            color: 'var(--cyan)',
            textDecoration: 'none',
          }}
        >
          <ChevronLeft size={14} /> Master data
        </Link>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
        <Library size={20} color="var(--green)" />
        <h1 style={{ margin: 0, fontSize: 18, color: 'var(--text)', fontWeight: 700 }}>{category}</h1>
      </div>

      <div style={{ background: 'var(--bg2)', border: '1px solid var(--border)', borderRadius: 10, overflow: 'hidden' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <thead>
            <tr style={{ background: 'var(--bg3)', borderBottom: '1px solid var(--border)' }}>
              {['Code', 'Name', 'Description', 'Sort', 'Active'].map((h) => (
                <th
                  key={h}
                  style={{
                    padding: '10px 12px',
                    textAlign: 'left',
                    color: 'var(--muted)',
                    fontSize: 10,
                    textTransform: 'uppercase',
                  }}
                >
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={5} style={{ textAlign: 'center', padding: 40 }}>
                  <Loader2 size={20} />
                </td>
              </tr>
            ) : rows.length === 0 ? (
              <tr>
                <td colSpan={5} style={{ textAlign: 'center', padding: 32, color: 'var(--muted)' }}>
                  No rows in this category yet.
                </td>
              </tr>
            ) : (
              rows.map((r) => (
                <tr key={r.id} style={{ borderBottom: '1px solid var(--border)' }}>
                  <td style={{ padding: '9px 12px', fontFamily: "'Fira Code',monospace", color: '#10b981' }}>{r.code}</td>
                  <td style={{ padding: '9px 12px' }}>{r.name}</td>
                  <td style={{ padding: '9px 12px', color: 'var(--text2)' }}>{r.description || '—'}</td>
                  <td style={{ padding: '9px 12px' }}>{r.sortOrder ?? '—'}</td>
                  <td style={{ padding: '9px 12px' }}>{r.isActive ? 'Yes' : 'No'}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div style={{ display: 'flex', gap: 12, marginTop: 14, alignItems: 'center' }}>
          <button
            type="button"
            disabled={page <= 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            style={{ padding: '6px 12px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--bg3)' }}
          >
            Previous
          </button>
          <span style={{ fontSize: 12, color: 'var(--muted)' }}>
            Page {page + 1} / {totalPages}
          </span>
          <button
            type="button"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
            style={{ padding: '6px 12px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--bg3)' }}
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}
