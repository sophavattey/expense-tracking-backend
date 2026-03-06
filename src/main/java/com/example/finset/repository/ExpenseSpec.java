package com.example.finset.repository;

import com.example.finset.entity.Expense;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ExpenseSpec {

    /**
     * Pure predicate-only Specification — no fetch joins here.
     * Category loading is handled by @EntityGraph on the repository method,
     * which Hibernate applies only to the data query, never the count query.
     */
    public static Specification<Expense> filter(
            Long userId,
            Long categoryId,
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