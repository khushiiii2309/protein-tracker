package com.proteintracker.dto;

import com.proteintracker.entity.MealStatus;

import java.time.Instant;
import java.util.List;

public class MealDtos {

    public record FoodItemResponse(
            Long id,
            String foodName,
            Double estimatedQuantity,
            String unit,
            Double proteinG,
            Double caloriesKcal,
            Double carbsG,
            Double fatG,
            Boolean userConfirmed,
            Double minQuantity,
            Double maxQuantity
    ) {}

    public record MealResponse(
            Long id,
            String photoUrl,
            Instant capturedAt,
            MealStatus status,
            String mealType,
            String description,
            Double totalProteinG,
            Double totalCaloriesKcal,
            Double totalCarbsG,
            Double totalFatG,
            Double confidenceScore,
            Double detectionConfidence,
            Double portionConfidence,
            Double nutritionConfidence,
            List<FoodItemResponse> foodItems
    ) {}

    public record DailySummaryResponse(
            Double totalProteinG,
            Double totalCaloriesKcal,
            Double totalCarbsG,
            Double totalFatG,
            Double proteinGoalG,
            Double proteinRemainingG,
            Double caloriesGoalKcal,
            Double carbsGoalG,
            Double fatGoalG,
            Integer currentStreak,
            String aiSuggestion,
            List<MealResponse> meals
    ) {}

    public record AISuggestionResponse(
            String suggestion,
            String reason,
            List<String> recommendedMeals
    ) {}

    public record DayEntry(
            String date, // e.g., "Today, Jul 6" or "Yesterday, Jul 5"
            Double totalProteinG,
            Boolean goalHit,
            List<MealResponse> meals
    ) {}

    public record JournalResponse(
            Integer currentStreak,
            String streakMessage,
            List<DayEntry> days
    ) {}
}
