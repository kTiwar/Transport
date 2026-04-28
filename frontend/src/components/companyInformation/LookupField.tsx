import clsx from 'clsx';

const focus =
  'focus:z-10 focus:border-[#107c10] focus:outline-none focus:ring-1 focus:ring-[#107c10]';

type Props = {
  value: string;
  onChange: (v: string) => void;
  disabled?: boolean;
  onLookup?: () => void;
  className?: string;
  'aria-label'?: string;
};

export default function LookupField({
  value,
  onChange,
  disabled,
  onLookup,
  className,
  'aria-label': aria,
}: Props) {
  return (
    <div className={clsx('flex w-full max-w-[320px]', className)}>
      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        disabled={disabled}
        aria-label={aria}
        className={clsx(
          'h-[26px] min-w-0 flex-1 rounded-l-sm border border-r-0 border-erp-field bg-white px-2 font-erp text-[13px] text-erp-text',
          focus,
          disabled && 'cursor-not-allowed bg-[#f3f2f1] text-erp-muted',
        )}
      />
      <button
        type="button"
        disabled={disabled}
        onClick={() => !disabled && onLookup?.()}
        title="Lookup"
        aria-label="Lookup"
        className={clsx(
          'h-[26px] shrink-0 rounded-r-sm border border-erp-field bg-[#f3f2f1] px-2.5 font-erp text-[11px] font-semibold leading-none text-erp-text hover:bg-[#edebe9]',
          'focus-visible:z-10 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-[-1px] focus-visible:outline-[#107c10]',
          disabled && 'cursor-not-allowed opacity-50',
        )}
      >
        …
      </button>
    </div>
  );
}
