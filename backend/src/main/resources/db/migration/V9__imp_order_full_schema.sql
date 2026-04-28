-- ============================================================
-- V9 : Full Import Order Schema Expansion
-- Adds all missing columns to existing imp_order_* tables
-- and creates new order-related child tables.
-- All changes use IF NOT EXISTS / DO-blocks for safety.
-- ============================================================

-- ──────────────────────────────────────────────────────────────
-- 1. EXPAND imp_order_header
-- ──────────────────────────────────────────────────────────────
ALTER TABLE imp_order_header
    ADD COLUMN IF NOT EXISTS order_description          VARCHAR(150),
    ADD COLUMN IF NOT EXISTS description_2              VARCHAR(80),
    ADD COLUMN IF NOT EXISTS search_description         VARCHAR(190),
    ADD COLUMN IF NOT EXISTS order_date_only            DATE,
    ADD COLUMN IF NOT EXISTS first_order_line_date      DATE,
    ADD COLUMN IF NOT EXISTS collection_date            TIMESTAMP,
    ADD COLUMN IF NOT EXISTS delivery_date              TIMESTAMP,

    -- Customer / Sell-to
    ADD COLUMN IF NOT EXISTS customer_name              VARCHAR(100),
    ADD COLUMN IF NOT EXISTS customer_name_2            VARCHAR(50),
    ADD COLUMN IF NOT EXISTS customer_search_name       VARCHAR(50),
    ADD COLUMN IF NOT EXISTS sell_to_address            VARCHAR(50),
    ADD COLUMN IF NOT EXISTS sell_to_address_2          VARCHAR(50),
    ADD COLUMN IF NOT EXISTS sell_to_city               VARCHAR(30),
    ADD COLUMN IF NOT EXISTS sell_to_contact            VARCHAR(50),
    ADD COLUMN IF NOT EXISTS sell_to_post_code          VARCHAR(20),
    ADD COLUMN IF NOT EXISTS sell_to_county             VARCHAR(30),
    ADD COLUMN IF NOT EXISTS sell_to_country_region_code VARCHAR(10),
    ADD COLUMN IF NOT EXISTS vat_registration_no        VARCHAR(20),
    ADD COLUMN IF NOT EXISTS bound_name                 VARCHAR(45),

    -- Bill-to
    ADD COLUMN IF NOT EXISTS bill_to_customer_no        VARCHAR(25),
    ADD COLUMN IF NOT EXISTS bill_to_name               VARCHAR(50),
    ADD COLUMN IF NOT EXISTS bill_to_name_2             VARCHAR(50),
    ADD COLUMN IF NOT EXISTS bill_to_address            VARCHAR(50),
    ADD COLUMN IF NOT EXISTS bill_to_address_2          VARCHAR(50),
    ADD COLUMN IF NOT EXISTS bill_to_city               VARCHAR(30),
    ADD COLUMN IF NOT EXISTS bill_to_contact            VARCHAR(50),
    ADD COLUMN IF NOT EXISTS bill_to_post_code          VARCHAR(20),
    ADD COLUMN IF NOT EXISTS bill_to_county             VARCHAR(30),
    ADD COLUMN IF NOT EXISTS bill_to_country_region_code VARCHAR(10),

    -- References / IDs
    ADD COLUMN IF NOT EXISTS no_series                  VARCHAR(10),
    ADD COLUMN IF NOT EXISTS forwarding_order_no        VARCHAR(20),
    ADD COLUMN IF NOT EXISTS trade_lane_no              VARCHAR(20),
    ADD COLUMN IF NOT EXISTS transit_time_no            VARCHAR(20),
    ADD COLUMN IF NOT EXISTS import_file_entry_no       BIGINT,
    ADD COLUMN IF NOT EXISTS shortcut_reference_1_code  VARCHAR(80),
    ADD COLUMN IF NOT EXISTS shortcut_reference_2_code  VARCHAR(80),
    ADD COLUMN IF NOT EXISTS shortcut_reference_3_code  VARCHAR(80),
    ADD COLUMN IF NOT EXISTS booking_reference          VARCHAR(80),
    ADD COLUMN IF NOT EXISTS overrule_cust_ref_duplicate BOOLEAN DEFAULT FALSE,

    -- Tariff
    ADD COLUMN IF NOT EXISTS proper_tariff              BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS tariff_no                  VARCHAR(20),
    ADD COLUMN IF NOT EXISTS tariff_id                  VARCHAR(20),
    ADD COLUMN IF NOT EXISTS multiple_tariff            BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS special_tariff_unit_cost   DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS execution_time             DECIMAL(18,4),

    -- Cargo / weight
    ADD COLUMN IF NOT EXISTS total_gross_weight         DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS total_net_weight           DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS temperature                DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS recalc_distance            BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS distance                   DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS duration                   DECIMAL(18,4),

    -- Logistics flags
    ADD COLUMN IF NOT EXISTS shipping_required          BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS road_transport_orders      VARCHAR(190),
    ADD COLUMN IF NOT EXISTS shipping_transport_order   VARCHAR(190),
    ADD COLUMN IF NOT EXISTS linked_trucks              VARCHAR(100),
    ADD COLUMN IF NOT EXISTS linked_drivers_co_drivers  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS linked_trailers_containers VARCHAR(100),
    ADD COLUMN IF NOT EXISTS tradelane_equals_order     BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS source                     VARCHAR(50),
    ADD COLUMN IF NOT EXISTS user_id                    VARCHAR(50),

    -- Transport / vessel / carrier
    ADD COLUMN IF NOT EXISTS carrier_name               VARCHAR(100),
    ADD COLUMN IF NOT EXISTS vessel_name_import         VARCHAR(80),
    ADD COLUMN IF NOT EXISTS vessel_name_export         VARCHAR(80),
    ADD COLUMN IF NOT EXISTS origin_info                VARCHAR(100),
    ADD COLUMN IF NOT EXISTS destination_info           VARCHAR(100),
    ADD COLUMN IF NOT EXISTS seal_no                    VARCHAR(30),
    ADD COLUMN IF NOT EXISTS vessel_eta                 TIMESTAMP,
    ADD COLUMN IF NOT EXISTS vessel_etd                 TIMESTAMP,
    ADD COLUMN IF NOT EXISTS origin_port_name           VARCHAR(50),
    ADD COLUMN IF NOT EXISTS destination_port_name      VARCHAR(50),
    ADD COLUMN IF NOT EXISTS cust_serv_responsible      VARCHAR(20),

    -- Container info (V35 equivalent)
    ADD COLUMN IF NOT EXISTS container_number           VARCHAR(50),
    ADD COLUMN IF NOT EXISTS container_type             VARCHAR(20),
    ADD COLUMN IF NOT EXISTS container_type_iso_code    VARCHAR(10),
    ADD COLUMN IF NOT EXISTS carrier_id                 VARCHAR(20),
    ADD COLUMN IF NOT EXISTS seal_number                VARCHAR(50),
    ADD COLUMN IF NOT EXISTS import_or_export           VARCHAR(20),
    ADD COLUMN IF NOT EXISTS pickup_pincode             VARCHAR(50),
    ADD COLUMN IF NOT EXISTS pickup_reference           VARCHAR(150),
    ADD COLUMN IF NOT EXISTS dropoff_pincode            VARCHAR(50),
    ADD COLUMN IF NOT EXISTS dropoff_reference          VARCHAR(150),
    ADD COLUMN IF NOT EXISTS container_cancelled        BOOLEAN DEFAULT FALSE,

    -- Vessel info (V35 equivalent)
    ADD COLUMN IF NOT EXISTS vessel_name                VARCHAR(100),
    ADD COLUMN IF NOT EXISTS closing_date_time          TIMESTAMP,
    ADD COLUMN IF NOT EXISTS depot_out_from_date_time   TIMESTAMP,
    ADD COLUMN IF NOT EXISTS depot_in_from_date_time    TIMESTAMP,
    ADD COLUMN IF NOT EXISTS vgm_closing_date_time      TIMESTAMP,
    ADD COLUMN IF NOT EXISTS vgm_weight                 DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS origin_country             VARCHAR(10),
    ADD COLUMN IF NOT EXISTS destination_country        VARCHAR(10),

    -- Demurrage / detention
    ADD COLUMN IF NOT EXISTS ata                        TIMESTAMP,
    ADD COLUMN IF NOT EXISTS atd                        TIMESTAMP,
    ADD COLUMN IF NOT EXISTS eta                        TIMESTAMP,
    ADD COLUMN IF NOT EXISTS etd                        TIMESTAMP,
    ADD COLUMN IF NOT EXISTS days_of_demurrage          INTEGER,
    ADD COLUMN IF NOT EXISTS days_of_detention          INTEGER,
    ADD COLUMN IF NOT EXISTS days_of_storage            INTEGER,

    -- Audit
    ADD COLUMN IF NOT EXISTS created_by                 VARCHAR(50),
    ADD COLUMN IF NOT EXISTS last_modified_by           VARCHAR(50),
    ADD COLUMN IF NOT EXISTS last_modification_date_time TIMESTAMP;

