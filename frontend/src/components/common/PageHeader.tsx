import type { ReactNode } from 'react';

interface Props {
  title: string;
  highlight?: string;
  subtitle?: string;
  actions?: ReactNode;
}

export default function PageHeader({ title, highlight, subtitle, actions }: Props) {
  return (
    <div style={{
      padding: '28px 32px 20px',
      borderBottom: '1px solid var(--border)',
      display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 16,
      background: 'linear-gradient(180deg, var(--bg1) 0%, var(--bg0) 100%)',
    }}>
      <div>
        <h1 style={{ fontSize: 24, fontWeight: 900, letterSpacing: '-0.5px', marginBottom: 4 }}>
          {title}{highlight && <span style={{ color: 'var(--cyan)' }}> {highlight}</span>}
        </h1>
        {subtitle && <p style={{ fontSize: 12, color: 'var(--muted)', maxWidth: 600 }}>{subtitle}</p>}
      </div>
      {actions && <div style={{ display: 'flex', gap: 10, alignItems: 'center', flexShrink: 0 }}>{actions}</div>}
    </div>
  );
}
