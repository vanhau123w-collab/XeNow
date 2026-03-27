package com.rental.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Review")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReviewID")
    private Integer reviewId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HistoryID", unique = true)
    private RentalHistory rentalHistory;

    @Column(name = "Rating")
    private Integer rating; // 1-5

    @Column(name = "Comment", columnDefinition = "TEXT")
    private String comment;
}