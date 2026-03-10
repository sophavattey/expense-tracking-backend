package com.example.finset.repository;

import com.example.finset.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {   // ← UUID

    @Query("""
        SELECT c FROM Category c
        WHERE c.user IS NULL
           OR c.user.id = :userId
        ORDER BY c.isDefault DESC, c.name ASC
        """)
    List<Category> findAllVisibleToUser(@Param("userId") UUID userId);        // ← UUID

    List<Category> findByUserIdOrderByNameAsc(UUID userId);                   // ← UUID

    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);        // ← UUID

    @Query("""
        SELECT c FROM Category c
        WHERE c.id = :id
          AND (c.user IS NULL OR c.user.id = :userId)
        """)
    Optional<Category> findByIdAndVisibleToUser(
        @Param("id")     UUID id,                                             // ← UUID
        @Param("userId") UUID userId                                          // ← UUID
    );
}