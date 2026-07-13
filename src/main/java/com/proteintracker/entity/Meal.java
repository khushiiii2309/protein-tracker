package com.proteintracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String photoUrl;

    @Column(nullable = false)
    private Instant capturedAt;

    private String mealType; // "Breakfast", "Lunch", "Dinner", "Snack"

    private String description; // e.g., "Greek yogurt & berries"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MealStatus status = MealStatus.PENDING_AI;

    private Double totalProteinG;
    private Double totalCaloriesKcal;
    private Double totalCarbsG;
    private Double totalFatG;

    /** 0.0 - 1.0 confidence score from the AI estimation step */
    private Double confidenceScore;

    @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FoodItem> foodItems = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.capturedAt == null) {
            this.capturedAt = Instant.now();
        }
    }
}
