package com.acme.salary.employee;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    private static final String VALID_REQUEST = """
            {
              "fullName": "Asha Patel",
              "email": "asha.patel@acme.com",
              "jobTitle": "Software Engineer",
              "department": "Engineering",
              "country": "India",
              "salary": 1800000,
              "currency": "INR",
              "joiningDate": "2022-04-01"
            }
            """;

    private static final String INVALID_REQUEST = """
            {
              "fullName": "",
              "email": "not-an-email",
              "jobTitle": "Software Engineer",
              "department": "Engineering",
              "country": "India",
              "salary": -5,
              "currency": "INR",
              "joiningDate": "2022-04-01"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService service;

    @Test
    void createsAnEmployee() throws Exception {
        when(service.create(any(EmployeeDetails.class))).thenReturn(savedEmployee(1L));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/employees/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fullName").value("Asha Patel"))
                .andExpect(jsonPath("$.country").value("India"));
    }

    @Test
    void rejectsInvalidInputWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_REQUEST))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fullName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.salary").exists());
    }

    @Test
    void reportsAConflictWhenTheEmailIsTaken() throws Exception {
        when(service.create(any(EmployeeDetails.class)))
                .thenThrow(new DuplicateEmailException("asha.patel@acme.com"));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("An employee with email asha.patel@acme.com already exists"));
    }

    @Test
    void returnsAnEmployeeById() throws Exception {
        when(service.getById(1L)).thenReturn(savedEmployee(1L));

        mockMvc.perform(get("/api/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("asha.patel@acme.com"));
    }

    @Test
    void returnsNotFoundForAMissingEmployee() throws Exception {
        when(service.getById(99L)).thenThrow(new EmployeeNotFoundException(99L));

        mockMvc.perform(get("/api/employees/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found with id 99"));
    }

    @Test
    void updatesAnEmployee() throws Exception {
        when(service.update(eq(1L), any(EmployeeDetails.class))).thenReturn(savedEmployee(1L));

        mockMvc.perform(put("/api/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deletesAnEmployee() throws Exception {
        mockMvc.perform(delete("/api/employees/1"))
                .andExpect(status().isNoContent());

        verify(service).delete(1L);
    }

    @Test
    void returnsNotFoundWhenDeletingAMissingEmployee() throws Exception {
        doThrow(new EmployeeNotFoundException(99L)).when(service).delete(99L);

        mockMvc.perform(delete("/api/employees/99"))
                .andExpect(status().isNotFound());
    }

    private Employee savedEmployee(Long id) {
        Employee employee = new Employee(
                "Asha Patel", "asha.patel@acme.com", "Software Engineer", "Engineering",
                "India", new BigDecimal("1800000"), "INR", LocalDate.of(2022, 4, 1));
        ReflectionTestUtils.setField(employee, "id", id);
        return employee;
    }
}
