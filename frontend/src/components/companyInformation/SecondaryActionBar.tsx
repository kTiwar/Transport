import { useCallback, useState, type RefObject } from 'react';

export const ACTION_BAR_LINKS: { label: string; scrollId: string | 'top' }[] = [
  { label: 'Report', scrollId: 'top' },
  { label: 'Application Settings', scrollId: 'section-application-settings' },
  { label: 'System Settings', scrollId: 'section-system-settings' },
  { label: 'Currencies', scrollId: 'section-currencies' },
  { label: 'Codes', scrollId: 'section-codes' },
  { label: 'Regional Settings', scrollId: 'section-regional-settings' },
  { label: 'Show Attached', scrollId: 'section-show-attached' },
];

type Props = {
  scrollContainerRef: RefObject<HTMLElement | null>;
  onNavigate?: () => void;
};

export default function SecondaryActionBar({ scrollContainerRef, onNavigate }: Props) {
  const [expanded, setExpanded] = useState(true);

  const scrollTo = useCallback(
    (target: string | 'top') => {
      if (target === 'top') {
        scrollContainerRef.current?.scrollTo({ top: 0, behavior: 'smooth' });
        return;
      }
      const el = document.getElementById(target);
      el?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    },
    [scrollContainerRef],
  );

  return (
    <div className="border-b border-erp-rule bg-[#faf9f8] px-2 py-1 sm:px-3">
      <div className="flex flex-wrap items-center gap-x-0.5 gap-y-1 font-erp text-[13px]">
        {expanded &&
          ACTION_BAR_LINKS.map((link, i) => (
            <span key={link.scrollId + link.label} className="inline-flex items-center">
              {i > 0 ? (
                <span className="mx-1 select-none text-erp-rule" aria-hidden>
                  |
                </span>
              ) : null}
              <button
                type="button"
                onClick={() => scrollTo(link.scrollId)}
                className="rounded-sm px-2 py-1 text-[#0078d4] hover:bg-[#f3f2f1] hover:underline focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-0 focus-visible:outline-[#0078d4]"
              >
                {link.label}
              </button>
            </span>
          ))}
        {expanded ? (
          <span className="mx-1 select-none text-erp-rule" aria-hidden>
            |
          </span>
        ) : null}
        <button
          type="button"
          onClick={() => onNavigate?.()}
          className="rounded-sm px-2 py-1 text-[#0078d4] hover:bg-[#f3f2f1] hover:underline focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-0 focus-visible:outline-[#0078d4]"
        >
          Navigate
        </button>
        <span className="mx-1 select-none text-erp-rule" aria-hidden>
          |
        </span>
        <button
          type="button"
          onClick={() => setExpanded((e) => !e)}
          className="rounded-sm px-2 py-1 text-[#0078d4] hover:bg-[#f3f2f1] hover:underline focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-0 focus-visible:outline-[#0078d4]"
        >
          {expanded ? 'Fewer options' : 'More options'}
        </button>
      </div>
    </div>
  );
}
