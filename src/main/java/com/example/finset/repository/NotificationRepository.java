package com.example.finset.repository;

import com.example.finset.entity.Notification;
import com.example.finset.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /** Most recent 50 notifications for a user */
    @Query("""
        SELECT n FROM Notification n
        WHERE n.user = :user
        ORDER BY n.createdAt DESC
        LIMIT 50
        """)
    List<Notification> findTopByUserOrderByCreatedAtDesc(@Param("user") User user);

    /** Unread count badge */
    long countByUserAndReadFalse(User user);

    /** Mark all unread as read */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user = :user AND n.read = false")
    void markAllReadByUser(@Param("user") User user);

    /** Mark single notification as read */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.id = :id AND n.user = :user")
    void markReadByIdAndUser(@Param("id") UUID id, @Param("user") User user);

    /** Cleanup old notifications (keep last 90 days) */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user = :user AND n.createdAt < :before")
    void deleteOlderThan(@Param("user") User user, @Param("before") LocalDateTime before);

    /** Prevent duplicate budget notifications in same period */
    @Query("""
        SELECT COUNT(n) > 0 FROM Notification n
        WHERE n.user = :user
          AND n.type = :type
          AND n.createdAt >= :since
        """)
    boolean existsRecentByUserAndType(
        @Param("user")  User user,
        @Param("type")  Notification.Type type,
        @Param("since") LocalDateTime since
    );
}