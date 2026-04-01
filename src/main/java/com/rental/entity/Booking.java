package com.rental.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Booking")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    public enum Status {
        Pending, Confirmed, Ongoing, Completed, Cancelled
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BookingID")
    private Integer bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CustomerID", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VehicleID", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ApprovedBy")
    private User approvedBy;

    @Column(name = "PickupAddress", length = 500)
    private String pickupAddress;

    @Column(name = "ReturnAddress", length = 500)
    private String returnAddress;

    @Column(name = "StartDate", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "EndDate", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "TotalPrice", precision = 15, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "DepositAmount", precision = 15, scale = 2)
    private BigDecimal depositAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status")
    @Builder.Default
    private Status status = Status.Pending;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CouponID")
    private Coupon coupon;

    @Column(name = "ReturnMileage")
    private Integer returnMileage;

    @Column(name = "ReturnNote", length = 500)
    private String returnNote;

    @Column(name = "DeletedAt")
    private LocalDateTime deletedAt;

    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
