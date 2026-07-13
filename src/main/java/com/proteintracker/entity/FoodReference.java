package com.proteintracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "food_references")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String defaultUnit;

    private Double proteinPer100g;
    private Double caloriesPer100g;
    private Double carbsPer100g;
    private Double fatPer100g;

    /** Typical weight in grams for one "default unit", e.g. 1 roti ~= 30g */
    private Double typicalPortionGrams;
}
