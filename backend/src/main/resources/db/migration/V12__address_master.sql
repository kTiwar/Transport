-- Address master data model (PostgreSQL)

CREATE TABLE address_master (
    address_id          BIGSERIAL PRIMARY KEY,
    address_code        VARCHAR(64)  NOT NULL,
    address_type        VARCHAR(64),
    entity_type         VARCHAR(64),
    entity_id           BIGINT,
    address_line1       VARCHAR(512),
    address_line2       VARCHAR(512),
    address_line3       VARCHAR(512),
    landmark            VARCHAR(256),
    city                VARCHAR(128),
    district            VARCHAR(128),
    state_province      VARCHAR(128),
    postal_code         VARCHAR(32),
    country_code        VARCHAR(8),
    country_name        VARCHAR(128),
    latitude            DOUBLE PRECISION,
    longitude           DOUBLE PRECISION,
    timezone            VARCHAR(64),
    is_primary          BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    validation_status   VARCHAR(32),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_address_master_code UNIQUE (address_code)
);

CREATE INDEX idx_address_master_entity ON address_master (entity_type, entity_id);
CREATE INDEX idx_address_master_city ON address_master (city);
CREATE INDEX idx_address_master_active ON address_master (is_active);

CREATE TABLE address_contact (
    contact_id              BIGSERIAL PRIMARY KEY,
    address_id              BIGINT NOT NULL REFERENCES address_master (address_id) ON DELETE CASCADE,
    contact_name            VARCHAR(256),
    phone_number            VARCHAR(64),
    alternate_phone         VARCHAR(64),
    email                   VARCHAR(256),
    is_primary_contact      BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_address_contact_address ON address_contact (address_id);

CREATE TABLE address_attributes (
    attr_id     BIGSERIAL PRIMARY KEY,
    address_id  BIGINT NOT NULL REFERENCES address_master (address_id) ON DELETE CASCADE,
    attr_key    VARCHAR(128) NOT NULL,
    attr_value  TEXT
);

CREATE INDEX idx_address_attributes_address ON address_attributes (address_id);

CREATE TABLE address_usage (
    usage_id    BIGSERIAL PRIMARY KEY,
    address_id  BIGINT NOT NULL REFERENCES address_master (address_id) ON DELETE CASCADE,
    usage_type  VARCHAR(64),
    priority    INT
);

CREATE INDEX idx_address_usage_address ON address_usage (address_id);

CREATE TABLE address_i18n (
    id              BIGSERIAL PRIMARY KEY,
    address_id      BIGINT NOT NULL REFERENCES address_master (address_id) ON DELETE CASCADE,
    language_code   VARCHAR(16) NOT NULL,
    address_text    TEXT,
    CONSTRAINT uq_address_i18n_lang UNIQUE (address_id, language_code)
);

CREATE INDEX idx_address_i18n_address ON address_i18n (address_id);

CREATE TABLE address_audit (
    audit_id    BIGSERIAL PRIMARY KEY,
    address_id  BIGINT NOT NULL REFERENCES address_master (address_id) ON DELETE CASCADE,
    changed_by  VARCHAR(128),
    old_value   TEXT,
    new_value   TEXT,
    changed_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_address_audit_address ON address_audit (address_id);
CREATE INDEX idx_address_audit_changed_at ON address_audit (changed_at DESC);
