package com.example.finset.controller;

import com.example.finset.dto.BudgetDto;
import com.example.finset.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@AuthenticationPrincipal UserDetails principal) {
        UUID userId = UUID.fromString(principal.getUsername());               // ← UUID
        return ResponseEntity.ok(Map.of("success", true,
            "data", budgetService.getStatus(userId)));
    }

    @PostMapping
    public ResponseEntity<?> create(
        @AuthenticationPrincipal UserDetails principal,
        @Valid @RequestBody BudgetDto.Request req
    ) {
        UUID userId = UUID.fromString(principal.getUsername());               // ← UUID
        return ResponseEntity.status(201).body(Map.of(
            "success", true,
            "message", "Budget created",
            "data",    budgetService.create(userId, req)
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID id,                                                // ← UUID
        @Valid @RequestBody BudgetDto.Request req
    ) {
        UUID userId = UUID.fromString(principal.getUsername());               // ← UUID
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Budget updated",
            "data",    budgetService.update(userId, id, req)
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID id                                                 // ← UUID
    ) {
        UUID userId = UUID.fromString(principal.getUsername());               // ← UUID
        budgetService.delete(userId, id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Budget deleted"));
    }
}