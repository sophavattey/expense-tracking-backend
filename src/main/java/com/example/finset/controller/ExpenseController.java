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

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public ResponseEntity<?> getAll(
        @AuthenticationPrincipal UserDetails principal,
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false)    Long categoryId,
        @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false)    Expense.Currency currency
    ) {
        Long userId = Long.parseLong(principal.getUsername());
        ExpenseDto.PageResponse result = expenseService.getExpenses(
            userId, categoryId, from, to, currency, page, size);
        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable Long id
    ) {
        Long userId = Long.parseLong(principal.getUsername());
        ExpenseDto.Response expense = expenseService.getById(userId, id);
        return ResponseEntity.ok(Map.of("success", true, "data", expense));
    }

    @PostMapping
    public ResponseEntity<?> create(
        @AuthenticationPrincipal UserDetails principal,
        @Valid @RequestBody ExpenseDto.Request req
    ) {
        Long userId = Long.parseLong(principal.getUsername());
        ExpenseDto.Response created = expenseService.create(userId, req);
        return ResponseEntity.status(201).body(Map.of(
            "success", true,
            "message", "Expense recorded",
            "data", created
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable Long id,
        @Valid @RequestBody ExpenseDto.Request req
    ) {
        Long userId = Long.parseLong(principal.getUsername());
        ExpenseDto.Response updated = expenseService.update(userId, id, req);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Expense updated",
            "data", updated
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable Long id
    ) {
        Long userId = Long.parseLong(principal.getUsername());
        expenseService.delete(userId, id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Expense deleted"));
    }

    @GetMapping("/summary")
    public ResponseEntity<?> summary(
        @AuthenticationPrincipal UserDetails principal,
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) Integer month,
        @RequestParam(defaultValue = "USD") Expense.Currency currency
    ) {
        Long userId = Long.parseLong(principal.getUsername());
        YearMonth ym = YearMonth.now();
        int y = year  != null ? year  : ym.getYear();
        int m = month != null ? month : ym.getMonthValue();

        ExpenseDto.MonthlySummary summary =
            expenseService.getMonthlySummary(userId, y, m, currency);
        return ResponseEntity.ok(Map.of("success", true, "data", summary));
    }
}