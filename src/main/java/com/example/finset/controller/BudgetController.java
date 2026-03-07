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

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    /**
     * GET /api/budgets/status
     * Returns all budgets with live spent / remaining / percentage.
     * This is the primary endpoint used by the frontend dashboard and budget page.
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@AuthenticationPrincipal UserDetails principal) {
        Long userId = Long.parseLong(principal.getUsername());
        return ResponseEntity.ok(Map.of("success", true,
            "data", budgetService.getStatus(userId)));
    }

    /** POST /api/budgets — create a new budget */
    @PostMapping
    public ResponseEntity<?> create(
        @AuthenticationPrincipal UserDetails principal,
        @Valid @RequestBody BudgetDto.Request req
    ) {
        Long userId = Long.parseLong(principal.getUsername());
        return ResponseEntity.status(201).body(Map.of(
            "success", true,
            "message", "Budget created",
            "data",    budgetService.create(userId, req)
        ));
    }

    /** PUT /api/budgets/{id} — update a budget */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable Long id,
        @Valid @RequestBody BudgetDto.Request req
    ) {
        Long userId = Long.parseLong(principal.getUsername());
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Budget updated",
            "data",    budgetService.update(userId, id, req)
        ));
    }

    /** DELETE /api/budgets/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable Long id
    ) {
        Long userId = Long.parseLong(principal.getUsername());
        budgetService.delete(userId, id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Budget deleted"));
    }
}