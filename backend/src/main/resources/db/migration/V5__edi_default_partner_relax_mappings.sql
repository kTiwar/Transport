-- Relax DEFAULT communication partner so EDI → Import → TMS auto-chain works without cfg_import_mapping rows.

UPDATE cfg_communication_partner
SET customer_no_is_internal        = true,
    office_is_internal             = true,
    cust_serv_resp_is_internal     = true,
    sales_responsible_is_internal  = true,
    action_code_is_internal        = true,
    address_no_is_internal         = true,
    good_no_is_internal            = true,
    uom_code_is_internal           = true,
    good_type_is_internal          = true,
    good_sub_type_is_internal      = true,
    reference_code_is_internal     = true,
    carrier_is_internal            = true,
    revenue_is_internal            = true,
    revenue_type_is_internal       = true,
    currency_code_is_internal      = true,
    equipment_type_is_internal     = true,
    equipment_sub_type_is_internal = true
WHERE code = 'DEFAULT';