-- ──────────────────────────────────────────────────────────────
-- 2. EXPAND imp_order_line
-- ──────────────────────────────────────────────────────────────
ALTER TABLE imp_order_line
    ADD COLUMN IF NOT EXISTS unique_reference           VARCHAR(150),
    ADD COLUMN IF NOT EXISTS original                   BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS processed                  BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS no_planning_required       BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS address_name_2             VARCHAR(100),
    ADD COLUMN IF NOT EXISTS address_search_name        VARCHAR(100),
    ADD COLUMN IF NOT EXISTS address_zone_name          VARCHAR(100),
    ADD COLUMN IF NOT EXISTS address_city_id            INTEGER,
    ADD COLUMN IF NOT EXISTS region_no                  VARCHAR(50),
    ADD COLUMN IF NOT EXISTS master_region_no           VARCHAR(50),
    ADD COLUMN IF NOT EXISTS planning_zone_no           VARCHAR(20),
    ADD COLUMN IF NOT EXISTS planning_zone_description  VARCHAR(80),
    ADD COLUMN IF NOT EXISTS planning_zone              VARCHAR(80),
    ADD COLUMN IF NOT EXISTS transport_order_no         VARCHAR(20),
    ADD COLUMN IF NOT EXISTS planning_sequence          INTEGER,
    ADD COLUMN IF NOT EXISTS grouping_id                INTEGER,
    ADD COLUMN IF NOT EXISTS sorting_key                INTEGER,
    ADD COLUMN IF NOT EXISTS no_series                  VARCHAR(10),
    ADD COLUMN IF NOT EXISTS order_block                VARCHAR(20),

    -- DateTime windows
    ADD COLUMN IF NOT EXISTS initial_datetime_from_2    TIMESTAMP,
    ADD COLUMN IF NOT EXISTS initial_datetime_until_2   TIMESTAMP,
    ADD COLUMN IF NOT EXISTS closing_datetime           TIMESTAMP,
    ADD COLUMN IF NOT EXISTS latest_departure_hour      TIMESTAMP,
    ADD COLUMN IF NOT EXISTS latest_hour_of_departure   TIMESTAMP,
    ADD COLUMN IF NOT EXISTS time_from                  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS time_until                 TIMESTAMP,
    ADD COLUMN IF NOT EXISTS planned_datetime_of_prev_ol TIMESTAMP,
    ADD COLUMN IF NOT EXISTS confirmed_datetime         TIMESTAMP,

    -- Line type / planning
    ADD COLUMN IF NOT EXISTS order_line_type            VARCHAR(50),
    ADD COLUMN IF NOT EXISTS bound_calculated           BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS plug_in                    BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS transport_mode             VARCHAR(20),
    ADD COLUMN IF NOT EXISTS transport_mode_type        VARCHAR(50),
    ADD COLUMN IF NOT EXISTS supplier_no                VARCHAR(20),
    ADD COLUMN IF NOT EXISTS booking_info_validation    VARCHAR(50),
    ADD COLUMN IF NOT EXISTS type_of_time               VARCHAR(50),
    ADD COLUMN IF NOT EXISTS order_line_id              INTEGER,
    ADD COLUMN IF NOT EXISTS order_block_id             INTEGER,
    ADD COLUMN IF NOT EXISTS planning_block_id          INTEGER,

    -- Driver / vehicle
    ADD COLUMN IF NOT EXISTS haulier_no                 VARCHAR(20),
    ADD COLUMN IF NOT EXISTS truck_no                   VARCHAR(20),
    ADD COLUMN IF NOT EXISTS truck_description          VARCHAR(50),
    ADD COLUMN IF NOT EXISTS driver_no                  VARCHAR(20),
    ADD COLUMN IF NOT EXISTS driver_full_name           VARCHAR(60),
    ADD COLUMN IF NOT EXISTS driver_short_name          VARCHAR(30),
    ADD COLUMN IF NOT EXISTS co_driver_no               VARCHAR(20),
    ADD COLUMN IF NOT EXISTS co_driver_full_name        VARCHAR(60),
    ADD COLUMN IF NOT EXISTS co_driver_short_name       VARCHAR(30),
    ADD COLUMN IF NOT EXISTS confirmed_by               VARCHAR(20),
    ADD COLUMN IF NOT EXISTS selected_by                VARCHAR(20),

    -- Equipment
    ADD COLUMN IF NOT EXISTS container_no_2             VARCHAR(20),
    ADD COLUMN IF NOT EXISTS container_number           VARCHAR(20),
    ADD COLUMN IF NOT EXISTS container_number_2         VARCHAR(20),
    ADD COLUMN IF NOT EXISTS trailer_no                 VARCHAR(20),
    ADD COLUMN IF NOT EXISTS trailer_description        VARCHAR(45),
    ADD COLUMN IF NOT EXISTS chassis_no                 VARCHAR(20),
    ADD COLUMN IF NOT EXISTS chassis_description        VARCHAR(50),
    ADD COLUMN IF NOT EXISTS other_equipment_no         VARCHAR(20),
    ADD COLUMN IF NOT EXISTS other_equipment_description VARCHAR(50),
    ADD COLUMN IF NOT EXISTS split_pb                   VARCHAR(50),
    ADD COLUMN IF NOT EXISTS equipment_traction         VARCHAR(50),
    ADD COLUMN IF NOT EXISTS fleet_no_trailer           VARCHAR(20),
    ADD COLUMN IF NOT EXISTS registration_no_trailer    VARCHAR(20),
    ADD COLUMN IF NOT EXISTS fleet_no_chassis           VARCHAR(30),
    ADD COLUMN IF NOT EXISTS registration_no_chassis    VARCHAR(30),

    -- Distances / metrics
    ADD COLUMN IF NOT EXISTS calculated_distance        DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS calculated_distance_to     DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS mileage                    DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS mileage_difference         DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS duration_actual_difference DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS calculated_driving_time    TIME,
    ADD COLUMN IF NOT EXISTS calculated_driving_time_to TIME,
    ADD COLUMN IF NOT EXISTS distance_id                INTEGER,
    ADD COLUMN IF NOT EXISTS distance_id_to             INTEGER,
    ADD COLUMN IF NOT EXISTS latitude                   INTEGER,
    ADD COLUMN IF NOT EXISTS longitude                  INTEGER,

    -- Shipping line
    ADD COLUMN IF NOT EXISTS shipping_line_diary_id     INTEGER,
    ADD COLUMN IF NOT EXISTS sl_booked_for_ol           BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS capacity_diary_id          INTEGER,
    ADD COLUMN IF NOT EXISTS shipping_company_no        VARCHAR(20),
    ADD COLUMN IF NOT EXISTS shipping_company_name      VARCHAR(50),
    ADD COLUMN IF NOT EXISTS shipping_line_no           VARCHAR(20),
    ADD COLUMN IF NOT EXISTS shipping_line_name         VARCHAR(50),
    ADD COLUMN IF NOT EXISTS preferred_shipping_line    VARCHAR(20),
    ADD COLUMN IF NOT EXISTS pre_book                   BOOLEAN DEFAULT FALSE,

    -- Status flags
    ADD COLUMN IF NOT EXISTS is_late                    BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS part_of_order              BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS booking_change_reason      VARCHAR(20),
    ADD COLUMN IF NOT EXISTS sent_to_haulier            BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS needs_revision             BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS manual_calculated          BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS check_booking              BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS booking_required           BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS needs_recalculate          BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS selected                   BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS sent_to_bc                 BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS cancelled_by_driver        BOOLEAN DEFAULT FALSE,

    -- Misc
    ADD COLUMN IF NOT EXISTS groupage_block             INTEGER,
    ADD COLUMN IF NOT EXISTS source                     VARCHAR(50),
    ADD COLUMN IF NOT EXISTS external_order_line_id     VARCHAR(20),
    ADD COLUMN IF NOT EXISTS order_line_ref_1           VARCHAR(80),
    ADD COLUMN IF NOT EXISTS order_line_ref_2           VARCHAR(80),
    ADD COLUMN IF NOT EXISTS import_date_time           TIMESTAMP,
    ADD COLUMN IF NOT EXISTS processed_date_time        TIMESTAMP,
    ADD COLUMN IF NOT EXISTS created_by                 VARCHAR(50),
    ADD COLUMN IF NOT EXISTS creation_date_time         TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_modified_by           VARCHAR(50),
    ADD COLUMN IF NOT EXISTS last_modification_date_time TIMESTAMP;

