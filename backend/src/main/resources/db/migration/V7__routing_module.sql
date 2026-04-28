-- ═══════════════════════════════════════════════════════════════════════════
-- TMS Route Optimization Module — schema (PostgreSQL; PostGIS optional)
-- Enable PostGIS on the DB for geometry indexes: CREATE EXTENSION IF NOT EXISTS postgis;
-- ═══════════════════════════════════════════════════════════════════════════

-- Cached geocoding results (Nominatim / manual)
CREATE TABLE routing_location (
    id              BIGSERIAL PRIMARY KEY,
    address_key     VARCHAR(512) NOT NULL,
    address_line    TEXT,
    postcode        VARCHAR(32),
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    city            VARCHAR(128),
    country         VARCHAR(8),
    source          VARCHAR(32)  NOT NULL DEFAULT 'NOMINATIM',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_routing_location_key UNIQUE (address_key)
);

CREATE INDEX idx_routing_location_postcode ON routing_location (postcode);

-- Fleet
CREATE TABLE routing_vehicle (
    vehicle_id          BIGSERIAL PRIMARY KEY,
    code                VARCHAR(64)  NOT NULL UNIQUE,
    vehicle_type        VARCHAR(64),
    capacity_weight_kg  DOUBLE PRECISION NOT NULL DEFAULT 1000,
    capacity_volume_m3  DOUBLE PRECISION NOT NULL DEFAULT 80,
    start_location_id   BIGINT REFERENCES routing_location (id),
    end_location_id     BIGINT REFERENCES routing_location (id),
    shift_start         TIME,
    shift_end           TIME,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Standalone delivery orders + optional link to TMS order card
CREATE TABLE routing_delivery_order (
    id                      BIGSERIAL PRIMARY KEY,
    public_order_id         VARCHAR(64) UNIQUE,
    pickup_address          TEXT         NOT NULL,
    pickup_postcode         VARCHAR(32),
    delivery_address        TEXT         NOT NULL,
    delivery_postcode       VARCHAR(32),
    weight_kg               DOUBLE PRECISION NOT NULL DEFAULT 1,
    volume_m3               DOUBLE PRECISION NOT NULL DEFAULT 0.01,
    time_window_start       TIMESTAMPTZ,
    time_window_end         TIMESTAMPTZ,
    pickup_location_id      BIGINT REFERENCES routing_location (id),
    delivery_location_id    BIGINT REFERENCES routing_location (id),
    tms_order_id            BIGINT REFERENCES tms_order (id),
    tms_order_no            VARCHAR(20),
    status                  VARCHAR(32)  NOT NULL DEFAULT 'NEW',
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_routing_order_tms ON routing_delivery_order (tms_order_no);
CREATE INDEX idx_routing_order_status ON routing_delivery_order (status);

-- Planned routes
CREATE TABLE routing_route (
    route_id        BIGSERIAL PRIMARY KEY,
    vehicle_id      BIGINT NOT NULL REFERENCES routing_vehicle (vehicle_id),
    route_date      DATE   NOT NULL,
    total_distance_m DOUBLE PRECISION,
    total_duration_s DOUBLE PRECISION,
    status          VARCHAR(32) NOT NULL DEFAULT 'PLANNED',
    optimizer_run_id VARCHAR(64),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_routing_route_vehicle_date ON routing_route (vehicle_id, route_date);

CREATE TABLE routing_route_stop (
    stop_id         BIGSERIAL PRIMARY KEY,
    route_id        BIGINT NOT NULL REFERENCES routing_route (route_id) ON DELETE CASCADE,
    order_id        BIGINT REFERENCES routing_delivery_order (id),
    sequence_number INT    NOT NULL,
    stop_type       VARCHAR(16) NOT NULL,
    location_id     BIGINT REFERENCES routing_location (id),
    arrival_time    TIMESTAMPTZ,
    departure_time  TIMESTAMPTZ,
    travel_time_s   DOUBLE PRECISION,
    distance_m      DOUBLE PRECISION
);

CREATE INDEX idx_routing_stop_route ON routing_route_stop (route_id, sequence_number);

-- Driver GPS trail
CREATE TABLE routing_vehicle_tracking (
    id          BIGSERIAL PRIMARY KEY,
    vehicle_id  BIGINT NOT NULL REFERENCES routing_vehicle (vehicle_id),
    latitude    DOUBLE PRECISION NOT NULL,
    longitude   DOUBLE PRECISION NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_routing_track_vehicle_time ON routing_vehicle_tracking (vehicle_id, recorded_at DESC);
