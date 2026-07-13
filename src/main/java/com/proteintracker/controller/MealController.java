package com.proteintracker.controller;

import com.proteintracker.dto.MealDtos.AISuggestionResponse;
import com.proteintracker.dto.MealDtos.DailySummaryResponse;
import com.proteintracker.dto.MealDtos.JournalResponse;
import com.proteintracker.dto.MealDtos.MealResponse;
import com.proteintracker.service.AISuggestionService;
import com.proteintracker.service.MealService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.proteintracker.dto.MealUpdateRequest;

@RestController
@RequestMapping("/api/meals")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;
    private final AISuggestionService aiSuggestionService;

    @PostMapping(value = "/photo", consumes = "multipart/form-data")
    public ResponseEntity<MealResponse> uploadMealPhoto(@RequestParam("photo") MultipartFile photo) {
        return ResponseEntity.ok(mealService.uploadMealPhoto(photo));
    }

    @GetMapping("/today")
    public ResponseEntity<DailySummaryResponse> getTodaySummary() {
        return ResponseEntity.ok(mealService.getTodaySummary());
    }

    @GetMapping("/suggestions")
    public ResponseEntity<AISuggestionResponse> getAISuggestions() {
        DailySummaryResponse summary = mealService.getTodaySummary();
        AISuggestionResponse suggestions = aiSuggestionService.getSuggestion(
                summary.totalProteinG(),
                summary.proteinGoalG(),
                summary.totalCaloriesKcal(),
                summary.caloriesGoalKcal()
        );
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/journal")
    public ResponseEntity<JournalResponse> getJournal(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(mealService.getJournal(days));
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<MealResponse> confirmMeal(
            @PathVariable Long id,
            @RequestBody MealUpdateRequest request) {
        return ResponseEntity.ok(mealService.confirmMeal(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMeal(@PathVariable Long id) {
        mealService.deleteMeal(id);
        return ResponseEntity.noContent().build();
    }
}
