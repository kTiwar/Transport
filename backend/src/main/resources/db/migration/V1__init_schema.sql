-- ============================================================
-- TMS EDI Integration Platform — Initial Database Schema
-- PostgreSQL 15+  |  Flyway Migration V1
-- ============================================================

-- ── Partners ────────────────────────────────────────────────
CREATE TABLE edi_partners (
    partner_id      BIGSERIAL PRIMARY KEY,
    partner_code    VARCHAR(50)  NOT NULL UNIQUE,
    partner_name    VARCHAR(200) NOT NULL,
    default_format  VARCHAR(20),
    processing_mode VARCHAR(10)  NOT NULL DEFAULT 'AUTO',
    sftp_config     TEXT,
    api_key         VARCHAR(100),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    sla_hours       INT          NOT NULL DEFAULT 24,
    contact_email   VARCHAR(200),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_partners_code   ON edi_partners (partner_code);
CREATE INDEX idx_partners_apikey ON edi_partners (api_key);

-- ── TMS Files ───────────────────────────────────────────────
CREATE TABLE tms_files (
    entry_no              BIGSERIAL PRIMARY KEY,
    file_name             VARCHAR(500)  NOT NULL,
    file_type             VARCHAR(20)   NOT NULL,
    partner_id            BIGINT        NOT NULL REFERENCES edi_partners (partner_id),
    received_timestamp    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    processing_mode       VARCHAR(10)   NOT NULL DEFAULT 'AUTO',
    status                VARCHAR(20)   NOT NULL DEFAULT 'RECEIVED',
    file_size             BIGINT,
    checksum              VARCHAR(64),
    storage_path          VARCHAR(1000),
    file_content          BYTEA,
    error_message         TEXT,
    retry_count           INT           NOT NULL DEFAULT 0,
    processed_timestamp   TIMESTAMPTZ,
    original_mapping_id   BIGINT,
    order_count           INT,
    is_deleted            BOOLEAN       NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_tms_files_partner_id ON tms_files (partner_id);
CREATE INDEX idx_tms_files_status     ON tms_files (status);
CREATE INDEX idx_tms_files_checksum   ON tms_files (checksum) WHERE is_deleted = FALSE;
CREATE INDEX idx_tms_files_received   ON tms_files (received_timestamp DESC);

-- ── Mapping Header ──────────────────────────────────────────
CREATE TABLE mapping_header (
    mapping_id    BIGSERIAL PRIMARY KEY,
    partner_id    BIGINT        NOT NULL REFERENCES edi_partners (partner_id),
    file_type     VARCHAR(20)   NOT NULL,
    mapping_name  VARCHAR(200)  NOT NULL,
    version       INT           NOT NULL DEFAULT 1,
    active_flag   BOOLEAN       NOT NULL DEFAULT FALSE,
    description   TEXT,
    created_by    VARCHAR(100),
    created_date  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_date  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_mapping_partner_type ON mapping_header (partner_id, file_type);
CREATE INDEX idx_mapping_active       ON mapping_header (active_flag);

-- ── Mapping Lines ───────────────────────────────────────────
CREATE TABLE mapping_lines (
    mapping_line_id       BIGSERIAL PRIMARY KEY,
    mapping_id            BIGINT        NOT NULL REFERENCES mapping_header (mapping_id) ON DELETE CASCADE,
    source_field_path     VARCHAR(500),
    target_field          VARCHAR(200)  NOT NULL,
    transformation_rule   VARCHAR(200),
    transformation_params TEXT,
    default_value         VARCHAR(500),
    is_required           BOOLEAN       NOT NULL DEFAULT FALSE,
    sequence              INT           NOT NULL DEFAULT 0,
    condition_rule        TEXT,
    lookup_table_name     VARCHAR(100)
);

CREATE INDEX idx_mapping_lines_header ON mapping_lines (mapping_id);

-- ── EDI Order Header (Staging) ──────────────────────────────
CREATE TABLE edi_order_header (
    id                       BIGSERIAL PRIMARY KEY,
    entry_no                 BIGINT       NOT NULL REFERENCES tms_files (entry_no),
    partner_id               BIGINT       NOT NULL REFERENCES edi_partners (partner_id),
    external_order_id        VARCHAR(100) NOT NULL,
    customer_code            VARCHAR(50),
    order_date               DATE,
    requested_delivery_date  DATE,
    origin_address           TEXT,
    destination_address      TEXT,
    incoterm                 VARCHAR(10),
    priority                 VARCHAR(20)  DEFAULT 'NORMAL',
    status                   VARCHAR(30)  DEFAULT 'NEW',
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_order_partner UNIQUE (external_order_id, partner_id)
);

CREATE INDEX idx_eoh_entry_no   ON edi_order_header (entry_no);
CREATE INDEX idx_eoh_partner_id ON edi_order_header (partner_id);
CREATE INDEX idx_eoh_ext_order  ON edi_order_header (external_order_id, partner_id);

-- ── EDI Order Lines (Staging) ───────────────────────────────
CREATE TABLE edi_order_lines (
    id                BIGSERIAL PRIMARY KEY,
    order_header_id   BIGINT          NOT NULL REFERENCES edi_order_header (id) ON DELETE CASCADE,
    entry_no          BIGINT          NOT NULL REFERENCES tms_files (entry_no),
    line_number       INT             NOT NULL,
    item_code         VARCHAR(100),
    description       TEXT,
    quantity          NUMERIC(18,4),
    unit_of_measure   VARCHAR(20)     DEFAULT 'KG',
    weight_kg         NUMERIC(12,3),
    volume_m3         NUMERIC(12,4),
    external_line_ref VARCHAR(50)
);

CREATE INDEX idx_eol_order_header ON edi_order_lines (order_header_id);
CREATE INDEX idx_eol_entry_no     ON edi_order_lines (entry_no);

-- ── EDI Cargo Details (Staging) ────────────────────────────
CREATE TABLE edi_cargo_details (
    id                   BIGSERIAL PRIMARY KEY,
    entry_no             BIGINT          NOT NULL REFERENCES tms_files (entry_no),
    cargo_type           VARCHAR(50),
    total_weight         NUMERIC(14,3),
    total_volume         NUMERIC(14,4),
    pallet_count         INT,
    hazmat_flag          BOOLEAN         DEFAULT FALSE,
    temperature_req      VARCHAR(30),
    special_instructions TEXT,
    created_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ecd_entry_no ON edi_cargo_details (entry_no);

-- ── EDI Costs (Staging) ────────────────────────────────────
CREATE TABLE edi_costs (
    id                   BIGSERIAL PRIMARY KEY,
    entry_no             BIGINT          NOT NULL REFERENCES tms_files (entry_no),
    charge_type          VARCHAR(50),
    amount               NUMERIC(14,2),
    currency             VARCHAR(3)         DEFAULT 'EUR',
    vat_amount           NUMERIC(12,2),
    external_charge_code VARCHAR(50),
    created_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ec_entry_no ON edi_costs (entry_no);

-- ── EDI Error Log ───────────────────────────────────────────
CREATE TABLE edi_error_log (
    error_id        BIGSERIAL PRIMARY KEY,
    entry_no        BIGINT       NOT NULL REFERENCES tms_files (entry_no),
    mapping_line_id BIGINT,
    error_type      VARCHAR(50)  NOT NULL,
    error_code      VARCHAR(20),
    error_message   TEXT         NOT NULL,
    field_path      VARCHAR(500),
    timestamp       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    resolved_flag   BOOLEAN      NOT NULL DEFAULT FALSE,
    resolved_by     VARCHAR(100),
    resolved_at     TIMESTAMPTZ,
    resolution_note TEXT
);

CREATE INDEX idx_eel_entry_no   ON edi_error_log (entry_no);
CREATE INDEX idx_eel_resolved   ON edi_error_log (resolved_flag);
CREATE INDEX idx_eel_error_type ON edi_error_log (error_type);

-- ── Audit Log ───────────────────────────────────────────────
CREATE TABLE audit_log (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(100) NOT NULL,
    action        VARCHAR(50)  NOT NULL,
    resource_type VARCHAR(50),
    resource_id   VARCHAR(100),
    details       TEXT,
    ip_address    VARCHAR(45),
    timestamp     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_al_username  ON audit_log (username);
CREATE INDEX idx_al_timestamp ON audit_log (timestamp DESC);
CREATE INDEX idx_al_action    ON audit_log (action);

-- ── Application Users (for login) ──────────────────────────
CREATE TABLE app_users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(200) NOT NULL,
    full_name     VARCHAR(200),
    email         VARCHAR(200),
    role          VARCHAR(20)  NOT NULL DEFAULT 'VIEWER',
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_username ON app_users (username);

-- ── Lookup Tables ───────────────────────────────────────────
CREATE TABLE edi_lookup_table (
    id            BIGSERIAL PRIMARY KEY,
    table_name    VARCHAR(100) NOT NULL,
    source_value  VARCHAR(200) NOT NULL,
    target_value  VARCHAR(200) NOT NULL,
    partner_id    BIGINT       REFERENCES edi_partners (partner_id),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_lookup UNIQUE (table_name, source_value, partner_id)
);

CREATE INDEX idx_lookup_name ON edi_lookup_table (table_name, source_value);

-- ── Seed default admin user (password: Admin@2026!) ─────────
INSERT INTO app_users (username, password_hash, full_name, email, role)
VALUES (
    'admin',
    '$2a$10$jYQ09ftp6WY4XCCYzY6yluFzo5Qyy4eZBYICnb3JQKU6E38t4s6Gy',
    'System Administrator',
    'admin@tms.com',
    'ADMIN'
);

-- ── Seed example partner ─────────────────────────────────────
INSERT INTO edi_partners (partner_code, partner_name, default_format, processing_mode, sla_hours, contact_email)
VALUES ('ACME_LOGISTICS', 'ACME Logistics Ltd.', 'XML', 'AUTO', 4, 'edi@acme-logistics.com');

