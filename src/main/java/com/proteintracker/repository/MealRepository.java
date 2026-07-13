package com.proteintracker.repository;

import com.proteintracker.entity.Meal;
import com.proteintracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface MealRepository extends JpaRepository<Meal, Long> {
    List<Meal> findByUserAndCapturedAtBetweenOrderByCapturedAtDesc(
            User user, Instant start, Instant end);

    long countByUserAndCapturedAtBetween(User user, Instant start, Instant end);

    List<Meal> findByUserOrderByCapturedAtDesc(User user);
}
