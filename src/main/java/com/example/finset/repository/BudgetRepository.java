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
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {       // ← UUID

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

    Optional<Budget> findByIdAndUser(UUID id, User user);                     // ← UUID

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
        @Param("categoryId") UUID categoryId,                                 // ← UUID
        @Param("excludeId")  UUID excludeId                                   // ← UUID
    );

    @Query("""
        SELECT COALESCE(SUM(e.amountBase), 0) FROM Expense e
        WHERE e.user.id      = :userId
          AND e.category.id  = :categoryId
          AND e.date         >= :startDate
          AND e.date         <  :endDate
        """)
    BigDecimal sumSpentForCategory(
        @Param("userId")     UUID userId,                                     // ← UUID
        @Param("categoryId") UUID categoryId,                                 // ← UUID
        @Param("startDate")  LocalDate startDate,
        @Param("endDate")    LocalDate endDate
    );

    @Query("""
        SELECT COALESCE(SUM(e.amountBase), 0) FROM Expense e
        WHERE e.user.id  = :userId
          AND e.date    >= :startDate
          AND e.date     < :endDate
        """)
    BigDecimal sumSpentOverall(
        @Param("userId")    UUID userId,                                      // ← UUID
        @Param("startDate") LocalDate startDate,
        @Param("endDate")   LocalDate endDate
    );
}