package com.salarymanagement.auth;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads the JWT from the {@code access_token} HttpOnly cookie (name configured via
 * {@code app.security.jwt.cookie-name}) and, if the token is valid, populates the security
 * context so the request is treated as authenticated.
 *
 * <p>Cookie-only: this filter never reads the {@code Authorization} header. The JWT is issued
 * exclusively as an HttpOnly cookie by {@link AuthController}, so the cookie is the sole
 * supported transport - there is no header-based fallback mode.
 *
 * <p>A missing, malformed, or invalid/expired cookie simply leaves the request unauthenticated;
 * it is then rejected with 401 by {@link JwtAuthenticationEntryPoint} if it reaches a protected
 * endpoint. Both cases are handled identically - there is no separate error path here for
 * "missing" vs. "invalid" tokens.
 *
 * <p>Constructed as a {@code @Bean} method in {@link SecurityConfig} (not {@code @Component}-
 * scanned) - see that class's Javadoc for why.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final String cookieName;

    public JwtAuthenticationFilter(JwtService jwtService, String cookieName) {
        this.jwtService = jwtService;
        this.cookieName = cookieName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = readTokenFromCookie(request);

        if (token != null) {
            try {
                String username = jwtService.validateAndGetSubject(token);
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            username, null, List.of(new SimpleGrantedAuthority("ROLE_HR_MANAGER")));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException ex) {
                // Invalid/expired/malformed token: never log the token itself. Leaving the
                // security context empty causes the request to be rejected as unauthenticated.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String readTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
