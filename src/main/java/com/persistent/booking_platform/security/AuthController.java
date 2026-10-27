package com.persistent.booking_platform.security;

import static com.persistent.booking_platform.dto.Requests.LoginRequest;
import static com.persistent.booking_platform.dto.Requests.RegisterRequest;
import static com.persistent.booking_platform.dto.Responses.AuthResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Create a customer account and sign in")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.registerCustomer(request);
    }

    @Operation(summary = "Create a theatre partner account and sign in")
    @PostMapping("/register-partner")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerPartner(@Valid @RequestBody RegisterRequest request) {
        return authService.registerPartner(request);
    }

    @Operation(summary = "Sign in and receive an access token (send it as Authorization: Bearer ...)")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
