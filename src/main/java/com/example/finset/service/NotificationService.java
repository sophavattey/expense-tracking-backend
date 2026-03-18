package com.example.finset.service;

import com.example.finset.dto.NotificationDto;
import com.example.finset.entity.*;
import com.example.finset.repository.NotificationRepository;
import com.example.finset.repository.UserRepository;
import com.example.finset.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notifRepository;
    private final UserRepository         userRepository;

    /* ─── Read ───────────────────────────────────────────────────── */

    @Transactional(readOnly = true)
    public NotificationDto.ListResponse getNotifications(UUID userId) {
        User user = getUser(userId);
        List<Notification> notifs = notifRepository.findTopByUserOrderByCreatedAtDesc(user);
        long unread = notifRepository.countByUserAndReadFalse(user);

        NotificationDto.ListResponse resp = new NotificationDto.ListResponse();
        resp.setNotifications(notifs.stream().map(this::toResponse).toList());
        resp.setUnreadCount(unread);
        return resp;
    }

    /* ─── Mark read ──────────────────────────────────────────────── */

    @Transactional
    public void markAllRead(UUID userId) {
        User user = getUser(userId);
        notifRepository.markAllReadByUser(user);
    }

    @Transactional
    public void markRead(UUID userId, UUID notifId) {
        User user = getUser(userId);
        notifRepository.markReadByIdAndUser(notifId, user);
    }

    /* ─── Create helpers (called from other services) ────────────── */

    /**
     * Budget warning (80%) or exceeded (100%+).
     * De-duplicates — won't create more than one of the same type per hour.
     */
    @Transactional
    public void notifyBudgetThreshold(User user, String categoryName,
                                       int percentage, BigDecimal remaining,
                                       boolean exceeded, String periodLabel,
                                       boolean isGroup, String groupName) {
        Notification.Type type = exceeded
            ? Notification.Type.BUDGET_EXCEEDED
            : Notification.Type.BUDGET_WARNING;

        // Deduplicate — skip if same type was sent in the last 1 hour
        LocalDateTime since = LocalDateTime.now().minusHours(1);
        if (notifRepository.existsRecentByUserAndType(user, type, since)) return;

        String context = isGroup ? " (" + groupName + ")" : "";
        String title   = exceeded
            ? categoryName + " budget exceeded" + context
            : categoryName + " budget at " + percentage + "%" + context;
        String body    = exceeded
            ? "You've gone over your " + periodLabel + " budget for " + categoryName + "."
            : "$" + remaining.setScale(2, java.math.RoundingMode.HALF_UP) + " remaining in your "
              + periodLabel + " budget.";

        save(user, type, title, body, "/dashboard/budgets");
    }

    /** Notifies all group members except the one who added the expense. */
    @Transactional
    public void notifyGroupExpenseAdded(Group group, User actor, String merchantOrCategory,
                                         BigDecimal amountUsd) {
        String actorName = actor.getName().split(" ")[0]; // first name only
        String title     = actorName + " added an expense";
        String body      = "$" + amountUsd.setScale(2, java.math.RoundingMode.HALF_UP)
                         + " · " + merchantOrCategory + " · " + group.getName();

        group.getMembers().stream()
            .map(GroupMember::getUser)
            .filter(u -> !u.getId().equals(actor.getId()))
            .forEach(u -> save(u, Notification.Type.GROUP_EXPENSE_ADDED, title, body,
                               "/dashboard/expenses"));
    }

    /** Notifies group owner when a new member joins. */
    @Transactional
    public void notifyMemberJoined(Group group, User newMember) {
        if (group.getOwner().getId().equals(newMember.getId())) return; // owner joining their own group
        String firstName = newMember.getName().split(" ")[0];
        save(group.getOwner(),
             Notification.Type.GROUP_MEMBER_JOINED,
             firstName + " joined " + group.getName(),
             firstName + " is now a member of your group.",
             "/dashboard/groups/" + group.getId());
    }

    /** Notifies group owner when a member leaves or is removed. */
    @Transactional
    public void notifyMemberLeft(Group group, User member, boolean wasRemoved) {
        if (group.getOwner().getId().equals(member.getId())) return;
        String firstName = member.getName().split(" ")[0];
        String title     = wasRemoved
            ? firstName + " was removed from " + group.getName()
            : firstName + " left " + group.getName();
        String body = wasRemoved
            ? firstName + " has been removed from your group."
            : firstName + " has left your group.";
        save(group.getOwner(),
             Notification.Type.GROUP_MEMBER_LEFT,
             title, body,
             "/dashboard/groups/" + group.getId());
    }

    /* ─── Internal ───────────────────────────────────────────────── */

    private void save(User user, Notification.Type type, String title, String body, String actionUrl) {
        try {
            Notification n = Notification.builder()
                .user(user).type(type)
                .title(title).body(body)
                .actionUrl(actionUrl)
                .read(false)
                .build();
            notifRepository.save(n);
        } catch (Exception e) {
            // Never let notification failure break the main transaction
            log.warn("Failed to save notification for user {}: {}", user.getId(), e.getMessage());
        }
    }

    private NotificationDto.Response toResponse(Notification n) {
        NotificationDto.Response r = new NotificationDto.Response();
        r.setId(n.getId());
        r.setType(n.getType());
        r.setTitle(n.getTitle());
        r.setBody(n.getBody());
        r.setActionUrl(n.getActionUrl());
        r.setRead(n.isRead());
        r.setCreatedAt(n.getCreatedAt());
        return r;
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}