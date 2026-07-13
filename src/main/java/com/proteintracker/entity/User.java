package com.proteintracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String name;

    @Column(name = "daily_protein_goal_grams")
    private Double dailyProteinGoalGrams;

    @Column(name = "daily_calories_goal_kcal")
    private Double dailyCaloriesGoalKcal;

    @Column(name = "daily_carbs_goal_grams")
    private Double dailyCarbsGoalGrams;

    @Column(name = "daily_fat_goal_grams")
    private Double dailyFatGoalGrams;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.dailyProteinGoalGrams == null) {
            this.dailyProteinGoalGrams = 140.0; // sensible default matching design
        }
        if (this.dailyCaloriesGoalKcal == null) {
            this.dailyCaloriesGoalKcal = 2000.0;
        }
        if (this.dailyCarbsGoalGrams == null) {
            this.dailyCarbsGoalGrams = 200.0;
        }
        if (this.dailyFatGoalGrams == null) {
            this.dailyFatGoalGrams = 65.0;
        }
    }
}
