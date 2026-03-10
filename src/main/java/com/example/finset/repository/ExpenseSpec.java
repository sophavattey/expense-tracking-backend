package com.example.finset.repository;

import com.example.finset.entity.Expense;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ExpenseSpec {

    public static Specification<Expense> filter(
            UUID userId,                      // ← UUID
            UUID categoryId,                  // ← UUID
            LocalDate from,
            LocalDate to,
            Expense.Currency currency
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("user").get("id"), userId));

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), to));
            }
            if (currency != null) {
                predicates.add(cb.equal(root.get("currency"), currency));
            }

            query.orderBy(
                cb.desc(root.get("date")),
                cb.desc(root.get("createdAt"))
            );

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}