package com.tms.edi.entity.cfg;

import jakarta.persistence.*;
import lombok.*;

/**
 * EDI Communication Partner configuration.
 * Maps to AL: "Go4IMP Import Comm. Partner"
 * Controls which fields are internal (no mapping needed) vs. external (require mapping lookup).
 */
@Entity
@Table(name = "cfg_communication_partner")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunicationPartner {

    @Id
    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "error_recipients", length = 250)
    private String errorRecipients;

    @Column(name = "default_customer_no", length = 20)
    private String defaultCustomerNo;

    // Internal flags — when true, no mapping lookup is needed
    @Column(name = "customer_no_is_internal")
    @Builder.Default private Boolean customerNoIsInternal = false;

    @Column(name = "office_is_internal")
    @Builder.Default private Boolean officeIsInternal = false;

    @Column(name = "cust_serv_resp_is_internal")
    @Builder.Default private Boolean custServRespIsInternal = false;

    @Column(name = "sales_responsible_is_internal")
    @Builder.Default private Boolean salesResponsibleIsInternal = false;

    @Column(name = "action_code_is_internal")
    @Builder.Default private Boolean actionCodeIsInternal = false;

    @Column(name = "address_no_is_internal")
    @Builder.Default private Boolean addressNoIsInternal = false;

    @Column(name = "good_no_is_internal")
    @Builder.Default private Boolean goodNoIsInternal = false;

    @Column(name = "uom_code_is_internal")
    @Builder.Default private Boolean uomCodeIsInternal = false;

    @Column(name = "good_type_is_internal")
    @Builder.Default private Boolean goodTypeIsInternal = false;

    @Column(name = "good_sub_type_is_internal")
    @Builder.Default private Boolean goodSubTypeIsInternal = false;

    @Column(name = "reference_code_is_internal")
    @Builder.Default private Boolean referenceCodeIsInternal = false;

    @Column(name = "carrier_is_internal")
    @Builder.Default private Boolean carrierIsInternal = false;

    /** When true, country codes on import header/lines are treated as TMS codes (no mapping lookup). */
    @Column(name = "country_code_is_internal")
    @Builder.Default private Boolean countryCodeIsInternal = false;

    @Column(name = "revenue_is_internal")
    @Builder.Default private Boolean revenueIsInternal = false;

    @Column(name = "revenue_type_is_internal")
    @Builder.Default private Boolean revenueTypeIsInternal = false;

    @Column(name = "currency_code_is_internal")
    @Builder.Default private Boolean currencyCodeIsInternal = false;

    @Column(name = "equipment_type_is_internal")
    @Builder.Default private Boolean equipmentTypeIsInternal = false;

    @Column(name = "equipment_sub_type_is_internal")
    @Builder.Default private Boolean equipmentSubTypeIsInternal = false;

    /** When true, transport type values from the partner are already TMS-native codes (no mapping lookup). */
    @Column(name = "transport_type_is_internal")
    @Builder.Default private Boolean transportTypeIsInternal = false;

    /** When true, trip type values from the partner are already TMS-native codes (no mapping lookup). */
    @Column(name = "trip_type_is_internal")
    @Builder.Default private Boolean tripTypeIsInternal = false;

    @Column(name = "auto_insert_address")
    @Builder.Default private Boolean autoInsertAddress = false;

    @Column(name = "auto_insert_city")
    @Builder.Default private Boolean autoInsertCity = false;

    @Column(name = "revalidate_address_mapping")
    @Builder.Default private Boolean revalidateAddressMapping = false;

    @Column(name = "is_web_portal")
    @Builder.Default private Boolean isWebPortal = false;

    @Column(name = "adr_mapping")
    @Builder.Default private Boolean adrMapping = false;

    @Column(name = "active")
    @Builder.Default private Boolean active = true;
}
