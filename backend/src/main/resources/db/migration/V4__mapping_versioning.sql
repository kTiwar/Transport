-- V4: Mapping status column + version history table

ALTER TABLE mapping_header
    ADD COLUMN IF NOT EXISTS status VARCHAR(10) NOT NULL DEFAULT 'DRAFT';

CREATE TABLE IF NOT EXISTS mapping_version_history (
    id              BIGSERIAL       PRIMARY KEY,
    mapping_id      BIGINT          NOT NULL REFERENCES mapping_header(mapping_id) ON DELETE CASCADE,
    version         INTEGER         NOT NULL,
    saved_by        VARCHAR(100),
    saved_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    lines_snapshot  TEXT,
    change_summary  VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_mvh_mapping_id ON mapping_version_history(mapping_id);
CREATE INDEX IF NOT EXISTS idx_mvh_saved_at   ON mapping_version_history(mapping_id, saved_at DESC);
