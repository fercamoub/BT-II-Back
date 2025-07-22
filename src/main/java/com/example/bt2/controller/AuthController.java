package com.example.bt2.controller;

import com.example.bt2.entity.UserToken;
import com.example.bt2.service.SpotifyAuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*") // Configure properly for production
public class AuthController {

    @Autowired
    private SpotifyAuthService authService;

    @PostMapping("/spotify")
    public ResponseEntity<Map<String, String>> initiateSpotifyAuth() {
        String authUrl = authService.buildAuthorizationUri();

        Map<String, String> response = new HashMap<>();
        response.put("authUrl", authUrl);
        response.put("message", "Redirect user to this URL for Spotify authentication");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/spotify/callback")
    public ResponseEntity<Map<String, Object>> handleSpotifyCallback(@RequestParam String code) {
        try {
            UserToken userToken = authService.exchangeCodeForToken(code);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userId", userToken.getUserId());
            response.put("message", "Authentication successful");
            response.put("expiresAt", userToken.getExpiresAt().toString());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Authentication failed");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/refresh/{userId}")
    public ResponseEntity<Map<String, Object>> refreshToken(@PathVariable String userId) {
        try {
            UserToken userToken = authService.refreshAccessToken(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userId", userToken.getUserId());
            response.put("message", "Token refreshed successfully");
            response.put("expiresAt", userToken.getExpiresAt().toString());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Token refresh failed");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Keep the original callback for backward compatibility or browser testing
    @GetMapping("/callback")
    public void callback(@RequestParam String code, HttpServletResponse response) throws IOException {
        // Redirect to callback endpoint
        response.sendRedirect("/auth/spotify/callback?code=" + code);
    }
}