package com.example.finset.repository;

import com.example.finset.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // All categories visible to a user = system defaults + their own customs
    @Query("""
        SELECT c FROM Category c
        WHERE c.user IS NULL
           OR c.user.id = :userId
        ORDER BY c.isDefault DESC, c.name ASC
        """)
    List<Category> findAllVisibleToUser(@Param("userId") Long userId);

    // Only user's custom categories
    List<Category> findByUserIdOrderByNameAsc(Long userId);

    // Check duplicate name for a user's custom category
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    // Find a specific category that belongs to a user or is a system default
    @Query("""
        SELECT c FROM Category c
        WHERE c.id = :id
          AND (c.user IS NULL OR c.user.id = :userId)
        """)
    Optional<Category> findByIdAndVisibleToUser(@Param("id") Long id, @Param("userId") Long userId);
}