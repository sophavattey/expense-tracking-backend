package com.example.finset.repository;

import com.example.finset.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID>,      // ← UUID
        JpaSpecificationExecutor<Expense> {

    @EntityGraph(attributePaths = {"category"})
    Page<Expense> findAll(Specification<Expense> spec, Pageable pageable);

    Optional<Expense> findByIdAndUserId(UUID id, UUID userId);                // ← UUID

    long countByCategoryId(UUID categoryId);                                  // ← UUID

    @Query("""
        SELECT COALESCE(SUM(e.amountBase), 0) FROM Expense e
        WHERE e.user.id  = :userId
          AND e.date    >= :startDate
          AND e.date     < :endDate
        """)
    BigDecimal sumBaseByUserAndMonth(
        @Param("userId")    UUID userId,                                      // ← UUID
        @Param("startDate") LocalDate startDate,
        @Param("endDate")   LocalDate endDate
    );

    @Query("""
        SELECT c.name, c.icon, c.color, SUM(e.amountBase)
        FROM Expense e JOIN e.category c
        WHERE e.user.id  = :userId
          AND e.date    >= :startDate
          AND e.date     < :endDate
        GROUP BY c.id, c.name, c.icon, c.color
        ORDER BY SUM(e.amountBase) DESC
        """)
    List<Object[]> sumByCategoryForMonth(
        @Param("userId")    UUID userId,                                      // ← UUID
        @Param("startDate") LocalDate startDate,
        @Param("endDate")   LocalDate endDate
    );
}