package com.example.finset.service;

import com.example.finset.dto.ExpenseDto;
import com.example.finset.entity.Category;
import com.example.finset.entity.Expense;
import com.example.finset.entity.User;
import com.example.finset.exception.ResourceNotFoundException;
import com.example.finset.repository.CategoryRepository;
import com.example.finset.repository.ExpenseRepository;
import com.example.finset.repository.ExpenseSpec;
import com.example.finset.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private static final BigDecimal KHR_RATE = new BigDecimal("4000");

    private final ExpenseRepository  expenseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository     userRepository;
    private final CategoryService    categoryService;

    private BigDecimal toUsdBase(BigDecimal amount, Expense.Currency currency) {
        if (currency == Expense.Currency.USD) return amount;
        return amount.divide(KHR_RATE, 2, RoundingMode.HALF_UP);
    }

    public ExpenseDto.PageResponse getExpenses(
        UUID userId, UUID categoryId,                                          // ← UUID
        LocalDate from, LocalDate to,
        Expense.Currency currency,
        int page, int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 200));
        Page<Expense> result = expenseRepository.findAll(
            ExpenseSpec.filter(userId, categoryId, from, to, currency),
            pageable
        );

        ExpenseDto.PageResponse resp = new ExpenseDto.PageResponse();
        resp.setContent(result.getContent().stream().map(this::toResponse).toList());
        resp.setPage(result.getNumber());
        resp.setSize(result.getSize());
        resp.setTotalElements(result.getTotalElements());
        resp.setTotalPages(result.getTotalPages());
        resp.setLast(result.isLast());
        return resp;
    }

    public ExpenseDto.Response getById(UUID userId, UUID expenseId) {          // ← UUID
        Expense expense = expenseRepository.findByIdAndUserId(expenseId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        return toResponse(expense);
    }

    @Transactional
    public ExpenseDto.Response create(UUID userId, ExpenseDto.Request req) {   // ← UUID
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Category category = categoryRepository
            .findByIdAndVisibleToUser(req.getCategoryId(), userId)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Expense expense = Expense.builder()
            .user(user)
            .category(category)
            .amount(req.getAmount())
            .currency(req.getCurrency())
            .amountBase(toUsdBase(req.getAmount(), req.getCurrency()))
            .date(req.getDate())
            .merchantName(req.getMerchantName())
            .note(req.getNote())
            .paymentMethod(req.getPaymentMethod() != null
                ? req.getPaymentMethod()
                : Expense.PaymentMethod.CASH)
            .build();

        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseDto.Response update(UUID userId, UUID expenseId, ExpenseDto.Request req) { // ← UUID
        Expense expense = expenseRepository.findByIdAndUserId(expenseId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        Category category = categoryRepository
            .findByIdAndVisibleToUser(req.getCategoryId(), userId)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        expense.setAmount(req.getAmount());
        expense.setCurrency(req.getCurrency());
        expense.setAmountBase(toUsdBase(req.getAmount(), req.getCurrency()));
        expense.setDate(req.getDate());
        expense.setCategory(category);
        expense.setMerchantName(req.getMerchantName());
        expense.setNote(req.getNote());
        if (req.getPaymentMethod() != null) {
            expense.setPaymentMethod(req.getPaymentMethod());
        }

        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void delete(UUID userId, UUID expenseId) {                          // ← UUID
        Expense expense = expenseRepository.findByIdAndUserId(expenseId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        expenseRepository.delete(expense);
    }

    public ExpenseDto.MonthlySummary getMonthlySummary(UUID userId, int year, int month) { // ← UUID
        YearMonth ym        = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate   = ym.plusMonths(1).atDay(1);

        BigDecimal totalUsd = expenseRepository
            .sumBaseByUserAndMonth(userId, startDate, endDate);
        BigDecimal totalKhr = totalUsd.multiply(KHR_RATE).setScale(0, RoundingMode.HALF_UP);

        List<Object[]> rows = expenseRepository
            .sumByCategoryForMonth(userId, startDate, endDate);

        List<ExpenseDto.CategoryBreakdown> breakdown = rows.stream().map(row -> {
            ExpenseDto.CategoryBreakdown b = new ExpenseDto.CategoryBreakdown();
            b.setCategoryName((String)  row[0]);
            b.setCategoryIcon((String)  row[1]);
            b.setCategoryColor((String) row[2]);
            BigDecimal catUsd = (BigDecimal) row[3];
            b.setTotalUsd(catUsd);
            b.setTotalKhr(catUsd.multiply(KHR_RATE).setScale(0, RoundingMode.HALF_UP));
            b.setPercentage(totalUsd.compareTo(BigDecimal.ZERO) == 0 ? 0
                : catUsd.multiply(BigDecimal.valueOf(100))
                    .divide(totalUsd, 0, RoundingMode.HALF_UP).intValue());
            return b;
        }).toList();

        ExpenseDto.MonthlySummary summary = new ExpenseDto.MonthlySummary();
        summary.setYear(year);
        summary.setMonth(month);
        summary.setTotalSpentUsd(totalUsd);
        summary.setTotalSpentKhr(totalKhr);
        summary.setBreakdown(breakdown);
        return summary;
    }

    private ExpenseDto.Response toResponse(Expense e) {
        ExpenseDto.Response r = new ExpenseDto.Response();
        r.setId(e.getId());
        r.setAmount(e.getAmount());
        r.setCurrency(e.getCurrency());
        r.setAmountBase(e.getAmountBase());
        r.setDate(e.getDate());
        r.setMerchantName(e.getMerchantName());
        r.setNote(e.getNote());
        r.setPaymentMethod(e.getPaymentMethod());
        r.setCreatedAt(e.getCreatedAt());
        r.setUpdatedAt(e.getUpdatedAt());
        UUID userId = e.getUser() != null ? e.getUser().getId() : null;       // ← UUID
        r.setCategory(categoryService.toResponse(e.getCategory(), userId));
        return r;
    }
}