-- ──────────────────────────────────────────────────────────────
-- 3. EXPAND imp_order_cargo
-- ──────────────────────────────────────────────────────────────
ALTER TABLE imp_order_cargo
    ADD COLUMN IF NOT EXISTS cargo_no                   INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS original                   BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS processed                  BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS action_type                VARCHAR(50),
    ADD COLUMN IF NOT EXISTS external_good_sub_type     VARCHAR(150),
    ADD COLUMN IF NOT EXISTS units_per_parcel           DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS unit_volume                DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS unit_of_measure_description VARCHAR(10),
    ADD COLUMN IF NOT EXISTS qty_per_unit_of_measure    DECIMAL(18,4) DEFAULT 1,
    ADD COLUMN IF NOT EXISTS quantity_base              DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS volume                     DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS force_loading_meters       BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS imdg_type                  VARCHAR(20),
    ADD COLUMN IF NOT EXISTS limited_quantity           BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS tracing_no_1               VARCHAR(150),
    ADD COLUMN IF NOT EXISTS tracing_no_2               VARCHAR(150),
    ADD COLUMN IF NOT EXISTS set_temperature_flag       BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS min_temperature            DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS max_temperature            DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS ol_load_line_no            INTEGER,
    ADD COLUMN IF NOT EXISTS ol_unload_line_no          INTEGER,
    ADD COLUMN IF NOT EXISTS ol_load_ref                VARCHAR(150),
    ADD COLUMN IF NOT EXISTS ol_unload_ref              VARCHAR(150),
    ADD COLUMN IF NOT EXISTS no_processing_required     BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS transformed_record_for_line_no INTEGER,
    ADD COLUMN IF NOT EXISTS quantity_text              VARCHAR(150),
    ADD COLUMN IF NOT EXISTS gross_weight_text          VARCHAR(150),
    ADD COLUMN IF NOT EXISTS net_weight_text            VARCHAR(150),
    ADD COLUMN IF NOT EXISTS external_order_line_id     VARCHAR(20),
    ADD COLUMN IF NOT EXISTS order_line_no              INTEGER,
    ADD COLUMN IF NOT EXISTS import_date_time           TIMESTAMP,
    ADD COLUMN IF NOT EXISTS processed_date_time        TIMESTAMP,
    ADD COLUMN IF NOT EXISTS created_by                 VARCHAR(50),
    ADD COLUMN IF NOT EXISTS creation_date_time         TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_modified_by           VARCHAR(50),
    ADD COLUMN IF NOT EXISTS last_modification_date_time TIMESTAMP;

