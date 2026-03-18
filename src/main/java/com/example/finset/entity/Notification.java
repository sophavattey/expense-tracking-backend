package com.example.finset.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notif_user_read", columnList = "user_id, read"),
    @Index(name = "idx_notif_user_created", columnList = "user_id, created_at DESC")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Type type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String body;

    /** Optional — deep-link target on click (e.g. "/dashboard/budgets") */
    @Column(length = 255)
    private String actionUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean read = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum Type {
        BUDGET_WARNING,       // 80% of budget reached
        BUDGET_EXCEEDED,      // 100%+ of budget
        GROUP_EXPENSE_ADDED,  // member added expense to group
        GROUP_MEMBER_JOINED,  // someone joined the group (owner notified)
        GROUP_MEMBER_LEFT,    // member left or was removed (owner notified)
    }
}
