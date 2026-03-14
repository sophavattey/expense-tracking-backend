package com.example.finset.repository;

import com.example.finset.entity.Group;
import com.example.finset.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {

    Optional<Group> findByInviteCode(String inviteCode);

    /**
     * All groups the user belongs to, with ALL members eagerly loaded.
     * Uses a subquery so the JOIN FETCH does not filter the members collection
     * down to only the requesting user's own row.
     */
    @Query("""
        SELECT DISTINCT g FROM Group g
        JOIN FETCH g.members m
        JOIN FETCH m.user
        WHERE g IN (
            SELECT g2 FROM Group g2
            JOIN g2.members m2
            WHERE m2.user = :user
        )
        ORDER BY g.createdAt DESC
        """)
    List<Group> findAllByMember(@Param("user") User user);

    /** Check if a user is already a member of the group */
    @Query("""
        SELECT COUNT(m) > 0 FROM GroupMember m
        WHERE m.group.id = :groupId AND m.user = :user
        """)
    boolean isMember(@Param("groupId") UUID groupId, @Param("user") User user);

    /**
     * Load a group by id WITH all members eagerly fetched.
     * Use this instead of findById() whenever you need to call
     * group.getMembers() — avoids LazyInitializationException.
     */
    @Query("""
        SELECT g FROM Group g
        JOIN FETCH g.members m
        JOIN FETCH m.user
        WHERE g.id = :id
        """)
    Optional<Group> findByIdWithMembers(@Param("id") UUID id);
}