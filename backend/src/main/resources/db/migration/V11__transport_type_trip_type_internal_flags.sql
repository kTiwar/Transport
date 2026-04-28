-- V11: Add transport_type_is_internal and trip_type_is_internal flags
-- to cfg_communication_partner, matching the pattern used for all other
-- mapping bypass flags (customer_no, office, action_code, carrier, etc.).

ALTER TABLE cfg_communication_partner
    ADD COLUMN IF NOT EXISTS transport_type_is_internal BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS trip_type_is_internal      BOOLEAN NOT NULL DEFAULT FALSE;