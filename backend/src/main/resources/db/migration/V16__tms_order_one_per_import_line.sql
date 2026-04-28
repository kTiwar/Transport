-- One TMS order per import order line: link TMS order to staging line and store TMS no. on import line.

ALTER TABLE tms_order ADD COLUMN IF NOT EXISTS imp_line_no INTEGER;

CREATE UNIQUE INDEX IF NOT EXISTS uq_tms_order_imp_entry_line
    ON tms_order (imp_entry_no, imp_line_no)
    WHERE imp_line_no IS NOT NULL;

ALTER TABLE imp_order_line ADD COLUMN IF NOT EXISTS tms_order_no VARCHAR(20);

CREATE INDEX IF NOT EXISTS idx_imp_order_line_tms_order_no ON imp_order_line (tms_order_no);