package com.example.finset.dto;

import com.example.finset.entity.Budget;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class BudgetDto {

    /* ── Create / Update ─────────────────────────────────────── */
    @Data
    public static class Request {

        /** null → overall budget */
        private Long categoryId;

        @NotNull(message = "Period is required")
        private Budget.Period period;

        @NotNull(message = "Limit is required")
        @DecimalMin(value = "0.01", message = "Limit must be greater than 0")
        @Digits(integer = 13, fraction = 2)
        private BigDecimal limitUsd;

        private boolean recurring = true;

        /** Required only when recurring = false */
        private LocalDate startDate;
        private LocalDate endDate;
    }

    /* ── Simple response (create / update / list) ────────────── */
    @Data
    public static class Response {
        private Long                 id;
        private CategoryDto.Response category;   // null = overall budget
        private Budget.Period        period;
        private BigDecimal           limitUsd;
        private boolean              recurring;
        private LocalDate            startDate;
        private LocalDate            endDate;
    }

    /* ── Rich status response (budget tracker) ───────────────── */
    @Data
    public static class Status {
        private Long                 id;
        private CategoryDto.Response category;   // null = overall budget
        private Budget.Period        period;
        private BigDecimal           limitUsd;
        private BigDecimal           limitKhr;
        private BigDecimal           spentUsd;
        private BigDecimal           spentKhr;
        private BigDecimal           remainingUsd;
        private BigDecimal           remainingKhr;
        private int                  percentage;  // 0–100+ (can exceed if over budget)
        private boolean              isOver;
        private String               periodLabel; // e.g. "Mar 2026", "Week of Mar 3"
        private LocalDate            periodStart;
        private LocalDate            periodEnd;
    }

    /* ── Summary across all budgets ──────────────────────────── */
    @Data
    public static class Summary {
        private int        totalBudgets;
        private int        overBudgetCount;
        private int        nearLimitCount;   // >= 80% but not over
        private BigDecimal totalLimitUsd;
        private BigDecimal totalSpentUsd;
        private BigDecimal totalRemainingUsd;
        private List<Status> statuses;
    }
}