package com.example.finset.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /** Original amount the user entered */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** Original currency (USD or KHR) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency = Currency.USD;

    /**
     * Amount normalised to USD for totals, budgets, and charts.
     * USD expenses  → same as amount
     * KHR expenses  → amount / 4000  (fixed Cambodian rate)
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amountBase;

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 150)
    private String merchantName;

    @Column(length = 500)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Currency      { USD, KHR }
    public enum PaymentMethod { CASH, CARD, KHQR, BANK, APP, OTHER }
}