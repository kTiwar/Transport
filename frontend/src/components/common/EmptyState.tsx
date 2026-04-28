import type { ReactNode } from 'react';

interface Props { icon?: ReactNode; title: string; message?: string; action?: ReactNode }

export default function EmptyState({ icon = '📭', title, message, action }: Props) {
  return (
    <div style={{
      display:'flex', flexDirection:'column', alignItems:'center', justifyContent:'center',
      gap: 12, padding: '60px 20px', color: 'var(--muted)',
    }}>
      <div style={{ fontSize: 42, opacity:.5 }}>{icon}</div>
      <div style={{ fontFamily:"'Exo 2',sans-serif", fontWeight:700, fontSize:15, color:'var(--text2)' }}>{title}</div>
      {message && <div style={{ fontSize:12, textAlign:'center', maxWidth:320 }}>{message}</div>}
      {action}
    </div>
  );
}
