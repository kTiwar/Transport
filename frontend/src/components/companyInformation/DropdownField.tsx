import clsx from 'clsx';

type Option = { value: string; label: string };

type Props = {
  id?: string;
  options: Option[];
  value: string;
  onChange: (v: string) => void;
  disabled?: boolean;
  className?: string;
};

export default function DropdownField({ id, options, value, onChange, disabled, className }: Props) {
  return (
    <div className={clsx('relative w-full max-w-[320px]', className)}>
      <select
        id={id}
        value={value}
        disabled={disabled}
        onChange={(e) => onChange(e.target.value)}
        className={clsx(
          'h-[26px] w-full appearance-none rounded-sm border border-erp-field bg-white py-0.5 pl-2 pr-7 font-erp text-[13px] text-erp-text',
          'focus:border-[#107c10] focus:outline-none focus:ring-1 focus:ring-[#107c10]',
          disabled && 'cursor-not-allowed bg-[#f3f2f1] text-erp-muted',
        )}
      >
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
      <span
        className="pointer-events-none absolute right-2 top-1/2 -mt-px border-[5px] border-transparent border-t-erp-muted"
        aria-hidden
      />
    </div>
  );
}
