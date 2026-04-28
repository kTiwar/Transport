package com.tms.edi.controller;

import com.tms.edi.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "JWT token management")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider      tokenProvider;

    @Operation(summary = "Login and obtain JWT tokens")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));

        String accessToken  = tokenProvider.generateAccessToken(auth);
        String refreshToken = tokenProvider.generateRefreshToken(req.getUsername());

        log.info("User {} authenticated successfully", req.getUsername());
        return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken, 900));
    }

    @Operation(summary = "Refresh access token using refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshRequest req) {
        if (!tokenProvider.validateToken(req.getRefreshToken())) {
            return ResponseEntity.status(401).build();
        }
        String username = tokenProvider.getUsernameFromToken(req.getRefreshToken());
        Authentication auth = new UsernamePasswordAuthenticationToken(username, null, java.util.List.of());
        String newAccess = tokenProvider.generateAccessToken(auth);
        return ResponseEntity.ok(new TokenResponse(newAccess, req.getRefreshToken(), 900));
    }

    // DTOs
    @Data public static class LoginRequest {
        @NotBlank private String username;
        @NotBlank private String password;
    }

    @Data public static class RefreshRequest {
        @NotBlank private String refreshToken;
    }

    @Data @AllArgsConstructor
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private int expiresIn;
    }
}
