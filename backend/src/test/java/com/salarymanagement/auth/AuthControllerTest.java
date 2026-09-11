package com.salarymanagement.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice tests for {@link AuthController}. Security filters are disabled here (as in
 * {@code EmployeeControllerTest}) since this class isolates the controller/DTO/cookie behavior;
 * the real security filter chain (JWT cookie validation, CORS, the 401 entry point) is
 * separately exercised end-to-end in {@link ProtectedEndpointAccessTest} and
 * {@link CorsConfigurationTest}.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void loginWithValidCredentialsReturnsUsernameAndExpiryButNoToken() throws Exception {
        when(authService.login("hr.manager", "correct-password"))
                .thenReturn(new AuthResult("a.jwt.token", "hr.manager", 3600L));

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"hr.manager\",\"password\":\"correct-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("hr.manager"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("hr.manager"));

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertTrue(setCookie != null && setCookie.contains("access_token=a.jwt.token"), "cookie should carry the token value");
        assertTrue(setCookie.contains("HttpOnly"), "cookie should be HttpOnly");
        assertTrue(setCookie.contains("Path=/"), "cookie should have Path=/");
        assertTrue(setCookie.contains("SameSite=Lax"), "cookie should have SameSite=Lax");
        assertTrue(setCookie.contains("Max-Age=3600"), "cookie Max-Age should match the JWT expiration");
    }

    @Test
    void loginWithInvalidCredentialsReturnsUnauthorizedAndSetsNoCookie() throws Exception {
        when(authService.login(anyString(), anyString()))
                .thenThrow(new BadCredentialsException("Invalid username or password."));

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"hr.manager\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertNull(result.getResponse().getHeader("Set-Cookie"), "no cookie should be issued on failed login");
    }

    @Test
    void loginWithBlankUsernameIsRejectedAsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"something\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logoutReturnsOkAndClearsTheCookieWithoutReturningAToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals("", result.getResponse().getContentAsString());

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertTrue(setCookie != null && setCookie.startsWith("access_token="), "logout should clear the access_token cookie");
        assertTrue(setCookie.contains("Max-Age=0"), "logout cookie should expire immediately");
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("Path=/"));
    }
}
