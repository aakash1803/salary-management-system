package com.salarymanagement.employee;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Composable, dynamic query filters for {@link Employee}, built with the
 * Spring Data JPA Specification API. Each factory method returns
 * {@link Specification#unrestricted()} when its filter is not provided -
 * a no-op specification that is elided when combined with others - and
 * {@link #filterBy} combines only the filters that are actually supplied.
 *
 * <p>{@code Specification.where(...)} no longer accepts {@code null}, and
 * {@code and(...)}/{@code or(...)} no longer accept a {@code null} argument
 * either (both throw {@link IllegalArgumentException} as of this project's
 * Spring Data JPA version), so {@code unrestricted()} is used as the
 * identity element instead of {@code null}.
 */
public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    public static Specification<Employee> search(String search) {
        if (!StringUtils.hasText(search)) {
            return Specification.unrestricted();
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("firstName")), pattern),
                cb.like(cb.lower(root.get("lastName")), pattern),
                cb.like(cb.lower(root.get("employeeNumber")), pattern)
        );
    }

    public static Specification<Employee> hasCountry(String country) {
        if (!StringUtils.hasText(country)) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("country"), country);
    }

    public static Specification<Employee> hasDepartment(String department) {
        if (!StringUtils.hasText(department)) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("department"), department);
    }

    /**
     * Combines the search/country/department filters, applying only the
     * ones that are provided. A {@code null} or blank argument contributes
     * no restriction to the resulting query rather than being matched
     * literally.
     */
    public static Specification<Employee> filterBy(String search, String country, String department) {
        return search(search)
                .and(hasCountry(country))
                .and(hasDepartment(department));
    }
}
