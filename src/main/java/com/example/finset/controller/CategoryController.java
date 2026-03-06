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

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<?> getAll(@AuthenticationPrincipal UserDetails principal) {
        Long userId = Long.parseLong(principal.getUsername());
        List<CategoryDto.Response> categories = categoryService.getAllForUser(userId);
        return ResponseEntity.ok(Map.of("success", true, "data", categories));
    }

    @PostMapping
    public ResponseEntity<?> create(
        @AuthenticationPrincipal UserDetails principal,
        @Valid @RequestBody CategoryDto.Request req
    ) {
        Long userId = Long.parseLong(principal.getUsername());
        CategoryDto.Response created = categoryService.create(userId, req);
        return ResponseEntity.status(201).body(Map.of(
            "success", true,
            "message", "Category created",
            "data", created
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable Long id,
        @Valid @RequestBody CategoryDto.Request req
    ) {
        Long userId = Long.parseLong(principal.getUsername());
        CategoryDto.Response updated = categoryService.update(userId, id, req);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Category updated",
            "data", updated
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable Long id
    ) {
        Long userId = Long.parseLong(principal.getUsername());
        categoryService.delete(userId, id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Category deleted"));
    }
}