package com.salarymanagement.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end verification that the REAL Spring Security filter chain (not a mocked/bypassed
 * one, unlike the {@code @WebMvcTest} slice tests elsewhere in this project) protects the
 * employee and dashboard endpoints. {@code /api/employees}, {@code /api/employees/{id}}, and
 * {@code /api/dashboard} are used as the representative protected resources named in the
 * requirements - all three rely on the same {@code anyRequest().authenticated()} rule in
 * {@code SecurityConfig}, so one additional pair of tests for {@code /api/dashboard} is enough to
 * confirm that rule also covers the dashboard module, without repeating every case already
 * covered for the employee endpoints above.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProtectedEndpointAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void listEmployeesWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getEmployeeByIdWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/employees/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listEmployeesWithAnInvalidTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/employees").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listEmployeesWithAValidTokenIsAllowed() throws Exception {
        String token = jwtService.generateToken("hr.manager");

        mockMvc.perform(get("/api/employees").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void dashboardWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardWithAValidTokenIsAllowed() throws Exception {
        String token = jwtService.generateToken("hr.manager");

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
