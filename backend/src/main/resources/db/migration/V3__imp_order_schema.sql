-- ============================================================
-- V3 : EDI Import Order Processing Schema
-- Mirrors AL codeunit 71102979/71102980 tables
-- ============================================================

-- ── Communication Partner configuration ─────────────────────────
CREATE TABLE IF NOT EXISTS cfg_communication_partner (
    code                          VARCHAR(20)  PRIMARY KEY,
    name                          VARCHAR(100),
    error_recipients              VARCHAR(250),
    default_customer_no           VARCHAR(20),

    customer_no_is_internal       BOOLEAN  DEFAULT FALSE,
    office_is_internal            BOOLEAN  DEFAULT FALSE,
    cust_serv_resp_is_internal    BOOLEAN  DEFAULT FALSE,
    sales_responsible_is_internal BOOLEAN  DEFAULT FALSE,
    action_code_is_internal       BOOLEAN  DEFAULT FALSE,
    address_no_is_internal        BOOLEAN  DEFAULT FALSE,
    good_no_is_internal           BOOLEAN  DEFAULT FALSE,
    uom_code_is_internal          BOOLEAN  DEFAULT FALSE,
    good_type_is_internal         BOOLEAN  DEFAULT FALSE,
    good_sub_type_is_internal     BOOLEAN  DEFAULT FALSE,
    reference_code_is_internal    BOOLEAN  DEFAULT FALSE,
    carrier_is_internal           BOOLEAN  DEFAULT FALSE,
    revenue_is_internal           BOOLEAN  DEFAULT FALSE,
    revenue_type_is_internal      BOOLEAN  DEFAULT FALSE,
    currency_code_is_internal     BOOLEAN  DEFAULT FALSE,
    equipment_type_is_internal    BOOLEAN  DEFAULT FALSE,
    equipment_sub_type_is_internal BOOLEAN DEFAULT FALSE,

    auto_insert_address           BOOLEAN  DEFAULT FALSE,
    auto_insert_city              BOOLEAN  DEFAULT FALSE,
    revalidate_address_mapping    BOOLEAN  DEFAULT FALSE,
    is_web_portal                 BOOLEAN  DEFAULT FALSE,
    adr_mapping                   BOOLEAN  DEFAULT FALSE,
    active                        BOOLEAN  DEFAULT TRUE
);

-- ── Import Mapping lookup table ──────────────────────────────────
CREATE TABLE IF NOT EXISTS cfg_import_mapping (
    id                     BIGSERIAL    PRIMARY KEY,
    communication_partner  VARCHAR(20)  NOT NULL,
    mapping_type           VARCHAR(30)  NOT NULL,
    foreign_id             VARCHAR(100) NOT NULL,
    local_id               VARCHAR(100) NOT NULL,
    description            VARCHAR(200),
    active                 BOOLEAN      DEFAULT TRUE,
    UNIQUE (communication_partner, mapping_type, foreign_id)
);
CREATE INDEX IF NOT EXISTS idx_imp_mapping_lookup
    ON cfg_import_mapping(communication_partner, mapping_type, foreign_id);