-- ──────────────────────────────────────────────────────────────
-- 4. EXPAND imp_order_equipment
-- ──────────────────────────────────────────────────────────────
ALTER TABLE imp_order_equipment
    ADD COLUMN IF NOT EXISTS external_order_no          VARCHAR(30),
    ADD COLUMN IF NOT EXISTS material_type_v2           VARCHAR(50),
    ADD COLUMN IF NOT EXISTS customized_boolean         BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS source                     VARCHAR(50),
    ADD COLUMN IF NOT EXISTS remark                     VARCHAR(150),
    ADD COLUMN IF NOT EXISTS tare_weight                DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS vgm_weight                 DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS import_date_time           TIMESTAMP,
    ADD COLUMN IF NOT EXISTS processed_date_time        TIMESTAMP,
    ADD COLUMN IF NOT EXISTS external_order_line_id     VARCHAR(20),
    ADD COLUMN IF NOT EXISTS order_line_no              INTEGER,
    ADD COLUMN IF NOT EXISTS cleaning_instruction       VARCHAR(20),
    ADD COLUMN IF NOT EXISTS created_by                 VARCHAR(50),
    ADD COLUMN IF NOT EXISTS creation_date_time         TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_modified_by           VARCHAR(50),
    ADD COLUMN IF NOT EXISTS last_modification_date_time TIMESTAMP;

