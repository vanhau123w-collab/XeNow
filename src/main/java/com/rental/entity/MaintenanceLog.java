package com.rental.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "maintenance_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    private LocalDate serviceDate;

    @Column(columnDefinition = "TEXT")
    private String serviceType;

    private BigDecimal cost;

    private String technicianName;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDate nextServiceDate;
}
