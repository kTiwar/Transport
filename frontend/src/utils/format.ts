import { format, formatDistanceToNow, parseISO } from 'date-fns';

export function fmtDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  try { return format(parseISO(iso), 'dd MMM yyyy, HH:mm:ss'); }
  catch { return iso; }
}

export function fmtAgo(iso: string | null | undefined): string {
  if (!iso) return '—';
  try { return formatDistanceToNow(parseISO(iso), { addSuffix: true }); }
  catch { return iso; }
}

export function fmtBytes(bytes: number | null | undefined): string {
  if (bytes == null) return '—';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function fmtNumber(n: number | null | undefined): string {
  if (n == null) return '—';
  return new Intl.NumberFormat().format(n);
}

export function fmtPct(n: number): string {
  return `${n.toFixed(1)}%`;
}
