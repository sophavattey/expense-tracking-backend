package com.example.finset.service;

import com.example.finset.dto.BudgetDto;
import com.example.finset.entity.Budget;
import com.example.finset.entity.Category;
import com.example.finset.entity.User;
import com.example.finset.exception.ResourceNotFoundException;
import com.example.finset.repository.BudgetRepository;
import com.example.finset.repository.CategoryRepository;
import com.example.finset.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private static final BigDecimal KHR_RATE = new BigDecimal("4000");
    private static final int        NEAR_PCT = 80;

    private final BudgetRepository   budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository     userRepository;
    private final CategoryService    categoryService;

    /* ─── Read ──────────────────────────────────────────────────── */

    @Transactional(readOnly = true)
    public BudgetDto.Summary getStatus(UUID userId) {
        User user = getUser(userId);
        List<Budget> budgets = budgetRepository.findAllByUserWithCategory(user);

        List<BudgetDto.Status> statuses = budgets.stream()
            .map(b -> toStatus(b, userId))
            .toList();

        BigDecimal totalLimit = statuses.stream()
            .map(BudgetDto.Status::getLimitUsd)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpent = statuses.stream()
            .map(BudgetDto.Status::getSpentUsd)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BudgetDto.Summary summary = new BudgetDto.Summary();
        summary.setTotalBudgets(statuses.size());
        summary.setOverBudgetCount((int) statuses.stream().filter(BudgetDto.Status::isOver).count());
        summary.setNearLimitCount((int) statuses.stream()
            .filter(s -> !s.isOver() && s.getPercentage() >= NEAR_PCT).count());
        summary.setTotalLimitUsd(totalLimit);
        summary.setTotalSpentUsd(totalSpent);
        summary.setTotalRemainingUsd(totalLimit.subtract(totalSpent));
        summary.setStatuses(statuses);
        return summary;
    }

    /* ─── Create ────────────────────────────────────────────────── */

    @Transactional
    public BudgetDto.Response create(UUID userId, BudgetDto.Request req) {
        User user = getUser(userId);

        if (req.getCategoryId() == null)
            throw new IllegalArgumentException("A category is required for budgets.");

        Category category = resolveCategory(req.getCategoryId(), userId);

        if (budgetRepository.existsDuplicate(user, req.getPeriod(), req.getCategoryId(), null)) {
            String label = category != null ? category.getName() : "Overall";
            throw new IllegalStateException(
                label + " already has a " + req.getPeriod().name().toLowerCase() + " budget.");
        }

        Budget budget = Budget.builder()
            .user(user)
            .category(category)
            .period(req.getPeriod())
            .limitUsd(resolveLimit(req))
            .build();

        return toResponse(budgetRepository.save(budget), userId);
    }

    /* ─── Update ────────────────────────────────────────────────── */

    @Transactional
    public BudgetDto.Response update(UUID userId, UUID budgetId, BudgetDto.Request req) {
        User user = getUser(userId);
        Budget budget = budgetRepository.findByIdAndUser(budgetId, user)
            .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        if (req.getCategoryId() == null)
            throw new IllegalArgumentException("A category is required for budgets.");

        Category category = resolveCategory(req.getCategoryId(), userId);

        if (budgetRepository.existsDuplicate(user, req.getPeriod(), req.getCategoryId(), budgetId)) {
            String label = category != null ? category.getName() : "Overall";
            throw new IllegalStateException(
                label + " already has a " + req.getPeriod().name().toLowerCase() + " budget.");
        }

        budget.setCategory(category);
        budget.setPeriod(req.getPeriod());
        budget.setLimitUsd(resolveLimit(req));

        return toResponse(budgetRepository.save(budget), userId);
    }

    /* ─── Delete ────────────────────────────────────────────────── */

    @Transactional
    public void delete(UUID userId, UUID budgetId) {
        User user = getUser(userId);
        Budget budget = budgetRepository.findByIdAndUser(budgetId, user)
            .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        budgetRepository.delete(budget);
    }

    /* ─── Period window ─────────────────────────────────────────── */

    public record PeriodRange(LocalDate start, LocalDate end, String label) {}

    public PeriodRange currentPeriod(Budget budget) {
        LocalDate today = LocalDate.now();
        return switch (budget.getPeriod()) {
            case DAILY -> new PeriodRange(
                today,
                today.plusDays(1),
                today.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
            );
            case WEEKLY -> {
                LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate sunday = monday.plusDays(6);
                yield new PeriodRange(
                    monday,
                    sunday.plusDays(1),
                    "Week of " + monday.format(DateTimeFormatter.ofPattern("MMM d"))
                );
            }
            case MONTHLY -> {
                LocalDate start = today.with(TemporalAdjusters.firstDayOfMonth());
                LocalDate end   = today.with(TemporalAdjusters.firstDayOfNextMonth());
                yield new PeriodRange(
                    start, end,
                    today.format(DateTimeFormatter.ofPattern("MMM yyyy"))
                );
            }
        };
    }

    /* ─── Helpers ───────────────────────────────────────────────── */

    /**
     * Resolves the limit to USD regardless of input currency.
     *
     *   inputCurrency = "KHR" → limitUsd = limitKhr / 4000 (rounded to 4 decimal places)
     *   inputCurrency = "USD" → limitUsd used directly
     */
    private BigDecimal resolveLimit(BudgetDto.Request req) {
        boolean isKhr = "KHR".equalsIgnoreCase(req.getInputCurrency());

        if (isKhr) {
            if (req.getLimitKhr() == null || req.getLimitKhr().compareTo(BigDecimal.ONE) < 0)
                throw new IllegalArgumentException("KHR limit must be at least ៛1.");
            return req.getLimitKhr()
                .divide(KHR_RATE, 4, RoundingMode.HALF_UP);
        }

        // USD (default)
        if (req.getLimitUsd() == null || req.getLimitUsd().compareTo(new BigDecimal("0.01")) < 0)
            throw new IllegalArgumentException("USD limit must be at least $0.01.");
        return req.getLimitUsd();
    }

    /* ─── Internal mappers ──────────────────────────────────────── */

    private BudgetDto.Status toStatus(Budget b, UUID userId) {
        PeriodRange range = currentPeriod(b);

        BigDecimal spent = b.getCategory() != null
            ? budgetRepository.sumSpentForCategory(
                userId, b.getCategory().getId(), range.start(), range.end())
            : budgetRepository.sumSpentOverall(userId, range.start(), range.end());

        BigDecimal limit     = b.getLimitUsd();
        BigDecimal remaining = limit.subtract(spent);
        BigDecimal limitKhr  = limit.multiply(KHR_RATE).setScale(0, RoundingMode.HALF_UP);
        BigDecimal spentKhr  = spent.multiply(KHR_RATE).setScale(0, RoundingMode.HALF_UP);
        BigDecimal remKhr    = remaining.multiply(KHR_RATE).setScale(0, RoundingMode.HALF_UP);

        int pct = limit.compareTo(BigDecimal.ZERO) == 0 ? 0
            : spent.multiply(BigDecimal.valueOf(100))
                .divide(limit, 0, RoundingMode.HALF_UP).intValue();

        BudgetDto.Status s = new BudgetDto.Status();
        s.setId(b.getId());
        s.setCategory(b.getCategory() != null
            ? categoryService.toResponse(b.getCategory(), userId)
            : null);
        s.setPeriod(b.getPeriod());
        s.setLimitUsd(limit);
        s.setLimitKhr(limitKhr);
        s.setSpentUsd(spent);
        s.setSpentKhr(spentKhr);
        s.setRemainingUsd(remaining);
        s.setRemainingKhr(remKhr);
        s.setPercentage(Math.min(pct, 999));
        s.setOver(spent.compareTo(limit) > 0);
        s.setPeriodLabel(range.label());
        s.setPeriodStart(range.start());
        s.setPeriodEnd(range.end().minusDays(1));
        return s;
    }

    private BudgetDto.Response toResponse(Budget b, UUID userId) {
        BudgetDto.Response r = new BudgetDto.Response();
        r.setId(b.getId());
        r.setCategory(b.getCategory() != null
            ? categoryService.toResponse(b.getCategory(), userId)
            : null);
        r.setPeriod(b.getPeriod());
        r.setLimitUsd(b.getLimitUsd());
        r.setLimitKhr(b.getLimitUsd().multiply(KHR_RATE).setScale(0, RoundingMode.HALF_UP));
        return r;
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Category resolveCategory(UUID categoryId, UUID userId) {
        if (categoryId == null) return null;
        return categoryRepository.findByIdAndVisibleToUser(categoryId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }
}