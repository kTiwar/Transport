import { useState, type ReactNode } from 'react';
import clsx from 'clsx';
import { ChevronDown, ChevronUp } from 'lucide-react';

type Props = {
  title: string;
  defaultOpen?: boolean;
  children: ReactNode;
  className?: string;
};

export default function FormSection({ title, defaultOpen = true, children, className }: Props) {
  const [open, setOpen] = useState(defaultOpen);

  return (
    <section className={clsx('border-b border-erp-rule bg-white last:border-b-0', className)}>
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        className="flex w-full items-center justify-between border-b border-erp-hairline bg-[#faf9f8] px-4 py-2 text-left font-erp transition-colors hover:bg-[#f3f2f1] focus-visible:outline focus-visible:outline-2 focus-visible:outline-[#107c10]"
      >
        <span className="text-[13px] font-bold text-erp-text">{title}</span>
        <span className="flex items-center gap-1 text-xs font-normal text-[#0078d4]">
          {open ? (
            <>
              Show less <ChevronUp className="h-3.5 w-3.5" aria-hidden />
            </>
          ) : (
            <>
              Show more <ChevronDown className="h-3.5 w-3.5" aria-hidden />
            </>
          )}
        </span>
      </button>
      {open ? <div className="bg-white px-2 py-3 sm:px-4 sm:py-4">{children}</div> : null}
    </section>
  );
}
