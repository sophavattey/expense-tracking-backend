package com.example.finset.controller;

import com.example.finset.dto.CategoryDto;
import com.example.finset.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<?> getAll(@AuthenticationPrincipal UserDetails principal) {
        UUID userId = UUID.fromString(principal.getUsername());               // ← UUID
        List<CategoryDto.Response> categories = categoryService.getAllForUser(userId);
        return ResponseEntity.ok(Map.of("success", true, "data", categories));
    }

    @PostMapping
    public ResponseEntity<?> create(
        @AuthenticationPrincipal UserDetails principal,
        @Valid @RequestBody CategoryDto.Request req
    ) {
        UUID userId = UUID.fromString(principal.getUsername());               // ← UUID
        return ResponseEntity.status(201).body(Map.of(
            "success", true,
            "message", "Category created",
            "data",    categoryService.create(userId, req)
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID id,                                                // ← UUID
        @Valid @RequestBody CategoryDto.Request req
    ) {
        UUID userId = UUID.fromString(principal.getUsername());               // ← UUID
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Category updated",
            "data",    categoryService.update(userId, id, req)
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID id                                                 // ← UUID
    ) {
        UUID userId = UUID.fromString(principal.getUsername());               // ← UUID
        categoryService.delete(userId, id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Category deleted"));
    }
}