package com.rental.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "model")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Model {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ModelID")
    private Integer modelId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "BrandID", nullable = false)
    private Brand brand;

    @Column(name = "ModelName", nullable = false, length = 100)
    private String modelName;
}
