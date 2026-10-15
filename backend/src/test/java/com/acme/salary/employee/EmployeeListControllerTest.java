package com.acme.salary.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmployeeController.class)
class EmployeeListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService service;

    @Test
    void returnsAPageOfEmployees() throws Exception {
        when(service.search(any(EmployeeFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(savedEmployee(1L)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].fullName").value("Asha Patel"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void passesTheFiltersToTheService() throws Exception {
        returnAnEmptyPage();

        mockMvc.perform(get("/api/employees")
                        .param("search", "asha")
                        .param("country", "India")
                        .param("department", "Engineering")
                        .param("jobTitle", "Software Engineer"))
                .andExpect(status().isOk());

        ArgumentCaptor<EmployeeFilter> filter = ArgumentCaptor.forClass(EmployeeFilter.class);
        verify(service).search(filter.capture(), any(Pageable.class));
        assertThat(filter.getValue()).isEqualTo(
                new EmployeeFilter("asha", "India", "Engineering", "Software Engineer"));
    }

    @Test
    void usesTheRequestedPageAndSize() throws Exception {
        returnAnEmptyPage();

        mockMvc.perform(get("/api/employees").param("page", "3").param("size", "50"))
                .andExpect(status().isOk());

        Pageable pageable = capturedPageable();
        assertThat(pageable.getPageNumber()).isEqualTo(3);
        assertThat(pageable.getPageSize()).isEqualTo(50);
    }

    @Test
    void capsThePageSize() throws Exception {
        returnAnEmptyPage();

        mockMvc.perform(get("/api/employees").param("size", "1000"))
                .andExpect(status().isOk());

        assertThat(capturedPageable().getPageSize()).isEqualTo(EmployeeController.MAX_PAGE_SIZE);
    }

    @Test
    void sortsByTheRequestedField() throws Exception {
        returnAnEmptyPage();

        mockMvc.perform(get("/api/employees").param("sortBy", "salary").param("direction", "desc"))
                .andExpect(status().isOk());

        Sort.Order order = capturedPageable().getSort().getOrderFor("salary");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void rejectsAnUnknownSortField() throws Exception {
        mockMvc.perform(get("/api/employees").param("sortBy", "password"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsANegativePage() throws Exception {
        mockMvc.perform(get("/api/employees").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void breaksTiesByNameThenIdWhenSortingByAnotherField() throws Exception {
        returnAnEmptyPage();

        mockMvc.perform(get("/api/employees").param("sortBy", "salary").param("direction", "desc"))
                .andExpect(status().isOk());

        assertThat(sortOrder()).containsExactly("salary: DESC", "fullName: ASC", "id: ASC");
    }

    @Test
    void breaksTiesByIdWhenSortingByName() throws Exception {
        returnAnEmptyPage();

        mockMvc.perform(get("/api/employees").param("sortBy", "fullName").param("direction", "desc"))
                .andExpect(status().isOk());

        assertThat(sortOrder()).containsExactly("fullName: DESC", "id: ASC");
    }

    @Test
    void addsTheRequestedSecondarySort() throws Exception {
        returnAnEmptyPage();

        mockMvc.perform(get("/api/employees")
                        .param("sortBy", "country")
                        .param("secondarySortBy", "department")
                        .param("secondaryDirection", "desc"))
                .andExpect(status().isOk());

        assertThat(sortOrder()).containsExactly("country: ASC", "department: DESC", "fullName: ASC", "id: ASC");
    }

    @Test
    void doesNotRepeatTheNameWhenItIsTheSecondarySort() throws Exception {
        returnAnEmptyPage();

        mockMvc.perform(get("/api/employees")
                        .param("sortBy", "country")
                        .param("secondarySortBy", "fullName")
                        .param("secondaryDirection", "desc"))
                .andExpect(status().isOk());

        assertThat(sortOrder()).containsExactly("country: ASC", "fullName: DESC", "id: ASC");
    }

    @Test
    void ignoresASecondarySortThatMatchesTheMainSort() throws Exception {
        returnAnEmptyPage();

        mockMvc.perform(get("/api/employees")
                        .param("sortBy", "salary")
                        .param("direction", "desc")
                        .param("secondarySortBy", "salary"))
                .andExpect(status().isOk());

        assertThat(sortOrder()).containsExactly("salary: DESC", "fullName: ASC", "id: ASC");
    }

    @Test
    void ignoresABlankSecondarySort() throws Exception {
        returnAnEmptyPage();

        mockMvc.perform(get("/api/employees").param("sortBy", "salary").param("secondarySortBy", ""))
                .andExpect(status().isOk());

        assertThat(sortOrder()).containsExactly("salary: ASC", "fullName: ASC", "id: ASC");
    }

    @Test
    void rejectsAnUnknownSecondarySortField() throws Exception {
        mockMvc.perform(get("/api/employees").param("secondarySortBy", "password"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAnInvalidSecondarySortDirection() throws Exception {
        mockMvc.perform(get("/api/employees")
                        .param("secondarySortBy", "department")
                        .param("secondaryDirection", "sideways"))
                .andExpect(status().isBadRequest());
    }

    private void returnAnEmptyPage() {
        when(service.search(any(EmployeeFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
    }

    private Pageable capturedPageable() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(service).search(any(EmployeeFilter.class), pageable.capture());
        return pageable.getValue();
    }

    private List<String> sortOrder() {
        return capturedPageable().getSort().stream().map(Sort.Order::toString).toList();
    }

    private Employee savedEmployee(Long id) {
        Employee employee = new Employee(
                "Asha Patel", "asha.patel@acme.com", "Software Engineer", "Engineering",
                "India", new BigDecimal("1800000"), "INR", LocalDate.of(2022, 4, 1));
        ReflectionTestUtils.setField(employee, "id", id);
        return employee;
    }
}
