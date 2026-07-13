package com.proteintracker.dto;

public class UserDtos {

    public record UserProfileResponse(
            Long id,
            String email,
            String name,
            Double dailyProteinGoalGrams,
            Double dailyCaloriesGoalKcal,
            Double dailyCarbsGoalGrams,
            Double dailyFatGoalGrams,
            Integer currentStreak
    ) {}

    public record UpdateGoalsRequest(
            Double dailyProteinGoalGrams,
            Double dailyCaloriesGoalKcal,
            Double dailyCarbsGoalGrams,
            Double dailyFatGoalGrams
    ) {}
}
