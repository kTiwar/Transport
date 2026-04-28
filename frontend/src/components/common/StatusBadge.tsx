import type { FileStatus, ProcessingMode, FileType } from '../../types';
import clsx from 'clsx';

// ── Status badges ─────────────────────────────────────────────────────────────

const STATUS_COLORS: Record<FileStatus, { bg: string; color: string; dot?: boolean }> = {
  RECEIVED:   { bg: 'rgba(0,212,255,.12)',   color: 'var(--cyan)',   dot: true },
  PENDING:    { bg: 'rgba(139,92,246,.12)',  color: 'var(--purple)', dot: true },
  PROCESSING: { bg: 'rgba(245,158,11,.12)', color: 'var(--yellow)', dot: true },
  PROCESSED:  { bg: 'rgba(16,185,129,.12)', color: 'var(--green)',  dot: true },
  ERROR:      { bg: 'rgba(239,68,68,.12)',  color: 'var(--red)',    dot: true },
  DELETED:    { bg: 'rgba(90,112,128,.12)', color: 'var(--muted)',  dot: false },
};

export function StatusBadge({ status }: { status: FileStatus }) {
  const s = STATUS_COLORS[status] ?? STATUS_COLORS.RECEIVED;
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: 5,
      padding: '3px 10px', borderRadius: 20,
      background: s.bg, color: s.color,
      fontSize: 11, fontWeight: 600,
      fontFamily: "'Fira Code', monospace",
      border: `1px solid ${s.color}44`,
    }}>
      {s.dot && <span style={{ width: 6, height: 6, borderRadius: '50%', background: s.color }} />}
      {status}
    </span>
  );
}

// ── Processing mode badge ─────────────────────────────────────────────────────

export function ModeBadge({ mode }: { mode: ProcessingMode }) {
  const cfg: Record<ProcessingMode, { color: string; bg: string }> = {
    AUTO:      { color: 'var(--cyan)',   bg: 'rgba(0,212,255,.1)' },
    MANUAL:    { color: 'var(--purple)', bg: 'rgba(139,92,246,.1)' },
    SCHEDULED: { color: 'var(--yellow)', bg: 'rgba(245,158,11,.1)' },
  };
  const s = cfg[mode] ?? cfg.AUTO;
  return (
    <span style={{
      padding: '2px 8px', borderRadius: 12,
      background: s.bg, color: s.color,
      fontSize: 10, fontWeight: 700,
      fontFamily: "'Fira Code', monospace",
      border: `1px solid ${s.color}44`,
    }}>{mode}</span>
  );
}

// ── File type badge ───────────────────────────────────────────────────────────

const TYPE_COLORS: Record<FileType, string> = {
  XML:     'var(--cyan)',
  JSON:    'var(--green)',
  CSV:     'var(--yellow)',
  TXT:     'var(--muted)',
  EDIFACT: 'var(--red)',
  X12:     'var(--purple)',
  EXCEL:   'var(--orange)',
};

export function FileTypeBadge({ type }: { type: FileType }) {
  const color = TYPE_COLORS[type] ?? 'var(--muted)';
  return (
    <span style={{
      padding: '2px 8px', borderRadius: 12,
      background: `${color}18`, color,
      fontSize: 10, fontWeight: 700,
      fontFamily: "'Fira Code', monospace",
      border: `1px solid ${color}44`,
    }}>{type}</span>
  );
}

// ── Generic badge ─────────────────────────────────────────────────────────────

export function Badge({
  children,
  color = 'var(--cyan)',
}: {
  children: React.ReactNode;
  color?: string;
}) {
  return (
    <span style={{
      padding: '2px 9px', borderRadius: 12,
      background: `${color}15`, color,
      fontSize: 11, fontWeight: 600,
      fontFamily: "'Fira Code', monospace",
      border: `1px solid ${color}44`,
    }}>{children}</span>
  );
}
