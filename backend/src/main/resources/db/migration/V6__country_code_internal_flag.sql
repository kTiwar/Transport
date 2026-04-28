-- Allow header/line country codes to be used as TMS codes without cfg_import_mapping rows
-- (e.g. ISO 3166-1 alpha-2: BE, AU) when the communication partner is configured for it.

ALTER TABLE cfg_communication_partner
    ADD COLUMN IF NOT EXISTS country_code_is_internal BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE cfg_communication_partner
SET country_code_is_internal = TRUE
WHERE code = 'DEFAULT';
