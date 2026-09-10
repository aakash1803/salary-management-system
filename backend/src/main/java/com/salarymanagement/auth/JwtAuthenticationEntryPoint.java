package com.salarymanagement.auth;

import com.salarymanagement.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Writes a 401 response using the same {@link ErrorResponse} JSON shape as the rest of the API
 * (see {@code com.salarymanagement.common.GlobalExceptionHandler}) whenever an unauthenticated
 * request reaches a protected endpoint. Covers both "no token supplied" and "token was rejected
 * by JwtAuthenticationFilter" (invalid/expired/malformed), since both simply leave the security
 * context empty and arrive here the same way. Never includes a stack trace or any internal
 * detail in the body.
 *
 * <p>Constructed as a {@code @Bean} method in {@link SecurityConfig} (not {@code @Component}-
 * scanned) - see that class's Javadoc for why.
 */
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Authentication is required to access this resource.",
                request.getRequestURI());

        objectMapper.writeValue(response.getWriter(), body);
    }
}
