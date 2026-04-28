package com.tms.edi.entity.imp;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Staging table for EDI revenue/cost lines.
 * Maps to AL: "Go4IMP Transport Cost"
 */
@Entity
@Table(name = "imp_transport_cost")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportTransportCost {

    @EmbeddedId
    private ImportTransportCostId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_no", insertable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ImportOrderHeader header;

    @Column(name = "communication_partner", length = 20)
    private String communicationPartner;

    @Column(name = "revenue_code", length = 20)
    private String revenueCode;

    @Column(name = "revenue_type", length = 20)
    private String revenueType;

    @Column(name = "unit_of_measure_budget", length = 20)
    private String unitOfMeasureBudget;

    @Column(name = "currency_actual", length = 10)
    private String currencyActual;

    @Column(name = "amount_actual", precision = 18, scale = 4)
    private BigDecimal amountActual;

    @Column(name = "amount_budget", precision = 18, scale = 4)
    private BigDecimal amountBudget;

    @Column(name = "quantity_budget", precision = 18, scale = 4)
    private BigDecimal quantityBudget;
}
