package com.example.finset.controller;

import com.example.finset.dto.NotificationDto;
import com.example.finset.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notifService;

    @GetMapping
    public ResponseEntity<?> getNotifications(
        @AuthenticationPrincipal UserDetails principal
    ) {
        UUID userId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "OK",
            "data",    notifService.getNotifications(userId)
        ));
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllRead(
        @AuthenticationPrincipal UserDetails principal
    ) {
        UUID userId = UUID.fromString(principal.getUsername());
        notifService.markAllRead(userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "OK"));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markRead(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID id
    ) {
        UUID userId = UUID.fromString(principal.getUsername());
        notifService.markRead(userId, id);
        return ResponseEntity.ok(Map.of("success", true, "message", "OK"));
    }
}