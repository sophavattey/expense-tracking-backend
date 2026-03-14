package com.example.finset.controller;

import com.example.finset.dto.ExpenseDto;
import com.example.finset.entity.Expense;
import com.example.finset.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    /* ─── Personal ───────────────────────────────────────────────── */

    @GetMapping
    public ResponseEntity<?> getAll(
        @AuthenticationPrincipal UserDetails principal,
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false)    UUID categoryId,
        @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false)    Expense.Currency currency
    ) {
        UUID userId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(Map.of("success", true,
            "data", expenseService.getExpenses(userId, categoryId, from, to, currency, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID id
    ) {
        UUID userId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(Map.of("success", true,
            "data", expenseService.getById(userId, id)));
    }

    @PostMapping
    public ResponseEntity<?> create(
        @AuthenticationPrincipal UserDetails principal,
        @Valid @RequestBody ExpenseDto.Request req
    ) {
        UUID userId = UUID.fromString(principal.getUsername());
        return ResponseEntity.status(201).body(Map.of(
            "success", true,
            "message", "Expense recorded",
            "data",    expenseService.create(userId, req)
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID id,
        @Valid @RequestBody ExpenseDto.Request req
    ) {
        UUID userId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Expense updated",
            "data",    expenseService.update(userId, id, req)
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID id
    ) {
        UUID userId = UUID.fromString(principal.getUsername());
        expenseService.delete(userId, id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Expense deleted"));
    }

    @GetMapping("/summary")
    public ResponseEntity<?> summary(
        @AuthenticationPrincipal UserDetails principal,
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) Integer month
    ) {
        UUID userId = UUID.fromString(principal.getUsername());
        YearMonth ym = YearMonth.now();
        int y = year  != null ? year  : ym.getYear();
        int m = month != null ? month : ym.getMonthValue();
        return ResponseEntity.ok(Map.of("success", true,
            "data", expenseService.getMonthlySummary(userId, y, m)));
    }

    /* ─── Group ──────────────────────────────────────────────────── */

    /**
     * GET /api/expenses/group/{groupId}
     * Returns paginated expenses for all members of the group.
     * Requester must be a member of the group.
     */
    @GetMapping("/group/{groupId}")
    public ResponseEntity<?> getGroupExpenses(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID groupId,
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false)    UUID categoryId,
        @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false)    Expense.Currency currency
    ) {
        UUID userId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(Map.of("success", true,
            "data", expenseService.getGroupExpenses(userId, groupId, categoryId, from, to, currency, page, size)));
    }


    /**
     * POST /api/expenses/group/{groupId}
     * Creates an expense attributed to the requester, visible to all group members.
     * Requester must be a member of the group.
     */
    @PostMapping("/group/{groupId}")
    public ResponseEntity<?> createForGroup(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID groupId,
        @Valid @RequestBody ExpenseDto.Request req
    ) {
        UUID userId = UUID.fromString(principal.getUsername());
        return ResponseEntity.status(201).body(Map.of(
            "success", true,
            "message", "Expense recorded",
            "data",    expenseService.createForGroup(userId, groupId, req)
        ));
    }

    /**
     * GET /api/expenses/group/{groupId}/summary
     * Returns monthly summary (totals + category breakdown) for a group.
     * Requester must be a member of the group.
     */
    @GetMapping("/group/{groupId}/summary")
    public ResponseEntity<?> getGroupSummary(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID groupId,
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) Integer month
    ) {
        UUID userId = UUID.fromString(principal.getUsername());
        YearMonth ym = YearMonth.now();
        int y = year  != null ? year  : ym.getYear();
        int m = month != null ? month : ym.getMonthValue();
        return ResponseEntity.ok(Map.of("success", true,
            "data", expenseService.getGroupMonthlySummary(userId, groupId, y, m)));
    }
}