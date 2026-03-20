package com.example.finset.repository;

import com.example.finset.entity.Budget;
import com.example.finset.entity.Group;
import com.example.finset.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    /* ─── Fetch ──────────────────────────────────────────────────── */

    @Query("""
        SELECT b FROM Budget b
        LEFT JOIN FETCH b.category
        WHERE b.user = :user
          AND b.group IS NULL
        ORDER BY b.period, b.category.name
        """)
    List<Budget> findAllByUserWithCategory(@Param("user") User user);

    @Query("""
        SELECT b FROM Budget b
        LEFT JOIN FETCH b.category
        WHERE b.group = :group
        ORDER BY b.period, b.category.name
        """)
    List<Budget> findAllByGroup(@Param("group") Group group);

    Optional<Budget> findByIdAndUser(UUID id, User user);

    /* ─── Duplicate check ────────────────────────────────────────── */

    @Query("""
        SELECT COUNT(b) > 0 FROM Budget b
        WHERE b.user = :user
          AND b.group IS NULL
          AND b.period = :period
          AND b.category.id = :categoryId
          AND (:excludeId IS NULL OR b.id <> :excludeId)
        """)
    boolean existsDuplicate(
        @Param("user")       User user,
        @Param("period")     Budget.Period period,
        @Param("categoryId") UUID categoryId,
        @Param("excludeId")  UUID excludeId
    );

    @Query("""
        SELECT COUNT(b) > 0 FROM Budget b
        WHERE b.group = :group
          AND b.period = :period
          AND b.category.id = :categoryId
          AND (:excludeId IS NULL OR b.id <> :excludeId)
        """)
    boolean existsDuplicateInGroup(
        @Param("group")      Group group,
        @Param("period")     Budget.Period period,
        @Param("categoryId") UUID categoryId,
        @Param("excludeId")  UUID excludeId
    );

    /* ─── Spend queries — PERSONAL (single userId, group IS NULL) ── */

    @Query("""
        SELECT COALESCE(SUM(e.amountBase), 0) FROM Expense e
        WHERE e.user.id     = :userId
          AND e.category.id = :categoryId
          AND e.group       IS NULL
          AND e.date        >= :startDate
          AND e.date        <  :endDate
        """)
    BigDecimal sumSpentForCategory(
        @Param("userId")     UUID userId,
        @Param("categoryId") UUID categoryId,
        @Param("startDate")  LocalDate startDate,
        @Param("endDate")    LocalDate endDate
    );

    @Query("""
        SELECT COALESCE(SUM(e.amountBase), 0) FROM Expense e
        WHERE e.user.id  = :userId
          AND e.group    IS NULL
          AND e.date    >= :startDate
          AND e.date     < :endDate
        """)
    BigDecimal sumSpentOverall(
        @Param("userId")    UUID userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate")   LocalDate endDate
    );

    /* ─── Spend queries — GROUP (filter by groupId) ──────────────── */

    @Query("""
        SELECT COALESCE(SUM(e.amountBase), 0) FROM Expense e
        WHERE e.group.id     = :groupId
          AND e.category.id  = :categoryId
          AND e.date         >= :startDate
          AND e.date         <  :endDate
        """)
    BigDecimal sumSpentForCategoryByGroup(
        @Param("groupId")    UUID groupId,
        @Param("categoryId") UUID categoryId,
        @Param("startDate")  LocalDate startDate,
        @Param("endDate")    LocalDate endDate
    );

    @Query("""
        SELECT COALESCE(SUM(e.amountBase), 0) FROM Expense e
        WHERE e.group.id  = :groupId
          AND e.date     >= :startDate
          AND e.date      < :endDate
        """)
    BigDecimal sumSpentOverallByGroup(
        @Param("groupId")   UUID groupId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate")   LocalDate endDate
    );

    /* ─── Dissolve group cleanup ─────────────────────────────────── */

    @Modifying
    @Query("DELETE FROM Budget b WHERE b.group = :group")
    void deleteAllByGroup(@Param("group") Group group);
}