import clsx from 'clsx';
import { ArrowLeft, Pencil, Plus, Trash2, Check, Maximize2, Search } from 'lucide-react';

type Props = {
  title: string;
  onBack: () => void;
  editing: boolean;
  onToggleEdit: () => void;
  onNew?: () => void;
  onDelete?: () => void;
  saved: boolean;
  onExpand?: () => void;
  searchQuery: string;
  onSearchChange: (q: string) => void;
};

export default function PageHeader({
  title,
  onBack,
  editing,
  onToggleEdit,
  onNew,
  onDelete,
  saved,
  onExpand,
  searchQuery,
  onSearchChange,
}: Props) {
  return (
    <header className="border-b border-erp-rule bg-white">
      <div className="grid grid-cols-[auto_1fr_auto] items-center gap-2 px-3 py-2.5 sm:gap-3 sm:px-4">
        <div className="flex items-center gap-0.5">
          <button
            type="button"
            onClick={onBack}
            className="flex h-9 w-9 items-center justify-center rounded-sm text-erp-text hover:bg-[#f3f2f1] focus-visible:outline focus-visible:outline-2 focus-visible:outline-[#107c10]"
            aria-label="Back"
          >
            <ArrowLeft className="h-[18px] w-[18px]" strokeWidth={1.5} />
          </button>
          <button
            type="button"
            onClick={onToggleEdit}
            className={clsx(
              'flex h-9 w-9 items-center justify-center rounded-sm hover:bg-[#f3f2f1] focus-visible:outline focus-visible:outline-2 focus-visible:outline-[#107c10]',
              editing ? 'bg-[#edebe9] text-[#0078d4]' : 'text-erp-text',
            )}
            aria-label={editing ? 'Stop editing' : 'Edit'}
            aria-pressed={editing}
          >
            <Pencil className="h-[18px] w-[18px]" strokeWidth={1.5} />
          </button>
          <button
            type="button"
            onClick={onNew}
            className="flex h-9 w-9 items-center justify-center rounded-sm text-erp-text hover:bg-[#f3f2f1] focus-visible:outline focus-visible:outline-2 focus-visible:outline-[#107c10]"
            aria-label="New"
          >
            <Plus className="h-[18px] w-[18px]" strokeWidth={1.5} />
          </button>
          <button
            type="button"
            onClick={onDelete}
            className="flex h-9 w-9 items-center justify-center rounded-sm text-erp-text hover:bg-[#f3f2f1] focus-visible:outline focus-visible:outline-2 focus-visible:outline-[#107c10]"
            aria-label="Delete"
          >
            <Trash2 className="h-[18px] w-[18px]" strokeWidth={1.5} />
          </button>
        </div>

        <h1 className="min-w-0 truncate text-center font-erp text-[18px] font-normal leading-tight text-erp-text sm:text-[22px]">
          {title}
        </h1>

        <div className="flex flex-wrap items-center justify-end gap-2">
          <div className="relative hidden min-w-0 max-w-[200px] sm:block sm:max-w-[220px]">
            <Search
              className="pointer-events-none absolute left-2 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-erp-muted"
              aria-hidden
            />
            <input
              type="search"
              value={searchQuery}
              onChange={(e) => onSearchChange(e.target.value)}
              placeholder="Search"
              className="h-8 w-full rounded-sm border border-erp-field bg-white py-1 pl-8 pr-2 font-erp text-[13px] text-erp-text placeholder:text-erp-muted focus:border-[#107c10] focus:outline-none focus:ring-1 focus:ring-[#107c10]"
              aria-label="Search"
            />
          </div>
          <span
            className={clsx(
              'inline-flex items-center gap-1 whitespace-nowrap text-[11px] font-semibold tracking-wide',
              saved ? 'text-erp-success' : 'text-amber-600',
            )}
          >
            <Check className="h-3.5 w-3.5 shrink-0" strokeWidth={2.5} aria-hidden />
            {saved ? 'SAVED' : 'UNSAVED'}
          </span>
          <button
            type="button"
            onClick={onExpand}
            className="flex h-9 w-9 shrink-0 items-center justify-center rounded-sm text-erp-text hover:bg-[#f3f2f1] focus-visible:outline focus-visible:outline-2 focus-visible:outline-[#107c10]"
            aria-label="Expand"
          >
            <Maximize2 className="h-[18px] w-[18px]" strokeWidth={1.5} />
          </button>
        </div>
      </div>
    </header>
  );
}
