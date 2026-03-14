package com.example.finset.repository;

import com.example.finset.entity.Expense;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ExpenseSpec {

    /**
     * Personal expenses: user_id = userId AND group IS NULL
     */
    public static Specification<Expense> filter(
        UUID userId,
        UUID categoryId,
        LocalDate from,
        LocalDate to,
        Expense.Currency currency
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));
            predicates.add(cb.isNull(root.get("group")));          // ← personal only
            if (categoryId != null) predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            if (from       != null) predicates.add(cb.greaterThanOrEqualTo(root.get("date"), from));
            if (to         != null) predicates.add(cb.lessThanOrEqualTo(root.get("date"), to));
            if (currency   != null) predicates.add(cb.equal(root.get("currency"), currency));
            query.orderBy(cb.desc(root.get("date")), cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Group expenses: group_id = groupId (ignores which user created it)
     */
    public static Specification<Expense> filterByGroupId(
        UUID groupId,
        UUID categoryId,
        LocalDate from,
        LocalDate to,
        Expense.Currency currency
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("group").get("id"), groupId)); // ← group only
            if (categoryId != null) predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            if (from       != null) predicates.add(cb.greaterThanOrEqualTo(root.get("date"), from));
            if (to         != null) predicates.add(cb.lessThanOrEqualTo(root.get("date"), to));
            if (currency   != null) predicates.add(cb.equal(root.get("currency"), currency));
            query.orderBy(cb.desc(root.get("date")), cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}