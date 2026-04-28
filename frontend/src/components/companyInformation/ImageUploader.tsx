import { useCallback, useEffect, useRef, useState } from 'react';
import clsx from 'clsx';
import { ImagePlus, X } from 'lucide-react';

type Props = {
  previewUrl: string | null;
  onFile: (file: File | null) => void;
  disabled?: boolean;
};

export default function ImageUploader({ previewUrl, onFile, disabled }: Props) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragOver, setDragOver] = useState(false);

  const onPick = useCallback(
    (files: FileList | null) => {
      const f = files?.[0];
      if (!f || !f.type.startsWith('image/')) return;
      onFile(f);
    },
    [onFile],
  );

  useEffect(() => {
    return () => {
      if (previewUrl?.startsWith('blob:')) URL.revokeObjectURL(previewUrl);
    };
  }, [previewUrl]);

  return (
    <div className="w-full max-w-[320px]">
      <div
        role="button"
        tabIndex={disabled ? -1 : 0}
        title="Select Picture."
        onKeyDown={(e) => {
          if (disabled) return;
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            inputRef.current?.click();
          }
        }}
        onDragOver={(e) => {
          e.preventDefault();
          if (!disabled) setDragOver(true);
        }}
        onDragLeave={() => setDragOver(false)}
        onDrop={(e) => {
          e.preventDefault();
          setDragOver(false);
          if (disabled) return;
          onPick(e.dataTransfer.files);
        }}
        onClick={() => !disabled && inputRef.current?.click()}
        className={clsx(
          'relative flex h-[104px] w-[200px] cursor-pointer flex-col items-center justify-center border border-erp-field bg-white transition-shadow',
          dragOver && 'ring-1 ring-[#107c10]',
          disabled && 'cursor-not-allowed opacity-60',
        )}
      >
        <input
          ref={inputRef}
          type="file"
          accept="image/*"
          className="hidden"
          disabled={disabled}
          onChange={(e) => onPick(e.target.files)}
        />
        {previewUrl ? (
          <>
            <img src={previewUrl} alt="" className="max-h-full max-w-full object-contain p-1" />
            {!disabled ? (
              <button
                type="button"
                className="absolute -right-1 -top-1 flex h-6 w-6 items-center justify-center rounded-full border border-erp-field bg-white text-erp-text shadow-sm hover:bg-[#f3f2f1]"
                onClick={(e) => {
                  e.stopPropagation();
                  onFile(null);
                }}
                aria-label="Remove picture"
              >
                <X className="h-3.5 w-3.5" />
              </button>
            ) : null}
          </>
        ) : (
          <>
            <ImagePlus className="mb-1 h-7 w-7 text-erp-muted" strokeWidth={1.25} />
            <span className="px-2 text-center font-erp text-[11px] text-erp-muted">Select Picture.</span>
          </>
        )}
      </div>
    </div>
  );
}