-- ──────────────────────────────────────────────────────────────
-- 5. NEW TABLE: imp_order_remark
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS imp_order_remark (
    id                          BIGSERIAL    PRIMARY KEY,
    entry_no                    BIGINT       NOT NULL,
    external_order_no           VARCHAR(30),
    remark_type                 VARCHAR(20),
    line_no                     INTEGER      NOT NULL,
    remarks                     VARCHAR(250),
    external_remark_code        VARCHAR(50),
    import_date_time            TIMESTAMP,
    processed_date_time         TIMESTAMP,
    communication_partner       VARCHAR(20),
    external_order_line_id      VARCHAR(20),
    order_line_no               INTEGER,
    created_by                  VARCHAR(50),
    creation_date_time          TIMESTAMP,
    last_modified_by            VARCHAR(50),
    last_modification_date_time TIMESTAMP,
    FOREIGN KEY (entry_no) REFERENCES imp_order_header(entry_no) ON DELETE CASCADE,
    UNIQUE (entry_no, line_no)
);
CREATE INDEX IF NOT EXISTS idx_imp_remark_entry   ON imp_order_remark(entry_no);
CREATE INDEX IF NOT EXISTS idx_imp_remark_partner ON imp_order_remark(communication_partner);

-- ──────────────────────────────────────────────────────────────
-- 6. NEW TABLE: imp_order_line_cargo
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS imp_order_line_cargo (
    id                          BIGSERIAL    PRIMARY KEY,
    entry_no                    BIGINT       NOT NULL,
    order_line_no               INTEGER      NOT NULL,
    cargo_line_no               INTEGER      NOT NULL,
    external_good_no            VARCHAR(150),
    good_description            VARCHAR(100),
    good_type                   VARCHAR(150),
    good_sub_type               VARCHAR(150),
    quantity                    DECIMAL(18,4),
    unit_of_measure             VARCHAR(20),
    gross_weight                DECIMAL(18,4),
    net_weight                  DECIMAL(18,4),
    volume                      DECIMAL(18,4),
    length                      DECIMAL(18,4),
    width                       DECIMAL(18,4),
    height                      DECIMAL(18,4),
    loading_meters              DECIMAL(18,4),
    diameter                    DECIMAL(18,4),
    tracing_number_1            VARCHAR(150),
    tracing_number_2            VARCHAR(150),
    adr_type                    VARCHAR(20),
    adr_dangerous_for_environment BOOLEAN    DEFAULT FALSE,
    adr_un_no                   VARCHAR(4),
    adr_hazard_class            VARCHAR(30),
    adr_packing_group           VARCHAR(30),
    adr_tunnel_restriction_code VARCHAR(30),
    set_temperature             BOOLEAN      DEFAULT FALSE,
    temperature                 DECIMAL(10,2),
    created_by                  VARCHAR(50),
    creation_date_time          TIMESTAMP,
    last_modified_by            VARCHAR(50),
    last_modification_date_time TIMESTAMP,
    FOREIGN KEY (entry_no) REFERENCES imp_order_header(entry_no) ON DELETE CASCADE,
    UNIQUE (entry_no, order_line_no, cargo_line_no)
);
CREATE INDEX IF NOT EXISTS idx_imp_line_cargo_entry ON imp_order_line_cargo(entry_no);

