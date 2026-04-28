-- ============================================================
-- V10 : AI Mapping Learning table
-- Stores accepted field mappings so the AI engine can learn
-- from past decisions and boost confidence on future suggestions.
-- ============================================================

CREATE TABLE IF NOT EXISTS ai_mapping_learning (
    id                    BIGSERIAL     PRIMARY KEY,
    partner_code          VARCHAR(50),
    file_type             VARCHAR(20),
    source_field_path     VARCHAR(500)  NOT NULL,
    target_field          VARCHAR(200)  NOT NULL,
    transformation_rule   VARCHAR(100),
    transformation_params TEXT,
    accepted_count        INTEGER       NOT NULL DEFAULT 1,
    rejected_count        INTEGER       NOT NULL DEFAULT 0,
    -- Confidence boost applied to this pairing in future suggestions (0.0 – 0.30)
    confidence_boost      DECIMAL(5,4)  NOT NULL DEFAULT 0.1000,
    last_accepted_at      TIMESTAMP,
    created_at            TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_ai_learn UNIQUE (partner_code, file_type, source_field_path, target_field)
);

CREATE INDEX IF NOT EXISTS idx_ai_learn_partner ON ai_mapping_learning (partner_code, file_type);
CREATE INDEX IF NOT EXISTS idx_ai_learn_source  ON ai_mapping_learning (source_field_path);
CREATE INDEX IF NOT EXISTS idx_ai_learn_target  ON ai_mapping_learning (target_field);
