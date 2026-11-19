package project.illit.smedia.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.illit.smedia.auth.dto.AuthResponse;
import project.illit.smedia.auth.dto.LoginRequest;
import project.illit.smedia.auth.dto.LogoutRequest;
import project.illit.smedia.auth.dto.RefreshTokenRequest;
import project.illit.smedia.auth.dto.RegisterRequest;
import project.illit.smedia.auth.service.AuthService;
import project.illit.smedia.common.dto.ApiResponse;
import project.illit.smedia.common.ratelimit.RateLimit;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @RateLimit(limit = 5, windowSeconds = 60, keyType = RateLimit.KeyType.IP)
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration successful", response));
    }

    @RateLimit(limit = 10, windowSeconds = 60, keyType = RateLimit.KeyType.IP)
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @RateLimit(limit = 20, windowSeconds = 60, keyType = RateLimit.KeyType.IP)
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed", response));
    }

    @RateLimit(limit = 20, windowSeconds = 60)
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestBody(required = false) LogoutRequest request
    ) {
        authService.logout(authorizationHeader, request);
        return ResponseEntity.ok(ApiResponse.ok("Logout successful"));
    }
}
