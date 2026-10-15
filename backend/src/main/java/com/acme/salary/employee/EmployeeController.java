package com.acme.salary.employee;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * The HTTP front door for employees: list/search, create, read, update and delete.
 * It stays thin on purpose. It checks request parameters and shapes the response,
 * and leaves the real rules (duplicate emails, not-found) to {@link EmployeeService}.
 */
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    // Cap the page size so nobody can ask for all 10,000 rows in one request.
    static final int MAX_PAGE_SIZE = 100;
    private static final String NAME_FIELD = "fullName";
    // Whitelist of sortable fields. Never pass a raw "sortBy" value to the database layer.
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("fullName", "email", "jobTitle", "department", "country", "salary", "joiningDate");

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    // GET /api/employees. All filters are optional and combine with AND.
    // Sorting has a primary level (sortBy/direction) and an optional secondary level.
    @GetMapping
    public PageResponse<EmployeeResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String jobTitle,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fullName") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String secondarySortBy,
            @RequestParam(defaultValue = "asc") String secondaryDirection) {

        EmployeeFilter filter = new EmployeeFilter(search, country, department, jobTitle);
        Page<Employee> result = service.search(
                filter, pageRequest(page, size, sortBy, direction, secondarySortBy, secondaryDirection));
        return PageResponse.from(result.map(EmployeeResponse::from));
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        Employee created = service.create(request.toDetails());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(EmployeeResponse.from(created));
    }

    @GetMapping("/{id}")
    public EmployeeResponse getById(@PathVariable Long id) {
        return EmployeeResponse.from(service.getById(id));
    }

    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return EmployeeResponse.from(service.update(id, request.toDetails()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Turns the raw query parameters into a validated Pageable.
    // Bad input throws IllegalArgumentException, which the error handler maps to a 400.
    private Pageable pageRequest(int page, int size, String sortBy, String direction,
                                 String secondarySortBy, String secondaryDirection) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must not be negative");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Size must be at least 1");
        }
        requireSortable(sortBy);

        Sort sort = Sort.by(parseDirection(direction), sortBy);

        // An optional second sort level, for example "by country, then by salary high to low".
        boolean hasSecondarySort = secondarySortBy != null
                && !secondarySortBy.isBlank()
                && !secondarySortBy.equals(sortBy);
        if (hasSecondarySort) {
            requireSortable(secondarySortBy);
            sort = sort.and(Sort.by(parseDirection(secondaryDirection), secondarySortBy));
        }

        // Without a final tie-breaker, the database may return equal rows in a different order
        // on each call, so a row could show up on two pages or none while paging.
        // Ties are broken by name and then id, so rows with equal values always come back in the same order.
        boolean nameAlreadyUsed = NAME_FIELD.equals(sortBy)
                || (hasSecondarySort && NAME_FIELD.equals(secondarySortBy));
        if (!nameAlreadyUsed) {
            sort = sort.and(Sort.by(Sort.Direction.ASC, NAME_FIELD));
        }
        sort = sort.and(Sort.by(Sort.Direction.ASC, "id"));

        return PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), sort);
    }

    private void requireSortable(String field) {
        if (!SORTABLE_FIELDS.contains(field)) {
            throw new IllegalArgumentException("Cannot sort by " + field);
        }
    }

    private Sort.Direction parseDirection(String direction) {
        return Sort.Direction.fromOptionalString(direction)
                .orElseThrow(() -> new IllegalArgumentException("Direction must be asc or desc"));
    }
}
