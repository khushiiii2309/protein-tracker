package com.proteintracker.controller;

import com.proteintracker.dto.AuthDtos.AuthResponse;
import com.proteintracker.dto.AuthDtos.LoginRequest;
import com.proteintracker.dto.AuthDtos.RegisterRequest;
import com.proteintracker.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping
    public ResponseEntity<?> authInfo() {
        return ResponseEntity.ok(java.util.Map.of(
            "message", "Authentication endpoints",
            "endpoints", java.util.Map.of(
                "register", "POST /api/auth/register - Register a new user",
                "login", "POST /api/auth/login - Login with email and password"
            ),
            "example_register", java.util.Map.of(
                "email", "user@example.com",
                "password", "password123",
                "name", "John Doe"
            ),
            "example_login", java.util.Map.of(
                "email", "user@example.com",
                "password", "password123"
            )
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
