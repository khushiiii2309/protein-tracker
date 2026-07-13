package com.proteintracker.service;

import com.proteintracker.dto.MealDtos.AISuggestionResponse;
import com.proteintracker.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AISuggestionService {

    private final CurrentUserService currentUserService;

    /**
     * Generate AI-powered meal suggestions based on current progress and time of day.
     * This is where we'll integrate with OpenAI/Claude in the future.
     */
    public AISuggestionResponse getSuggestion(double currentProtein, double proteinGoal,
                                               double currentCalories, double caloriesGoal) {
        double proteinRemaining = proteinGoal - currentProtein;
        LocalTime now = LocalTime.now();

        String suggestion;
        String reason;
        List<String> recommendedMeals = new ArrayList<>();

        // Time-based and progress-based suggestions
        if (proteinRemaining <= 10) {
            suggestion = "You're crushing it! Only " + String.format("%.0fg", proteinRemaining) + " protein to go. 🎯";
            reason = "You're almost at your daily goal!";
            recommendedMeals.add("Protein shake (24g protein, 120 kcal)");
            recommendedMeals.add("Greek yogurt (17g protein, 100 kcal)");
        } else if (now.isBefore(LocalTime.of(10, 0))) {
            // Morning suggestions
            suggestion = "Good morning! Start strong with a protein-rich breakfast.";
            reason = "A high-protein breakfast keeps you full and energized.";
            recommendedMeals.add("Greek yogurt & berries (24g protein, 320 kcal)");
            recommendedMeals.add("Scrambled eggs with toast (28g protein, 380 kcal)");
            recommendedMeals.add("Protein pancakes (32g protein, 420 kcal)");
        } else if (now.isBefore(LocalTime.of(14, 0))) {
            // Lunch time suggestions
            suggestion = "Time for a balanced lunch to fuel your afternoon!";
            reason = "You're " + String.format("%.0fg", proteinRemaining) + " from your protein goal.";
            recommendedMeals.add("Grilled chicken bowl (42g protein, 520 kcal)");
            recommendedMeals.add("Turkey wrap (34g protein, 480 kcal)");
            recommendedMeals.add("Salmon & quinoa (38g protein, 540 kcal)");
        } else if (now.isBefore(LocalTime.of(17, 0))) {
            // Afternoon snack suggestions
            suggestion = "An afternoon protein boost will keep you on track.";
            reason = "You're " + String.format("%.0fg", proteinRemaining) + " from today's goal.";
            recommendedMeals.add("Protein shake (24g protein, 210 kcal)");
            recommendedMeals.add("Cottage cheese bowl (28g protein, 240 kcal)");
            recommendedMeals.add("Turkey slices & cheese (26g protein, 180 kcal)");
        } else if (proteinRemaining > 40) {
            // Evening - high protein needed
            suggestion = "Evenings need a little love 🌙. A protein-rich dinner will get you back on track.";
            reason = "You're " + String.format("%.0fg", proteinRemaining) + " short. A hearty meal will help!";
            recommendedMeals.add("Grilled steak with veggies (46g protein, 510 kcal)");
            recommendedMeals.add("Baked salmon & rice (44g protein, 580 kcal)");
            recommendedMeals.add("Chicken stir-fry (38g protein, 490 kcal)");
        } else {
            // Evening - moderate protein needed
            suggestion = "A balanced evening meal will complete your day perfectly!";
            reason = "You need " + String.format("%.0fg", proteinRemaining) + " more protein.";
            recommendedMeals.add("Grilled chicken (32g protein, 350 kcal)");
            recommendedMeals.add("Turkey burger (28g protein, 380 kcal)");
            recommendedMeals.add("Tofu & vegetables (24g protein, 320 kcal)");
        }

        return new AISuggestionResponse(suggestion, reason, recommendedMeals);
    }

    /**
     * Generate motivational message based on streak
     */
    public String getStreakMessage(int streak) {
        if (streak == 0) {
            return "Start your journey today! 🌱";
        } else if (streak == 1) {
            return "Great start! Keep the momentum going! 🔥";
        } else if (streak < 7) {
            return streak + " day streak 🌱 - Logged something every day. Keep it growing!";
        } else if (streak < 30) {
            return streak + " day streak 🔥 - You're building a solid habit!";
        } else if (streak < 100) {
            return streak + " day streak 💪 - Consistency is your superpower!";
        } else {
            return streak + " day streak 🏆 - You're a protein tracking legend!";
        }
    }
}
