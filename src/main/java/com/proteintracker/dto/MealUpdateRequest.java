package com.proteintracker.dto;

import java.util.List;

public record MealUpdateRequest(
        String mealType,
        String description,
        Double totalProteinG,
        Double totalCaloriesKcal,
        Double totalCarbsG,
        Double totalFatG,
        List<FoodItemUpdateRequest> foodItems
) {
    public record FoodItemUpdateRequest(
            String foodName,
            Double estimatedQuantity,
            String unit,
            Double proteinG,
            Double caloriesKcal,
            Double carbsG,
            Double fatG,
            Boolean userConfirmed
    ) {}
}
