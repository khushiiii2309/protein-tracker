package com.proteintracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "food_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_id", nullable = false)
    private Meal meal;

    @Column(nullable = false)
    private String foodName;

    /** Quantity in the given unit, e.g. 2 (rotis) or 250 (grams) */
    private Double estimatedQuantity;

    private Double minQuantity;
    private Double maxQuantity;

    /** e.g. "piece", "bowl", "g", "ml" */
    private String unit;

    private Double proteinG;
    private Double caloriesKcal;
    private Double carbsG;
    private Double fatG;

    @Builder.Default
    private Boolean userConfirmed = false;
}
