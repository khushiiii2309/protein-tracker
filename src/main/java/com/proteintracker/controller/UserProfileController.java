package com.proteintracker.controller;

import com.proteintracker.dto.UserDtos.UpdateGoalsRequest;
import com.proteintracker.dto.UserDtos.UserProfileResponse;
import com.proteintracker.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile() {
        return ResponseEntity.ok(userProfileService.getProfile());
    }

    @PutMapping("/goals")
    public ResponseEntity<UserProfileResponse> updateGoals(@RequestBody UpdateGoalsRequest request) {
        return ResponseEntity.ok(userProfileService.updateGoals(request));
    }
}
