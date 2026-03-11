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
        private UUID categoryId;

        @NotNull(message = "Period is required")
        private Budget.Period period;

        /**
         * The currency the user entered the limit in: "USD" or "KHR".
         * Defaults to "USD" for backwards compatibility.
         */
        @NotNull(message = "Input currency is required")
        private String inputCurrency = "USD";

        /**
         * Limit in USD — required when inputCurrency = "USD".
         * Validated in BudgetService when inputCurrency = "KHR" (limitKhr used instead).
         */
        private BigDecimal limitUsd;

        /**
         * Limit in KHR — required when inputCurrency = "KHR".
         * BudgetService converts this to USD before saving: limitUsd = limitKhr / 4000.
         */
        private BigDecimal limitKhr;
    }

    @Data
    public static class Response {
        private UUID                 id;
        private CategoryDto.Response category;
        private Budget.Period        period;
        private BigDecimal           limitUsd;   // always stored in USD
        private BigDecimal           limitKhr;   // limitUsd * 4000, for display
    }

    @Data
    public static class Status {
        private UUID                 id;
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
        private int          totalBudgets;
        private int          overBudgetCount;
        private int          nearLimitCount;
        private BigDecimal   totalLimitUsd;
        private BigDecimal   totalSpentUsd;
        private BigDecimal   totalRemainingUsd;
        private List<Status> statuses;
    }
}