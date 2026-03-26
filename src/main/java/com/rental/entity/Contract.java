package com.rental.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "contracts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer contractId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    private LocalDate signDate;
    private BigDecimal penaltyFee;
    private BigDecimal damageFee;
    private BigDecimal cleaningFee;
    private BigDecimal totalFinalPrice;

    @Column(columnDefinition = "TEXT")
    private String conditionBefore;

    @Column(columnDefinition = "TEXT")
    private String conditionAfter;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
