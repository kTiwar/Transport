import type { ReactNode, ButtonHTMLAttributes } from 'react';

type Variant = 'primary' | 'secondary' | 'danger' | 'ghost' | 'success' | 'warning';
type Size    = 'sm' | 'md' | 'lg';

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  icon?: ReactNode;
  loading?: boolean;
  children: ReactNode;
}

const VARIANTS: Record<Variant, { bg: string; color: string; border: string; hover: string }> = {
  primary:   { bg:'linear-gradient(135deg,var(--cyan),var(--cyan2))',  color:'#000', border:'transparent', hover:'filter:brightness(1.1)' },
  secondary: { bg:'rgba(0,212,255,.08)',   color:'var(--cyan)',    border:'rgba(0,212,255,.3)',   hover:'background:rgba(0,212,255,.15)' },
  danger:    { bg:'rgba(239,68,68,.1)',    color:'var(--red)',     border:'rgba(239,68,68,.35)',  hover:'background:rgba(239,68,68,.2)' },
  ghost:     { bg:'transparent',           color:'var(--text2)',   border:'var(--border)',        hover:'background:var(--bg2)' },
  success:   { bg:'rgba(16,185,129,.1)',   color:'var(--green)',   border:'rgba(16,185,129,.35)', hover:'background:rgba(16,185,129,.2)' },
  warning:   { bg:'rgba(245,158,11,.1)',   color:'var(--yellow)',  border:'rgba(245,158,11,.35)', hover:'background:rgba(245,158,11,.2)' },
};

const SIZES: Record<Size, { padding: string; fontSize: string }> = {
  sm: { padding:'5px 12px',  fontSize:'11px' },
  md: { padding:'8px 16px',  fontSize:'12px' },
  lg: { padding:'11px 22px', fontSize:'13px' },
};

export default function Btn({
  variant = 'secondary', size = 'md',
  icon, loading, children, style, disabled, ...rest
}: Props) {
  const v = VARIANTS[variant];
  const s = SIZES[size];
  return (
    <button
      {...rest}
      disabled={disabled || loading}
      style={{
        display: 'inline-flex', alignItems: 'center', gap: 7,
        padding: s.padding, fontSize: s.fontSize,
        fontFamily: "'Exo 2', sans-serif", fontWeight: 700, letterSpacing: '.3px',
        background: v.bg, color: v.color,
        border: `1px solid ${v.border}`,
        borderRadius: 8, cursor: disabled || loading ? 'not-allowed' : 'pointer',
        opacity: disabled || loading ? .6 : 1,
        transition: 'all .15s',
        whiteSpace: 'nowrap',
        ...style,
      }}
    >
      {loading
        ? <span style={{ width:12,height:12,border:'2px solid currentColor',borderTopColor:'transparent',borderRadius:'50%',animation:'spin 1s linear infinite',flexShrink:0 }} />
        : icon}
      {children}
    </button>
  );
}
