package com.proteintracker.service;

import com.proteintracker.dto.UserDtos.UpdateGoalsRequest;
import com.proteintracker.dto.UserDtos.UserProfileResponse;
import com.proteintracker.entity.User;
import com.proteintracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final MealService mealService;

    public UserProfileResponse getProfile() {
        User user = currentUserService.getCurrentUser();
        ZoneId zone = ZoneId.systemDefault();
        int streak = mealService.calculateStreak(user, zone);

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getDailyProteinGoalGrams(),
                user.getDailyCaloriesGoalKcal(),
                user.getDailyCarbsGoalGrams(),
                user.getDailyFatGoalGrams(),
                streak
        );
    }

    @Transactional
    public UserProfileResponse updateGoals(UpdateGoalsRequest request) {
        User user = currentUserService.getCurrentUser();

        if (request.dailyProteinGoalGrams() != null) {
            user.setDailyProteinGoalGrams(request.dailyProteinGoalGrams());
        }
        if (request.dailyCaloriesGoalKcal() != null) {
            user.setDailyCaloriesGoalKcal(request.dailyCaloriesGoalKcal());
        }
        if (request.dailyCarbsGoalGrams() != null) {
            user.setDailyCarbsGoalGrams(request.dailyCarbsGoalGrams());
        }
        if (request.dailyFatGoalGrams() != null) {
            user.setDailyFatGoalGrams(request.dailyFatGoalGrams());
        }

        userRepository.save(user);
        return getProfile();
    }
}
