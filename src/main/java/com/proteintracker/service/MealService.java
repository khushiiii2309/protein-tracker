package com.proteintracker.service;

import com.proteintracker.dto.MealDtos.DailySummaryResponse;
import com.proteintracker.dto.MealDtos.DayEntry;
import com.proteintracker.dto.MealDtos.FoodItemResponse;
import com.proteintracker.dto.MealDtos.JournalResponse;
import com.proteintracker.dto.MealDtos.MealResponse;
import com.proteintracker.entity.Meal;
import com.proteintracker.entity.MealStatus;
import com.proteintracker.entity.User;
import com.proteintracker.repository.MealRepository;
import com.proteintracker.dto.MealUpdateRequest;
import com.proteintracker.entity.FoodItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MealService {

    private final MealRepository mealRepository;
    private final FileStorageService fileStorageService;
    private final CurrentUserService currentUserService;
    private final AISuggestionService aiSuggestionService;
    private final AIMealAnalysisService aiMealAnalysisService;

    public MealResponse uploadMealPhoto(MultipartFile photo) {
        User user = currentUserService.getCurrentUser();
        String storedFilename = fileStorageService.store(photo);

        Meal meal = Meal.builder()
                .user(user)
                .photoUrl(storedFilename)
                .capturedAt(Instant.now())
                .status(MealStatus.AI_ESTIMATED)
                .build();

        AIMealAnalysisService.EstimatedMeal estimate = aiMealAnalysisService.analyzeMealPhoto(
                photo, storedFilename);
        
        meal.setDescription(estimate.description);
        meal.setMealType(estimate.mealType);
        meal.setTotalProteinG(estimate.totalProteinG);
        meal.setTotalCaloriesKcal(estimate.totalCaloriesKcal);
        meal.setTotalCarbsG(estimate.totalCarbsG);
        meal.setTotalFatG(estimate.totalFatG);
        meal.setConfidenceScore(estimate.confidenceScore);

        List<FoodItem> items = new ArrayList<>();
        for (AIMealAnalysisService.EstimatedFoodItem estItem : estimate.foodItems) {
            FoodItem foodItem = FoodItem.builder()
                    .meal(meal)
                    .foodName(estItem.foodName)
                    .estimatedQuantity(estItem.estimatedQuantity)
                    .unit(estItem.unit)
                    .proteinG(estItem.proteinG)
                    .caloriesKcal(estItem.caloriesKcal)
                    .carbsG(estItem.carbsG)
                    .fatG(estItem.fatG)
                    .userConfirmed(false)
                    .build();
            items.add(foodItem);
        }
        meal.setFoodItems(items);

        meal = mealRepository.save(meal);
        return toResponse(meal);
    }

    public DailySummaryResponse getTodaySummary() {
        User user = currentUserService.getCurrentUser();

        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        Instant startOfDay = today.atStartOfDay(zone).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant();

        List<Meal> meals = mealRepository
                .findByUserAndCapturedAtBetweenOrderByCapturedAtDesc(user, startOfDay, endOfDay);

        double totalProtein = sum(meals, Meal::getTotalProteinG);
        double totalCalories = sum(meals, Meal::getTotalCaloriesKcal);
        double totalCarbs = sum(meals, Meal::getTotalCarbsG);
        double totalFat = sum(meals, Meal::getTotalFatG);

        double proteinGoal = user.getDailyProteinGoalGrams() == null ? 140.0 : user.getDailyProteinGoalGrams();
        double proteinRemaining = Math.max(0, proteinGoal - totalProtein);
        double caloriesGoal = user.getDailyCaloriesGoalKcal() == null ? 2000.0 : user.getDailyCaloriesGoalKcal();
        double carbsGoal = user.getDailyCarbsGoalGrams() == null ? 200.0 : user.getDailyCarbsGoalGrams();
        double fatGoal = user.getDailyFatGoalGrams() == null ? 65.0 : user.getDailyFatGoalGrams();

        int streak = calculateStreak(user, zone);
        String aiSuggestion = generateAISuggestion(totalProtein, proteinGoal, totalCalories, caloriesGoal);

        List<MealResponse> mealResponses = meals.stream().map(this::toResponse).toList();

        return new DailySummaryResponse(
                totalProtein, totalCalories, totalCarbs, totalFat,
                proteinGoal, proteinRemaining, caloriesGoal, carbsGoal, fatGoal,
                streak, aiSuggestion, mealResponses);
    }

    public int calculateStreak(User user, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        int streak = 0;
        LocalDate checkDate = today;

        while (true) {
            Instant startOfDay = checkDate.atStartOfDay(zone).toInstant();
            Instant endOfDay = checkDate.plusDays(1).atStartOfDay(zone).toInstant();

            long mealCount = mealRepository.countByUserAndCapturedAtBetween(user, startOfDay, endOfDay);
            if (mealCount == 0) {
                break;
            }
            streak++;
            checkDate = checkDate.minusDays(1);
            if (streak > 365) break; // safety limit
        }
        return streak;
    }

    private String generateAISuggestion(double currentProtein, double proteinGoal, double currentCalories, double caloriesGoal) {
        return aiSuggestionService.getSuggestion(currentProtein, proteinGoal, currentCalories, caloriesGoal)
                .suggestion();
    }

    private double sum(List<Meal> meals, java.util.function.Function<Meal, Double> extractor) {
        return meals.stream()
                .map(extractor)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    private MealResponse toResponse(Meal meal) {
        List<FoodItemResponse> items = meal.getFoodItems().stream()
                .map(fi -> new FoodItemResponse(
                        fi.getId(), fi.getFoodName(), fi.getEstimatedQuantity(), fi.getUnit(),
                        fi.getProteinG(), fi.getCaloriesKcal(), fi.getCarbsG(), fi.getFatG(),
                        fi.getUserConfirmed()))
                .toList();

        return new MealResponse(
                meal.getId(), meal.getPhotoUrl(), meal.getCapturedAt(), meal.getStatus(),
                meal.getMealType(), meal.getDescription(),
                meal.getTotalProteinG(), meal.getTotalCaloriesKcal(),
                meal.getTotalCarbsG(), meal.getTotalFatG(),
                meal.getConfidenceScore(), items);
    }

    public JournalResponse getJournal(int daysBack) {
        User user = currentUserService.getCurrentUser();
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        
        int streak = calculateStreak(user, zone);
        String streakMessage = aiSuggestionService.getStreakMessage(streak);
        
        List<DayEntry> dayEntries = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d");
        
        for (int i = 0; i < daysBack; i++) {
            LocalDate date = today.minusDays(i);
            Instant startOfDay = date.atStartOfDay(zone).toInstant();
            Instant endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant();
            
            List<Meal> meals = mealRepository.findByUserAndCapturedAtBetweenOrderByCapturedAtDesc(
                    user, startOfDay, endOfDay);
            
            double totalProtein = sum(meals, Meal::getTotalProteinG);
            double proteinGoal = user.getDailyProteinGoalGrams() == null ? 140.0 : user.getDailyProteinGoalGrams();
            boolean goalHit = totalProtein >= proteinGoal;
            
            String dateLabel;
            if (i == 0) {
                dateLabel = "Today, " + date.format(formatter);
            } else if (i == 1) {
                dateLabel = "Yesterday, " + date.format(formatter);
            } else {
                dateLabel = date.format(DateTimeFormatter.ofPattern("EEEE, MMM d"));
            }
            
            List<MealResponse> mealResponses = meals.stream().map(this::toResponse).toList();
            
            if (!meals.isEmpty() || i == 0) { // Always include today, even if no meals
                dayEntries.add(new DayEntry(dateLabel, totalProtein, goalHit, mealResponses));
            }
        }
        
        return new JournalResponse(streak, streakMessage, dayEntries);
    }

    public MealResponse confirmMeal(Long id, MealUpdateRequest request) {
        User user = currentUserService.getCurrentUser();
        Meal meal = mealRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meal not found with ID " + id));

        if (!meal.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: You do not own this meal");
        }

        meal.setMealType(request.mealType());
        meal.setDescription(request.description());
        meal.setTotalProteinG(request.totalProteinG());
        meal.setTotalCaloriesKcal(request.totalCaloriesKcal());
        meal.setTotalCarbsG(request.totalCarbsG());
        meal.setTotalFatG(request.totalFatG());
        meal.setStatus(MealStatus.CONFIRMED);

        // Update food items
        meal.getFoodItems().clear();
        if (request.foodItems() != null) {
            for (MealUpdateRequest.FoodItemUpdateRequest itemReq : request.foodItems()) {
                FoodItem foodItem = FoodItem.builder()
                        .meal(meal)
                        .foodName(itemReq.foodName())
                        .estimatedQuantity(itemReq.estimatedQuantity())
                        .unit(itemReq.unit())
                        .proteinG(itemReq.proteinG())
                        .caloriesKcal(itemReq.caloriesKcal())
                        .carbsG(itemReq.carbsG())
                        .fatG(itemReq.fatG())
                        .userConfirmed(itemReq.userConfirmed() != null ? itemReq.userConfirmed() : true)
                        .build();
                meal.getFoodItems().add(foodItem);
            }
        }

        meal = mealRepository.save(meal);
        return toResponse(meal);
    }

    public void deleteMeal(Long id) {
        User user = currentUserService.getCurrentUser();
        Meal meal = mealRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meal not found with ID " + id));

        if (!meal.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: You do not own this meal");
        }

        mealRepository.delete(meal);
    }
}
