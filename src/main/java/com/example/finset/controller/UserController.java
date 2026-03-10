package com.example.finset.controller;

import com.example.finset.dto.response.ApiResponse;
import com.example.finset.dto.response.AuthResponse;
import com.example.finset.entity.User;
import com.example.finset.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService    userService;
    private final AuthController authController;   // reuse toDto()

    /**
     * PUT /api/user/preferences
     * Body: { "preferredCurrency": "KHR" }
     * Returns the updated AuthResponse so the frontend can sync its user state.
     */
    @PutMapping("/preferences")
    public ApiResponse<AuthResponse> updatePreferences(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {

        UUID userId  = UUID.fromString(userDetails.getUsername());
        String currency = body.get("preferredCurrency");

        User updated = userService.updatePreferredCurrency(userId, currency);
        log.info("User {} updated preferredCurrency to {}", userId, currency);
        return ApiResponse.ok("Preferences updated", authController.toDto(updated));
    }
}