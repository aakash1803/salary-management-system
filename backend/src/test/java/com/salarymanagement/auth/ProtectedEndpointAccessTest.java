package com.salarymanagement.auth;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end verification that the REAL Spring Security filter chain (not a mocked/bypassed
 * one, unlike the {@code @WebMvcTest} slice tests elsewhere in this project) protects the
 * employee, dashboard, and current-user endpoints using the {@code access_token} HttpOnly
 * cookie. {@code /api/employees}, {@code /api/employees/{id}}, and {@code /api/dashboard} are
 * used as the representative protected resources named in the requirements - all three rely on
 * the same {@code anyRequest().authenticated()} rule in {@code SecurityConfig}, so one
 * additional pair of tests for {@code /api/dashboard} is enough to confirm that rule also covers
 * the dashboard module, without repeating every case already covered for the employee endpoints
 * above. {@code /api/auth/me} is exercised here too (rather than in the {@code @WebMvcTest}
 * slice) because it specifically needs the real filter chain to establish the authenticated
 * principal from the cookie.
 *
 * <p>Authentication is cookie-only: every case below presents the JWT (or omits/corrupts it) as
 * the {@code access_token} cookie: never as an {@code Authorization} header, since the filter no
 * longer reads that header at all.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProtectedEndpointAccessTest {

    private static final String COOKIE_NAME = "access_token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Value("${app.security.jwt.secret}")
    private String jwtSecret;

    @Test
    void listEmployeesWithoutCookieIsRejected() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getEmployeeByIdWithoutCookieIsRejected() throws Exception {
        mockMvc.perform(get("/api/employees/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listEmployeesWithAnInvalidCookieIsRejected() throws Exception {
        mockMvc.perform(get("/api/employees").cookie(new Cookie(COOKIE_NAME, "not-a-real-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listEmployeesWithAnExpiredCookieIsRejected() throws Exception {
        // A JwtService configured with a negative expiration issues a token whose expiration
        // instant is already in the past - the same signing secret as the application's real
        // bean is used so only the expiration (not the signature) is what makes this token fail.
        JwtService expiredTokenIssuer = new JwtService(jwtSecret, -1);
        String expiredToken = expiredTokenIssuer.generateToken("hr.manager");

        mockMvc.perform(get("/api/employees").cookie(new Cookie(COOKIE_NAME, expiredToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listEmployeesWithAValidCookieIsAllowed() throws Exception {
        String token = jwtService.generateToken("hr.manager");

        mockMvc.perform(get("/api/employees").cookie(new Cookie(COOKIE_NAME, token)))
                .andExpect(status().isOk());
    }

    @Test
    void dashboardWithoutCookieIsRejected() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardWithAValidCookieIsAllowed() throws Exception {
        String token = jwtService.generateToken("hr.manager");

        mockMvc.perform(get("/api/dashboard").cookie(new Cookie(COOKIE_NAME, token)))
                .andExpect(status().isOk());
    }

    @Test
    void meWithoutCookieIsRejected() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithAnInvalidCookieIsRejected() throws Exception {
        mockMvc.perform(get("/api/auth/me").cookie(new Cookie(COOKIE_NAME, "not-a-real-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithAValidCookieReturnsTheAuthenticatedUsername() throws Exception {
        String token = jwtService.generateToken("hr.manager");

        mockMvc.perform(get("/api/auth/me").cookie(new Cookie(COOKIE_NAME, token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("hr.manager"));
    }
}
