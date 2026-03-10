package com.example.finset.service;

import com.example.finset.dto.CategoryDto;
import com.example.finset.entity.Category;
import com.example.finset.entity.User;
import com.example.finset.exception.ResourceNotFoundException;
import com.example.finset.repository.CategoryRepository;
import com.example.finset.repository.ExpenseRepository;
import com.example.finset.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository     userRepository;
    private final ExpenseRepository  expenseRepository;

    public List<CategoryDto.Response> getAllForUser(UUID userId) {             // ← UUID
        return categoryRepository.findAllVisibleToUser(userId)
            .stream()
            .map(c -> toResponse(c, userId))
            .toList();
    }

    @Transactional
    public CategoryDto.Response create(UUID userId, CategoryDto.Request req) { // ← UUID
        if (categoryRepository.existsByUserIdAndNameIgnoreCase(userId, req.getName())) {
            throw new IllegalArgumentException(
                "You already have a category named '" + req.getName() + "'");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Category category = Category.builder()
            .user(user)
            .name(req.getName().trim())
            .icon(req.getIcon())
            .color(req.getColor() != null ? req.getColor() : "#2563eb")
            .isDefault(false)
            .build();

        return toResponse(categoryRepository.save(category), userId);
    }

    @Transactional
    public CategoryDto.Response update(UUID userId, UUID categoryId, CategoryDto.Request req) { // ← UUID
        Category category = getOwnedCategory(userId, categoryId);

        if (!category.getName().equalsIgnoreCase(req.getName()) &&
            categoryRepository.existsByUserIdAndNameIgnoreCase(userId, req.getName())) {
            throw new IllegalArgumentException(
                "You already have a category named '" + req.getName() + "'");
        }

        category.setName(req.getName().trim());
        if (req.getIcon()  != null) category.setIcon(req.getIcon());
        if (req.getColor() != null) category.setColor(req.getColor());

        return toResponse(categoryRepository.save(category), userId);
    }

    @Transactional
    public void delete(UUID userId, UUID categoryId) {                         // ← UUID
        Category category = getOwnedCategory(userId, categoryId);

        long count = expenseRepository.countByCategoryId(categoryId);
        if (count > 0) {
            throw new IllegalStateException(
                "This category has " + count + " expense" + (count == 1 ? "" : "s") +
                ". Move or delete them before removing this category.");
        }

        categoryRepository.delete(category);
    }

    private Category getOwnedCategory(UUID userId, UUID categoryId) {          // ← UUID
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (category.isDefault()) {
            throw new IllegalArgumentException("System default categories cannot be modified");
        }
        if (category.getUser() == null || !category.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Category not found");
        }
        return category;
    }

    public CategoryDto.Response toResponse(Category c, UUID currentUserId) {  // ← UUID
        CategoryDto.Response r = new CategoryDto.Response();
        r.setId(c.getId());
        r.setName(c.getName());
        r.setIcon(c.getIcon());
        r.setColor(c.getColor());
        r.setDefault(c.isDefault());
        r.setOwned(c.getUser() != null && c.getUser().getId().equals(currentUserId));
        return r;
    }
}