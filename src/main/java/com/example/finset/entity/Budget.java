package com.example.finset.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "budgets",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"user_id", "category_id", "period"},
        name = "uk_budget_user_category_period"
    )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Budget {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * null  → overall budget (all categories combined)
     * set   → per-category budget
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Period period;

    /** Spending limit in USD */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal limitUsd;

    /**
     * true  → auto-resets every period (rolling)
     * false → one-shot budget between startDate / endDate
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean recurring = true;

    /** Only used when recurring = false */
    @Column
    private LocalDate startDate;

    /** Only used when recurring = false */
    @Column
    private LocalDate endDate;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Period { DAILY, WEEKLY, MONTHLY }
}