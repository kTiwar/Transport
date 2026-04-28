package com.tms.edi.entity.tms;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * TMS Order stop/action line (pickup or delivery stop).
 * Maps to AL: "Go4TMS Order Line"
 */
@Entity
@Table(name = "tms_order_line")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TmsOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tms_order_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private TmsOrder tmsOrder;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "sorting_key")
    private Integer sortingKey;

    /** Mapped from Import Action Code via ImportMapping */
    @Column(name = "action_code", length = 20)
    private String actionCode;

    /** Mapped from External Address No. via ImportMapping */
    @Column(name = "address_no", length = 20)
    private String addressNo;

    @Column(name = "initial_datetime_from")
    private LocalDateTime initialDatetimeFrom;

    @Column(name = "initial_datetime_until")
    private LocalDateTime initialDatetimeUntil;

    @Column(name = "requested_datetime_from")
    private LocalDateTime requestedDatetimeFrom;

    @Column(name = "requested_datetime_until")
    private LocalDateTime requestedDatetimeUntil;

    @Column(name = "booked_datetime_from")
    private LocalDateTime bookedDatetimeFrom;

    @Column(name = "booked_datetime_until")
    private LocalDateTime bookedDatetimeUntil;

    @Column(name = "closing_datetime")
    private LocalDateTime closingDatetime;

    @Column(name = "order_line_ref1", length = 50)
    private String orderLineRef1;

    @Column(name = "order_line_ref2", length = 50)
    private String orderLineRef2;

    @Column(name = "container_no", length = 20)
    private String containerNo;

    @Column(name = "loaded")
    private Boolean loaded;

    @Column(name = "source", length = 20)
    @Builder.Default
    private String source = "IMP_ORD";

    @Column(name = "planning_block_id")
    private Long planningBlockId;

    @Column(name = "order_line_id")
    private Long orderLineId;
}
