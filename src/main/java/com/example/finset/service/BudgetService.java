package com.example.finset.service;

import com.example.finset.dto.BudgetDto;
import com.example.finset.entity.Budget;
import com.example.finset.entity.Category;
import com.example.finset.entity.Group;
import com.example.finset.entity.GroupMember;
import com.example.finset.entity.User;
import com.example.finset.exception.ResourceNotFoundException;
import com.example.finset.repository.BudgetRepository;
import com.example.finset.repository.CategoryRepository;
import com.example.finset.repository.GroupRepository;
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

    private final BudgetRepository    budgetRepository;
    private final CategoryRepository  categoryRepository;
    private final UserRepository      userRepository;
    private final GroupRepository     groupRepository;
    private final CategoryService     categoryService;
    private final NotificationService notificationService;

    /* ─── Read — personal ───────────────────────────────────────── */

    @Transactional(readOnly = true)
    public BudgetDto.Summary getStatus(UUID userId) {
        User user = getUser(userId);
        List<Budget> budgets = budgetRepository.findAllByUserWithCategory(user);
        // Pass null groupId → personal mode (e.group IS NULL filter applied in repo)
        return buildSummary(budgets, userId, null);
    }

    /* ─── Read — group ──────────────────────────────────────────── */

    @Transactional(readOnly = true)
    public BudgetDto.Summary getGroupStatus(UUID userId, UUID groupId) {
        getGroupAndVerifyMember(groupId, userId);
        List<Budget> budgets = budgetRepository.findAllByGroup(
            groupRepository.findByIdWithMembers(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"))
        );
        // Pass groupId → group mode (e.group.id = groupId filter applied in repo)
        return buildSummary(budgets, userId, groupId);
    }

    /* ─── Create — personal ─────────────────────────────────────── */

    @Transactional
    public BudgetDto.Response create(UUID userId, BudgetDto.Request req) {
        User     user     = getUser(userId);
        Category category = resolveCategory(req.getCategoryId(), userId);

        if (req.getCategoryId() == null)
            throw new IllegalArgumentException("A category is required for budgets.");

        if (budgetRepository.existsDuplicate(user, req.getPeriod(), req.getCategoryId(), null)) {
            throw new IllegalStateException(
                category.getName() + " already has a " + req.getPeriod().name().toLowerCase() + " budget.");
        }

        Budget budget = Budget.builder()
            .user(user)
            .category(category)
            .period(req.getPeriod())
            .limitUsd(resolveLimit(req))
            .build();

        BudgetDto.Response saved = toResponse(budgetRepository.save(budget), userId);
        checkBudgetThresholds(budget, userId, null, false, null);
        return saved;
    }

    /* ─── Create — group (owner only) ───────────────────────────── */

    @Transactional
    public BudgetDto.Response createForGroup(UUID userId, UUID groupId, BudgetDto.Request req) {
        Group    group    = getGroupAndVerifyOwner(groupId, userId);
        User     user     = getUser(userId);
        Category category = resolveCategory(req.getCategoryId(), userId);

        if (req.getCategoryId() == null)
            throw new IllegalArgumentException("A category is required for group budgets.");

        if (budgetRepository.existsDuplicateInGroup(group, req.getPeriod(), req.getCategoryId(), null)) {
            throw new IllegalStateException(
                category.getName() + " already has a " + req.getPeriod().name().toLowerCase() + " group budget.");
        }

        Budget budget = Budget.builder()
            .user(user)
            .group(group)
            .category(category)
            .period(req.getPeriod())
            .limitUsd(resolveLimit(req))
            .build();

        BudgetDto.Response saved = toResponse(budgetRepository.save(budget), userId);
        checkBudgetThresholds(budget, userId, groupId, true, group.getName());
        return saved;
    }

    /* ─── Update ────────────────────────────────────────────────── */

    @Transactional
    public BudgetDto.Response update(UUID userId, UUID budgetId, BudgetDto.Request req) {
        User   user   = getUser(userId);
        Budget budget = budgetRepository.findByIdAndUser(budgetId, user)
            .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        if (budget.getGroup() != null)
            getGroupAndVerifyOwner(budget.getGroup().getId(), userId);

        Category category = resolveCategory(req.getCategoryId(), userId);

        if (req.getCategoryId() == null)
            throw new IllegalArgumentException("A category is required for budgets.");

        boolean isDupe = budget.getGroup() != null
            ? budgetRepository.existsDuplicateInGroup(budget.getGroup(), req.getPeriod(), req.getCategoryId(), budgetId)
            : budgetRepository.existsDuplicate(user, req.getPeriod(), req.getCategoryId(), budgetId);

        if (isDupe) {
            throw new IllegalStateException(
                category.getName() + " already has a " + req.getPeriod().name().toLowerCase() + " budget.");
        }

        budget.setCategory(category);
        budget.setPeriod(req.getPeriod());
        budget.setLimitUsd(resolveLimit(req));

        BudgetDto.Response saved = toResponse(budgetRepository.save(budget), userId);
        boolean isGroupBudget = budget.getGroup() != null;
        if (isGroupBudget) {
            UUID gid = budget.getGroup().getId();
            checkBudgetThresholds(budget, userId, gid, true, budget.getGroup().getName());
        } else {
            checkBudgetThresholds(budget, userId, null, false, null);
        }
        return saved;
    }

    /* ─── Delete ────────────────────────────────────────────────── */

    @Transactional
    public void delete(UUID userId, UUID budgetId) {
        User   user   = getUser(userId);
        Budget budget = budgetRepository.findByIdAndUser(budgetId, user)
            .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        if (budget.getGroup() != null)
            getGroupAndVerifyOwner(budget.getGroup().getId(), userId);

        budgetRepository.delete(budget);
    }

    /* ─── Period window ─────────────────────────────────────────── */

    public record PeriodRange(LocalDate start, LocalDate end, String label) {}

    public PeriodRange currentPeriod(Budget budget) {
        LocalDate today = LocalDate.now();
        return switch (budget.getPeriod()) {
            case DAILY -> new PeriodRange(
                today, today.plusDays(1),
                today.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
            );
            case WEEKLY -> {
                LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate sunday = monday.plusDays(6);
                yield new PeriodRange(
                    monday, sunday.plusDays(1),
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

    /* ─── Budget threshold check ────────────────────────────────── */

    public void checkBudgetThresholds(Budget budget, UUID userId, UUID groupId,
                                       boolean isGroup, String groupName) {
        BudgetDto.Status status = toStatus(budget, userId, groupId);
        int pct = status.getPercentage();
        if (pct < NEAR_PCT) return;

        List<User> recipients = isGroup
            ? budget.getGroup().getMembers().stream()
                .map(GroupMember::getUser).toList()
            : List.of(budget.getUser());

        String  catName     = budget.getCategory() != null ? budget.getCategory().getName() : "Overall";
        boolean exceeded    = status.isOver();
        String  periodLabel = status.getPeriodLabel();

        for (User recipient : recipients) {
            notificationService.notifyBudgetThreshold(
                recipient, catName, pct,
                status.getRemainingUsd(), exceeded,
                periodLabel, isGroup, groupName
            );
        }
    }

    /* ─── Shared summary builder ────────────────────────────────── */

    private BudgetDto.Summary buildSummary(List<Budget> budgets, UUID userId, UUID groupId) {
        List<BudgetDto.Status> statuses = budgets.stream()
            .map(b -> toStatus(b, userId, groupId))
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

    /* ─── toStatus ──────────────────────────────────────────────── */

    private BudgetDto.Status toStatus(Budget b, UUID userId, UUID groupId) {
        PeriodRange range = currentPeriod(b);

        BigDecimal spent;
        if (groupId != null) {
            // GROUP mode — filter by groupId only (not by user)
            spent = b.getCategory() != null
                ? budgetRepository.sumSpentForCategoryByGroup(
                    groupId, b.getCategory().getId(), range.start(), range.end())
                : budgetRepository.sumSpentOverallByGroup(
                    groupId, range.start(), range.end());
        } else {
            // PERSONAL mode — filter by userId AND group IS NULL
            spent = b.getCategory() != null
                ? budgetRepository.sumSpentForCategory(
                    userId, b.getCategory().getId(), range.start(), range.end())
                : budgetRepository.sumSpentOverall(
                    userId, range.start(), range.end());
        }

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
            ? categoryService.toResponse(b.getCategory(), userId) : null);
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

    /* ─── Helpers ───────────────────────────────────────────────── */

    private BigDecimal resolveLimit(BudgetDto.Request req) {
        boolean isKhr = "KHR".equalsIgnoreCase(req.getInputCurrency());
        if (isKhr) {
            if (req.getLimitKhr() == null || req.getLimitKhr().compareTo(BigDecimal.ONE) < 0)
                throw new IllegalArgumentException("KHR limit must be at least ៛1.");
            return req.getLimitKhr().divide(KHR_RATE, 4, RoundingMode.HALF_UP);
        }
        if (req.getLimitUsd() == null || req.getLimitUsd().compareTo(new BigDecimal("0.01")) < 0)
            throw new IllegalArgumentException("USD limit must be at least $0.01.");
        return req.getLimitUsd();
    }

    private BudgetDto.Response toResponse(Budget b, UUID userId) {
        BudgetDto.Response r = new BudgetDto.Response();
        r.setId(b.getId());
        r.setCategory(b.getCategory() != null
            ? categoryService.toResponse(b.getCategory(), userId) : null);
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

    private Group getGroupAndVerifyMember(UUID groupId, UUID userId) {
        Group group = groupRepository.findByIdWithMembers(groupId)
            .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        boolean isMember = group.getMembers().stream()
            .anyMatch(m -> m.getUser().getId().equals(userId));
        if (!isMember) throw new SecurityException("You are not a member of this group.");
        return group;
    }

    private Group getGroupAndVerifyOwner(UUID groupId, UUID userId) {
        Group group = groupRepository.findByIdWithMembers(groupId)
            .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        if (!group.getOwner().getId().equals(userId))
            throw new SecurityException("Only the group owner can manage budgets.");
        return group;
    }

    private List<UUID> getMemberIds(Group group) {
        return group.getMembers().stream()
            .map(m -> m.getUser().getId())
            .toList();
    }
}