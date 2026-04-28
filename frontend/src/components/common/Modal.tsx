import type { ReactNode } from 'react';
import { X } from 'lucide-react';

interface Props {
  open: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
  width?: number | string;
}

export default function Modal({ open, onClose, title, children, width = 520 }: Props) {
  if (!open) return null;
  return (
    <div
      onClick={onClose}
      style={{
        position:'fixed', inset:0,
        background:'rgba(6,9,15,.8)',
        backdropFilter:'blur(4px)',
        display:'flex', alignItems:'center', justifyContent:'center',
        zIndex:1000, padding:20,
      }}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="fade-in"
        style={{
          background:'var(--bg1)',
          border:'1px solid var(--border)',
          borderRadius:14, width, maxWidth:'100%',
          maxHeight:'90vh', overflow:'auto',
        }}
      >
        <div style={{
          padding:'16px 20px',
          borderBottom:'1px solid var(--border)',
          display:'flex', alignItems:'center', justifyContent:'space-between',
        }}>
          <h3 style={{ fontFamily:"'Exo 2',sans-serif", fontSize:15, fontWeight:700 }}>{title}</h3>
          <button onClick={onClose} style={{
            background:'none', border:'none',
            color:'var(--muted)', padding:4, borderRadius:6,
            cursor:'pointer', display:'flex',
          }}><X size={16}/></button>
        </div>
        <div style={{ padding:20 }}>{children}</div>
      </div>
    </div>
  );
}
