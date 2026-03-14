package com.example.finset.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "budgets")
// NOTE: Uniqueness is enforced by two partial DB indexes (not a JPA constraint)
// so that personal and group budgets can share the same category+period:
//   uk_budget_personal_category_period: (user_id, category_id, period) WHERE group_id IS NULL
//   uk_budget_group_category_period:    (group_id, category_id, period) WHERE group_id IS NOT NULL
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /**
     * null  → personal budget (owned by this user only)
     * set   → shared group budget (tracks spending of all group members)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Period period;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal limitUsd;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Period { DAILY, WEEKLY, MONTHLY }
}