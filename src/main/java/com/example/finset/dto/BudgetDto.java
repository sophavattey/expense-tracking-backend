package com.example.finset.dto;

import com.example.finset.entity.Budget;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class BudgetDto {

    @Data
    public static class Request {
        /** null → overall budget */
        private UUID categoryId;               // ← UUID

        @NotNull(message = "Period is required")
        private Budget.Period period;

        @NotNull(message = "Limit is required")
        @DecimalMin(value = "0.01", message = "Limit must be greater than 0")
        @Digits(integer = 13, fraction = 2)
        private BigDecimal limitUsd;

        private boolean recurring = true;

        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Data
    public static class Response {
        private UUID                 id;       // ← UUID
        private CategoryDto.Response category;
        private Budget.Period        period;
        private BigDecimal           limitUsd;
        private boolean              recurring;
        private LocalDate            startDate;
        private LocalDate            endDate;
    }

    @Data
    public static class Status {
        private UUID                 id;       // ← UUID
        private CategoryDto.Response category;
        private Budget.Period        period;
        private BigDecimal           limitUsd;
        private BigDecimal           limitKhr;
        private BigDecimal           spentUsd;
        private BigDecimal           spentKhr;
        private BigDecimal           remainingUsd;
        private BigDecimal           remainingKhr;
        private int                  percentage;
        private boolean              isOver;
        private String               periodLabel;
        private LocalDate            periodStart;
        private LocalDate            periodEnd;
    }

    @Data
    public static class Summary {
        private int        totalBudgets;
        private int        overBudgetCount;
        private int        nearLimitCount;
        private BigDecimal totalLimitUsd;
        private BigDecimal totalSpentUsd;
        private BigDecimal totalRemainingUsd;
        private List<Status> statuses;
    }
}