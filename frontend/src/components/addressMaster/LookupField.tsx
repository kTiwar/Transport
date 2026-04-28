import { LookupOption } from '../../api/addressLookupApi';

type Props = {
  label: string;
  value: LookupOption | null;
  required?: boolean;
  onOpen: () => void;
  onClear?: () => void;
  disabled?: boolean;
};

export default function LookupField({ label, value, required, onOpen, onClear, disabled }: Props) {
  return (
    <div>
      <div style={{ fontSize: 11, color: 'var(--muted)', marginBottom: 4 }}>
        {label} {required ? '*' : ''}
      </div>
      <div style={{ display: 'flex', gap: 6 }}>
        <input
          value={value ? `${value.code} - ${value.name}` : ''}
          placeholder={`Select ${label}`}
          readOnly
          style={inputStyle}
          disabled={disabled}
        />
        <button type="button" onClick={onOpen} disabled={disabled} style={btnStyle}>Lookup</button>
        <button type="button" onClick={onClear} disabled={disabled || !value} style={btnStyle}>Clear</button>
      </div>
    </div>
  );
}

const inputStyle: React.CSSProperties = {
  flex: 1,
  border: '1px solid var(--border)',
  background: 'var(--bg3)',
  color: 'var(--text)',
  borderRadius: 6,
  padding: '8px 10px',
  fontSize: 12,
};

const btnStyle: React.CSSProperties = {
  border: '1px solid var(--border)',
  background: 'var(--bg3)',
  color: 'var(--text)',
  borderRadius: 6,
  padding: '6px 10px',
  fontSize: 12,
};