-- Partition id when one import staging row creates multiple TMS orders (distinct line/cargo external order ids).
ALTER TABLE tms_order ADD COLUMN IF NOT EXISTS import_external_order_no VARCHAR(80);
CREATE INDEX IF NOT EXISTS idx_tms_order_imp_entry_partition ON tms_order (imp_entry_no, import_external_order_no);