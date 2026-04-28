import type { ReactNode, CSSProperties } from 'react';

interface Props {
  title?: string;
  icon?: ReactNode;
  accent?: string;
  children: ReactNode;
  style?: CSSProperties;
  noPad?: boolean;
}

export default function Card({ title, icon, accent = 'var(--cyan)', children, style, noPad }: Props) {
  return (
    <div style={{
      background: 'var(--card)',
      border: '1px solid var(--border)',
      borderRadius: 12,
      overflow: 'hidden',
      transition: 'border-color .2s, box-shadow .2s',
      ...style,
    }}>
      {title && (
        <div style={{
          padding: '14px 18px',
          borderBottom: '1px solid var(--border)',
          display: 'flex', alignItems: 'center', gap: 8,
          fontFamily: "'Exo 2', sans-serif", fontSize: 12,
          fontWeight: 700, letterSpacing: '.5px', textTransform: 'uppercase',
          color: accent,
        }}>
          {icon}
          {title}
        </div>
      )}
      <div style={noPad ? {} : { padding: '18px' }}>
        {children}
      </div>
    </div>
  );
}
