-- Backfill tms_order.imp_line_no and imp_order_line.tms_order_no only when unambiguous:
-- exactly one non-original import line exists for the TMS order import entry.

UPDATE tms_order t
SET imp_line_no = (
    SELECT MIN(ol.line_no)
    FROM imp_order_line ol
    WHERE ol.entry_no = t.imp_entry_no
      AND COALESCE(ol.original, FALSE) = FALSE
)
WHERE t.imp_line_no IS NULL
  AND t.imp_entry_no IS NOT NULL
  AND (
    SELECT COUNT(*)
    FROM imp_order_line x
    WHERE x.entry_no = t.imp_entry_no
      AND COALESCE(x.original, FALSE) = FALSE
  ) = 1;

UPDATE imp_order_line l
SET tms_order_no = t.order_no
FROM tms_order t
WHERE l.entry_no = t.imp_entry_no
  AND l.tms_order_no IS NULL
  AND t.imp_line_no IS NOT NULL
  AND t.imp_line_no = l.line_no
  AND COALESCE(l.original, FALSE) = FALSE;