-- ──────────────────────────────────────────────────────────────
-- 7. NEW TABLE: imp_ord_cust_data
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS imp_ord_cust_data (
    id                          BIGSERIAL    PRIMARY KEY,
    entry_no                    BIGINT       NOT NULL,
    line_no                     INTEGER      NOT NULL,
    field_name                  VARCHAR(250),
    field_value                 VARCHAR(250),
    remark                      VARCHAR(30),
    created_by                  VARCHAR(50),
    creation_date_time          TIMESTAMP,
    last_modified_by            VARCHAR(50),
    last_modification_date_time TIMESTAMP,
    FOREIGN KEY (entry_no) REFERENCES imp_order_header(entry_no) ON DELETE CASCADE,
    UNIQUE (entry_no, line_no)
);
CREATE INDEX IF NOT EXISTS idx_imp_cust_data_entry ON imp_ord_cust_data(entry_no);

-- ──────────────────────────────────────────────────────────────
-- 8. NEW TABLE: imp_order_additional_info
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS imp_order_additional_info (
    id                          BIGSERIAL    PRIMARY KEY,
    entry_no                    BIGINT       NOT NULL,
    type                        SMALLINT     NOT NULL,
    field_name                  VARCHAR(250) NOT NULL,
    field_value                 VARCHAR(250),
    import_date_time            TIMESTAMP,
    processed_date_time         TIMESTAMP,
    communication_partner       VARCHAR(20),
    created_by                  VARCHAR(50),
    creation_date_time          TIMESTAMP,
    last_modified_by            VARCHAR(20),
    last_modification_date_time TIMESTAMP,
    FOREIGN KEY (entry_no) REFERENCES imp_order_header(entry_no) ON DELETE CASCADE,
    UNIQUE (entry_no, field_name)
);
CREATE INDEX IF NOT EXISTS idx_imp_add_info_entry ON imp_order_additional_info(entry_no);

