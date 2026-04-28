-- Generic code/name lists (currencies, incoterms, modes, etc.)
CREATE TABLE reference_master (
    id           BIGSERIAL PRIMARY KEY,
    category     VARCHAR(64)  NOT NULL,
    code         VARCHAR(64)  NOT NULL,
    name         VARCHAR(256) NOT NULL,
    description  TEXT,
    extra_json   TEXT,
    sort_order   INT,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_reference_master_cat_code UNIQUE (category, code)
);

CREATE INDEX idx_reference_master_cat ON reference_master (category);
CREATE INDEX idx_reference_master_active ON reference_master (is_active);

-- Business parties (customers, carriers, suppliers)
CREATE TABLE master_party (
    id           BIGSERIAL PRIMARY KEY,
    party_type   VARCHAR(32)  NOT NULL,
    party_code   VARCHAR(64)  NOT NULL,
    name         VARCHAR(256) NOT NULL,
    legal_name   VARCHAR(256),
    vat_number   VARCHAR(64),
    country_code VARCHAR(8),
    city         VARCHAR(128),
    email        VARCHAR(256),
    phone        VARCHAR(64),
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_master_party_type_code UNIQUE (party_type, party_code)
);

CREATE INDEX idx_master_party_type ON master_party (party_type);
CREATE INDEX idx_master_party_active ON master_party (is_active);

-- Seed examples (optional; safe to re-run only on empty DB — use INSERT ... WHERE NOT EXISTS pattern per row in app or leave empty)
INSERT INTO reference_master (category, code, name, description, sort_order, is_active) VALUES
('CURRENCY', 'EUR', 'Euro', NULL, 10, true),
('CURRENCY', 'USD', 'US Dollar', NULL, 20, true),
('INCOTERM', 'EXW', 'Ex Works', NULL, 10, true),
('INCOTERM', 'DAP', 'Delivered At Place', NULL, 20, true),
('TRANSPORT_MODE', 'FTL', 'Full truck load', NULL, 10, true),
('TRANSPORT_MODE', 'LTL', 'Less than truck load', NULL, 20, true),
('SERVICE_LEVEL', 'STD', 'Standard', NULL, 10, true),
('EQUIPMENT_TYPE', 'DRY', 'Dry van', NULL, 10, true),
('LOCATION_TYPE', 'WH', 'Warehouse', NULL, 10, true),
('CHARGE_TYPE', 'FUEL', 'Fuel surcharge', NULL, 10, true),
('DOCUMENT_TYPE', 'CMR', 'CMR consignment note', NULL, 10, true),
('UOM', 'KG', 'Kilogram', NULL, 10, true)
ON CONFLICT (category, code) DO NOTHING;
