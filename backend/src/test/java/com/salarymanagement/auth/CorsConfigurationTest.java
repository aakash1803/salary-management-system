package com.salarymanagement.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end verification of the CORS configuration in {@link SecurityConfig}: only the
 * configured, trusted Angular origin ({@code app.security.cors.allowed-origin}) may make
 * credentialed cross-origin requests - never a wildcard.
 *
 * <p>Uses a protected endpoint ({@code /api/employees}) deliberately, to also confirm that the
 * CORS preflight itself is not blocked by the endpoint's own authentication requirement (see the
 * explicit {@code OPTIONS} permitAll rule in {@code SecurityConfig}).
 */
@SpringBootTest
@AutoConfigureMockMvc
class CorsConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void preflightFromTheConfiguredAngularOriginIsAllowedWithCredentials() throws Exception {
        mockMvc.perform(options("/api/employees")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void preflightFromAnUntrustedOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/employees")
                        .header("Origin", "http://untrusted.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
