package com.example.finset.config;

import com.example.finset.entity.Category;
import com.example.finset.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    private static final List<Category> DEFAULT_CATEGORIES = List.of(
        Category.builder().name("Food & Drink")    .icon("🍜").color("#2563eb").isDefault(true).build(),
        Category.builder().name("Groceries")        .icon("🛒").color("#22c55e").isDefault(true).build(),
        Category.builder().name("Transport")         .icon("🏍️").color("#f59e0b").isDefault(true).build(),
        Category.builder().name("Shopping")          .icon("🛍️").color("#60a5fa").isDefault(true).build(),
        Category.builder().name("Bills & Utilities") .icon("⚡").color("#f87171").isDefault(true).build(),
        Category.builder().name("Entertainment")     .icon("🎮").color("#a78bfa").isDefault(true).build(),
        Category.builder().name("Health")            .icon("💊").color("#34d399").isDefault(true).build(),
        Category.builder().name("Education")         .icon("📚").color("#fb923c").isDefault(true).build(),
        Category.builder().name("Travel")            .icon("✈️").color("#38bdf8").isDefault(true).build(),
        Category.builder().name("Others")            .icon("📦").color("#94a3b8").isDefault(true).build()
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long count = categoryRepository.count();
        if (count == 0) {
            categoryRepository.saveAll(DEFAULT_CATEGORIES);
            log.info(" Seeded {} default categories", DEFAULT_CATEGORIES.size());
        } else {
            log.info("⏭  Categories already seeded ({} found), skipping", count);
        }
    }
}