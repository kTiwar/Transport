-- Revert per-import-line TMS: one TMS order per import order (XML/header).

DROP INDEX IF EXISTS uq_tms_order_imp_entry_line;
ALTER TABLE tms_order DROP COLUMN IF EXISTS imp_line_no;

DROP INDEX IF EXISTS idx_imp_order_line_tms_order_no;
ALTER TABLE imp_order_line DROP COLUMN IF EXISTS tms_order_no;