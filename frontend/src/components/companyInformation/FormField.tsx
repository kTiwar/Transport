import clsx from 'clsx';
import type { ReactNode } from 'react';

type Props = {
  label: string;
  searchQuery?: string;
  children: ReactNode;
  className?: string;
  error?: string | null;
};

export default function FormField({ label, searchQuery, children, className, error }: Props) {
  const q = (searchQuery ?? '').trim().toLowerCase();
  if (q && !label.toLowerCase().includes(q)) {
    return null;
  }

  return (
    <div className={clsx('flex min-h-8 items-center gap-0 px-1 py-0.5 sm:px-2', className)}>
      <span className="w-[40%] max-w-[240px] shrink-0 pr-2 font-erp text-[13px] leading-snug text-erp-text">
        {label}
      </span>
      <span
        className="mx-1 min-h-[1px] min-w-[8px] flex-1 self-end border-b border-dotted border-[#a19f9d]"
        aria-hidden
      />
      <div className="flex w-full max-w-[320px] flex-[0_0_auto] flex-col gap-0.5 sm:min-w-[200px] sm:max-w-[360px] sm:flex-[0_0_45%]">
        {children}
        {error ? (
          <span className="text-xs text-red-600" role="alert">
            {error}
          </span>
        ) : null}
      </div>
    </div>
  );
}
