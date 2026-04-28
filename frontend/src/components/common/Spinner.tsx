export default function Spinner({ size = 24, color = 'var(--cyan)' }: { size?: number; color?: string }) {
  return (
    <div style={{
      width: size, height: size,
      border: `2px solid ${color}30`,
      borderTopColor: color,
      borderRadius: '50%',
      animation: 'spin 1s linear infinite',
      flexShrink: 0,
    }} />
  );
}

export function FullPageSpinner() {
  return (
    <div style={{
      display:'flex', alignItems:'center', justifyContent:'center',
      height:'100vh', background:'var(--bg0)',
    }}>
      <Spinner size={40} />
    </div>
  );
}
