package com.example.finset.repository;

import com.example.finset.entity.Budget;
import com.example.finset.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    /** All budgets for a user, ordered: overall first, then by period, then category name */
    @Query("""
        SELECT b FROM Budget b
        LEFT JOIN FETCH b.category
        WHERE b.user = :user
        ORDER BY
            CASE WHEN b.category IS NULL THEN 0 ELSE 1 END,
            b.period,
            b.category.name
        """)
    List<Budget> findAllByUserWithCategory(@Param("user") User user);

    Optional<Budget> findByIdAndUser(Long id, User user);

    /** Check duplicate: same user + category (null = overall) + period */
    @Query("""
        SELECT COUNT(b) > 0 FROM Budget b
        WHERE b.user = :user
          AND b.period = :period
          AND ((:categoryId IS NULL AND b.category IS NULL)
               OR (b.category.id = :categoryId))
          AND (:excludeId IS NULL OR b.id <> :excludeId)
        """)
    boolean existsDuplicate(
        @Param("user")       User user,
        @Param("period")     Budget.Period period,
        @Param("categoryId") Long categoryId,
        @Param("excludeId")  Long excludeId
    );

    /** Sum of amountBase (USD) for a specific category in a date range */
    @Query("""
        SELECT COALESCE(SUM(e.amountBase), 0) FROM Expense e
        WHERE e.user.id      = :userId
          AND e.category.id  = :categoryId
          AND e.date         >= :startDate
          AND e.date         <  :endDate
        """)
    BigDecimal sumSpentForCategory(
        @Param("userId")     Long userId,
        @Param("categoryId") Long categoryId,
        @Param("startDate")  LocalDate startDate,
        @Param("endDate")    LocalDate endDate
    );

    /** Sum of amountBase (USD) across ALL categories in a date range (overall budget) */
    @Query("""
        SELECT COALESCE(SUM(e.amountBase), 0) FROM Expense e
        WHERE e.user.id  = :userId
          AND e.date    >= :startDate
          AND e.date     < :endDate
        """)
    BigDecimal sumSpentOverall(
        @Param("userId")    Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate")   LocalDate endDate
    );
}