import clsx from 'clsx';

type Props = {
  checked: boolean;
  onChange: (next: boolean) => void;
  disabled?: boolean;
  id?: string;
  /** Hide On/Off caption (Business Central–style row with label only). */
  compact?: boolean;
  'aria-label'?: string;
};

export default function ToggleSwitch({
  checked,
  onChange,
  disabled,
  id,
  compact,
  'aria-label': aria,
}: Props) {
  return (
    <button
      id={id}
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={aria}
      disabled={disabled}
      onClick={() => !disabled && onChange(!checked)}
      onKeyDown={(e) => {
        if (disabled) return;
        if (e.key === ' ' || e.key === 'Enter') {
          e.preventDefault();
          onChange(!checked);
        }
      }}
      className={clsx(
        'inline-flex items-center gap-2 rounded-sm border-0 bg-transparent p-0.5 font-erp text-[13px] text-erp-text',
        'focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-erp-accent',
        disabled && 'cursor-not-allowed opacity-50',
        !disabled && 'cursor-pointer',
      )}
    >
      <span
        className={clsx(
          'relative h-[18px] w-[34px] shrink-0 rounded-full transition-colors',
          checked ? 'bg-erp-success' : 'bg-[#c8c6c4]',
        )}
      >
        <span
          className={clsx(
            'absolute top-0.5 h-[14px] w-[14px] rounded-full bg-white shadow transition-[left]',
            checked ? 'left-[18px]' : 'left-0.5',
          )}
        />
      </span>
      {!compact ? <span className="min-w-[28px] text-left">{checked ? 'On' : 'Off'}</span> : null}
    </button>
  );
}