-- ──────────────────────────────────────────────────────────────
-- 9. NEW TABLE: imp_order_custom_field
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS imp_order_custom_field (
    id                          BIGSERIAL    PRIMARY KEY,
    entry_no                    BIGINT       NOT NULL,
    line_no                     INTEGER      NOT NULL,
    field_name                  VARCHAR(200) NOT NULL,
    field_value                 VARCHAR(500),
    external_order_no           VARCHAR(30),
    communication_partner       VARCHAR(20),
    created_by                  VARCHAR(50),
    creation_date_time          TIMESTAMP,
    last_modified_by            VARCHAR(50),
    last_modification_date_time TIMESTAMP,
    FOREIGN KEY (entry_no) REFERENCES imp_order_header(entry_no) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_imp_custom_field_entry   ON imp_order_custom_field(entry_no);
CREATE INDEX IF NOT EXISTS idx_imp_custom_field_name    ON imp_order_custom_field(field_name);
CREATE INDEX IF NOT EXISTS idx_imp_custom_field_partner ON imp_order_custom_field(communication_partner);

-- ──────────────────────────────────────────────────────────────
-- 10. NEW TABLE: imp_order_hdr_ext (extended header data)
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS imp_order_hdr_ext (
    id                          BIGSERIAL    PRIMARY KEY,
    entry_no                    BIGINT       NOT NULL UNIQUE,
    order_acceptance_date_time  TIMESTAMP,
    short_notice                BOOLEAN      DEFAULT FALSE,
    custom_txt_field_1          VARCHAR(50),
    custom_txt_field_2          VARCHAR(50),
    custom_txt_field_3          VARCHAR(50),
    custom_datetime_field_1     TIMESTAMP,
    custom_datetime_field_2     TIMESTAMP,
    custom_boolean_field_1      BOOLEAN      DEFAULT FALSE,
    custom_boolean_field_2      BOOLEAN      DEFAULT FALSE,
    custom_int_field_1          INTEGER,
    custom_int_field_2          INTEGER,
    custom_dec_field_1          DECIMAL(18,4),
    custom_dec_field_2          DECIMAL(18,4),
    custom_date_field_1         DATE,
    custom_date_field_2         DATE,
    custom_dropdown_text1       VARCHAR(20),
    custom_dropdown_text2       VARCHAR(20),
    custom_dropdown_text3       VARCHAR(20),
    forwarder                   VARCHAR(20),
    forwarder_name              VARCHAR(50),
    order_equipments_info       VARCHAR(250),
    cross_docking               BOOLEAN      DEFAULT FALSE,
    customs_status              SMALLINT,
    actual_weight               DECIMAL(18,4),
    weighing_done               BOOLEAN      DEFAULT FALSE,
    created_by                  VARCHAR(50),
    creation_date_time          TIMESTAMP,
    last_modified_by            VARCHAR(50),
    last_modification_date_time TIMESTAMP,
    FOREIGN KEY (entry_no) REFERENCES imp_order_header(entry_no) ON DELETE CASCADE
);

-- ──────────────────────────────────────────────────────────────
-- 11. Indexes on expanded imp_order_header columns
-- ──────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_imp_hdr_import_file   ON imp_order_header(import_file_entry_no);
CREATE INDEX IF NOT EXISTS idx_imp_hdr_customer_name ON imp_order_header(customer_name);
CREATE INDEX IF NOT EXISTS idx_imp_hdr_bill_to       ON imp_order_header(bill_to_customer_no);
