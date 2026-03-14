package com.example.finset.dto;

import com.example.finset.entity.Expense;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ExpenseDto {

    @Data
    public static class Request {
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @Digits(integer = 13, fraction = 2, message = "Invalid amount format")
        private BigDecimal amount;

        @NotNull(message = "Currency is required")
        private Expense.Currency currency;

        @NotNull(message = "Date is required")
        private LocalDate date;

        @NotNull(message = "Category is required")
        private UUID categoryId;

        @Size(max = 150, message = "Merchant name must be 150 characters or less")
        private String merchantName;

        @Size(max = 500, message = "Note must be 500 characters or less")
        private String note;

        private Expense.PaymentMethod paymentMethod = Expense.PaymentMethod.CASH;
    }

    @Data
    public static class Response {
        private UUID                  id;
        /** The user who created this expense — used for group member attribution */
        private UUID                  userId;
        private BigDecimal            amount;
        private Expense.Currency      currency;
        private BigDecimal            amountBase;
        private LocalDate             date;
        private CategoryDto.Response  category;
        private String                merchantName;
        private String                note;
        private Expense.PaymentMethod paymentMethod;
        private LocalDateTime         createdAt;
        private LocalDateTime         updatedAt;
    }

    @Data
    public static class PageResponse {
        private List<Response> content;
        private int     page;
        private int     size;
        private long    totalElements;
        private int     totalPages;
        private boolean last;
    }

    @Data
    public static class MonthlySummary {
        private int        year;
        private int        month;
        private BigDecimal totalSpentUsd;
        private BigDecimal totalSpentKhr;
        private List<CategoryBreakdown> breakdown;
    }

    @Data
    public static class CategoryBreakdown {
        private String     categoryName;
        private String     categoryIcon;
        private String     categoryColor;
        private BigDecimal totalUsd;
        private BigDecimal totalKhr;
        private int        percentage;
    }
}