-- ── Import Order Header (staging) ───────────────────────────────
CREATE TABLE IF NOT EXISTS imp_order_header (
    entry_no               BIGSERIAL    PRIMARY KEY,
    communication_partner  VARCHAR(20)  NOT NULL,
    external_order_no      VARCHAR(80),
    external_customer_no   VARCHAR(30),
    transaction_type       VARCHAR(20)  NOT NULL DEFAULT 'INSERT_ORDER',
    status                 VARCHAR(20)  NOT NULL DEFAULT 'RECEIVED',
    tms_order_no           VARCHAR(20),

    transport_type         VARCHAR(20),
    trip_type_no           VARCHAR(20),
    office                 VARCHAR(20),
    cust_serv_responsible  VARCHAR(20),
    sales_responsible      VARCHAR(20),
    carrier_no             VARCHAR(20),
    web_portal_user        VARCHAR(100),
    traction_order         VARCHAR(20),

    neutral_shipment       BOOLEAN,
    ns_add_name            VARCHAR(100),
    ns_add_street          VARCHAR(100),
    ns_add_city_pc         VARCHAR(50),

    cash_on_delivery_type  VARCHAR(20),
    cash_on_delivery_amount DECIMAL(18,2),

    country_of_origin      VARCHAR(10),
    country_of_destination VARCHAR(10),
    order_date             TIMESTAMP,

    received_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    processed_at           TIMESTAMP,
    error_message          VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_imp_hdr_partner   ON imp_order_header(communication_partner);
CREATE INDEX IF NOT EXISTS idx_imp_hdr_status    ON imp_order_header(status);
CREATE INDEX IF NOT EXISTS idx_imp_hdr_ext_order ON imp_order_header(external_order_no);

-- ── Import Order Line (staging) ──────────────────────────────────
CREATE TABLE IF NOT EXISTS imp_order_line (
    entry_no               BIGINT       NOT NULL,
    line_no                INT          NOT NULL,
    communication_partner  VARCHAR(20),
    external_order_no      VARCHAR(80),

    action_code            VARCHAR(20),
    external_address_no    VARCHAR(50),
    address_name           VARCHAR(100),
    address_street         VARCHAR(100),
    address_number         VARCHAR(10),
    address_city           VARCHAR(50),
    address_country_code   VARCHAR(10),
    address_postal_code    VARCHAR(20),

    initial_datetime_from  TIMESTAMP,
    initial_datetime_until TIMESTAMP,
    requested_datetime_from  TIMESTAMP,
    requested_datetime_until TIMESTAMP,
    booked_datetime_from   TIMESTAMP,
    booked_datetime_until  TIMESTAMP,
    closing_datetime       TIMESTAMP,

    order_line_ref1        VARCHAR(50),
    order_line_ref2        VARCHAR(50),
    container_no           VARCHAR(20),
    loaded                 BOOLEAN,
    original               BOOLEAN      DEFAULT FALSE,
    local_order_line_id    BIGINT,

    PRIMARY KEY (entry_no, line_no),
    FOREIGN KEY (entry_no) REFERENCES imp_order_header(entry_no) ON DELETE CASCADE
);

-- ── Import Order Cargo (staging) ─────────────────────────────────
CREATE TABLE IF NOT EXISTS imp_order_cargo (
    entry_no               BIGINT       NOT NULL,
    line_no                INT          NOT NULL,
    communication_partner  VARCHAR(20),
    external_order_no      VARCHAR(80),
    order_line_no          INT,

    external_good_no       VARCHAR(50),
    external_good_type     VARCHAR(50),
    external_good_sub_type VARCHAR(50),

    quantity               DECIMAL(18,4),
    unit_of_measure_code   VARCHAR(20),
    pallet_places          DECIMAL(18,4),
    loading_meters         DECIMAL(18,4),
    force_loading_meters   BOOLEAN,

    description            VARCHAR(50),
    description2           VARCHAR(50),

    net_weight             DECIMAL(18,4),
    gross_weight           DECIMAL(18,4),
    width                  DECIMAL(18,4),
    length                 DECIMAL(18,4),
    height                 DECIMAL(18,4),
    diameter               DECIMAL(18,4),

    adr_type               VARCHAR(20),
    dangerous_goods        BOOLEAN,
    adr_dangerous_for_environment BOOLEAN,
    adr_un_no              VARCHAR(20),
    adr_hazard_class       VARCHAR(20),
    adr_packing_group      VARCHAR(20),
    adr_tunnel_restriction_code VARCHAR(20),

    set_temperature        DECIMAL(10,2),
    temperature            DECIMAL(10,2),
    min_temperature        DECIMAL(10,2),
    max_temperature        DECIMAL(10,2),

    tracing_no1            VARCHAR(50),
    tracing_no2            VARCHAR(50),
    original               BOOLEAN      DEFAULT FALSE,

    PRIMARY KEY (entry_no, line_no),
    FOREIGN KEY (entry_no) REFERENCES imp_order_header(entry_no) ON DELETE CASCADE
);

-- ── Import Order Reference (staging) ─────────────────────────────
CREATE TABLE IF NOT EXISTS imp_order_reference (
    entry_no               BIGINT       NOT NULL,
    line_no                INT          NOT NULL,
    communication_partner  VARCHAR(20),
    external_order_no      VARCHAR(80),

    reference_code         VARCHAR(20),
    reference              VARCHAR(100),
    order_line_no          INT          DEFAULT 0,
    original               BOOLEAN      DEFAULT FALSE,

    PRIMARY KEY (entry_no, line_no),
    FOREIGN KEY (entry_no) REFERENCES imp_order_header(entry_no) ON DELETE CASCADE
);

-- ── Import Order Equipment (staging) ─────────────────────────────
CREATE TABLE IF NOT EXISTS imp_order_equipment (
    entry_no               BIGINT       NOT NULL,
    line_no                INT          NOT NULL,
    communication_partner  VARCHAR(20),
    equipment_type_no      VARCHAR(50),
    equipment_sub_type_no  VARCHAR(50),
    material_type          VARCHAR(20)  DEFAULT '-',
    quantity               INT,

    PRIMARY KEY (entry_no, line_no),
    FOREIGN KEY (entry_no) REFERENCES imp_order_header(entry_no) ON DELETE CASCADE
);

-- ── Import Transport Cost (staging) ──────────────────────────────
CREATE TABLE IF NOT EXISTS imp_transport_cost (
    entry_no               BIGINT       NOT NULL,
    line_no                INT          NOT NULL,
    communication_partner  VARCHAR(20),
    revenue_code           VARCHAR(20),
    revenue_type           VARCHAR(20),
    unit_of_measure_budget VARCHAR(20),
    currency_actual        VARCHAR(10),
    amount_actual          DECIMAL(18,4),
    amount_budget          DECIMAL(18,4),
    quantity_budget        DECIMAL(18,4),

    PRIMARY KEY (entry_no, line_no),
    FOREIGN KEY (entry_no) REFERENCES imp_order_header(entry_no) ON DELETE CASCADE
);

-- ── Import Processing Log ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS imp_processing_log (
    id           BIGSERIAL    PRIMARY KEY,
    entry_no     BIGINT       NOT NULL,
    tms_order_no VARCHAR(20),
    log_type     VARCHAR(20)  NOT NULL DEFAULT 'ERROR',
    message      VARCHAR(1000),
    field_name   VARCHAR(100),
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by   VARCHAR(50)
);
CREATE INDEX IF NOT EXISTS idx_imp_log_entry ON imp_processing_log(entry_no);

-- ── TMS Address ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tms_address (
    no             VARCHAR(20) PRIMARY KEY,
    name           VARCHAR(100),
    street         VARCHAR(100),
    number         VARCHAR(10),
    city           VARCHAR(50),
    postal_code    VARCHAR(20),
    country_code   VARCHAR(10),
    city_id        BIGINT,
    latitude       DECIMAL(12,8),
    longitude      DECIMAL(12,8),
    auto_inserted  BOOLEAN DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_tms_addr_country ON tms_address(country_code, postal_code, city);

-- ── TMS Order (final) ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tms_order (
    id                       BIGSERIAL    PRIMARY KEY,
    order_no                 VARCHAR(20)  NOT NULL UNIQUE,
    customer_no              VARCHAR(20),
    transport_type           VARCHAR(20),
    trip_type_no             VARCHAR(20),
    office                   VARCHAR(20),
    cust_serv_responsible    VARCHAR(20),
    sales_responsible        VARCHAR(20),
    carrier_no               VARCHAR(20),
    communication_partner    VARCHAR(20),
    source                   VARCHAR(30)  DEFAULT 'ORDER_IMPORT',
    web_portal_user          VARCHAR(100),

    neutral_shipment         BOOLEAN,
    ns_add_name              VARCHAR(100),
    ns_add_street            VARCHAR(100),
    ns_add_city_pc           VARCHAR(50),

    cash_on_delivery_type    VARCHAR(20),
    cash_on_delivery_amount  DECIMAL(18,2),

    country_of_origin        VARCHAR(10),
    country_of_destination   VARCHAR(10),

    order_date               TIMESTAMP    NOT NULL DEFAULT NOW(),
    status                   VARCHAR(20)  DEFAULT 'OPEN',
    imp_entry_no             BIGINT
);
CREATE INDEX IF NOT EXISTS idx_tms_ord_customer ON tms_order(customer_no);
CREATE INDEX IF NOT EXISTS idx_tms_ord_status   ON tms_order(status);
CREATE INDEX IF NOT EXISTS idx_tms_ord_imp      ON tms_order(imp_entry_no);

-- ── TMS Order Line ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tms_order_line (
    id                   BIGSERIAL   PRIMARY KEY,
    tms_order_id         BIGINT      NOT NULL REFERENCES tms_order(id) ON DELETE CASCADE,
    line_no              INT         NOT NULL,
    sorting_key          INT,
    action_code          VARCHAR(20),
    address_no           VARCHAR(20),

    initial_datetime_from    TIMESTAMP,
    initial_datetime_until   TIMESTAMP,
    requested_datetime_from  TIMESTAMP,
    requested_datetime_until TIMESTAMP,
    booked_datetime_from     TIMESTAMP,
    booked_datetime_until    TIMESTAMP,
    closing_datetime         TIMESTAMP,

    order_line_ref1      VARCHAR(50),
    order_line_ref2      VARCHAR(50),
    container_no         VARCHAR(20),
    loaded               BOOLEAN,
    source               VARCHAR(20) DEFAULT 'IMP_ORD',
    planning_block_id    BIGINT,
    order_line_id        BIGINT,

    UNIQUE (tms_order_id, line_no)
);

-- ── TMS Order Cargo ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tms_order_cargo (
    id                BIGSERIAL   PRIMARY KEY,
    tms_order_id      BIGINT      NOT NULL REFERENCES tms_order(id) ON DELETE CASCADE,
    line_no           INT         NOT NULL,

    good_no           VARCHAR(20),
    good_type_code    VARCHAR(20),
    good_sub_type_code VARCHAR(20),

    quantity          DECIMAL(18,4),
    qty_per_uom       DECIMAL(18,4),
    unit_of_measure_code VARCHAR(20),
    description       VARCHAR(50),
    description2      VARCHAR(50),

    adr_type          VARCHAR(20),
    dangerous_goods   BOOLEAN,
    adr_dangerous_for_environment BOOLEAN,
    adr_un_no         VARCHAR(20),
    hazard_class      VARCHAR(20),
    packing_group     VARCHAR(20),
    tunnel_restriction_code VARCHAR(20),

    set_temperature   DECIMAL(10,2),
    temperature       DECIMAL(10,2),
    min_temperature   DECIMAL(10,2),
    max_temperature   DECIMAL(10,2),

    net_weight        DECIMAL(18,4),
    gross_weight      DECIMAL(18,4),
    width             DECIMAL(18,4),
    length            DECIMAL(18,4),
    height            DECIMAL(18,4),
    diameter          DECIMAL(18,4),
    pallet_places     DECIMAL(18,4),
    loading_meters    DECIMAL(18,4),
    tracing_no1       VARCHAR(50),
    tracing_no2       VARCHAR(50),

    UNIQUE (tms_order_id, line_no)
);

-- ── TMS Order Reference ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tms_order_reference (
    id             BIGSERIAL   PRIMARY KEY,
    tms_order_id   BIGINT      NOT NULL REFERENCES tms_order(id) ON DELETE CASCADE,
    reference_code VARCHAR(20) NOT NULL,
    reference      VARCHAR(100),
    customer_no    VARCHAR(20),
    order_line_no  INT         DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_tms_ref_code ON tms_order_reference(reference_code, reference);

-- ── Seed: default communication partner ──────────────────────────
INSERT INTO cfg_communication_partner (code, name, active)
VALUES ('DEFAULT', 'Default EDI Partner', true)
ON CONFLICT DO NOTHING;
