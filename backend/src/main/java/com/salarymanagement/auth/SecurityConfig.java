package com.salarymanagement.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Stateless, cookie-based JWT security configuration - and the sole place that constructs the
 * JWT infrastructure beans ({@link JwtService}, {@link JwtAuthenticationFilter},
 * {@link JwtAuthenticationEntryPoint}) via {@code @Bean} methods, rather than relying on
 * {@code @Component} scanning to discover them individually.
 *
 * <p>This matters specifically for {@code @WebMvcTest} slices: Spring Boot's web MVC test slice
 * always includes a user-defined {@code @Configuration} class that supplies a
 * {@link SecurityFilterChain} bean (so security rules can be exercised even in a narrow slice),
 * but it does not independently include arbitrary {@code @Component}-annotated collaborator
 * classes such as a plain {@code JwtService}. A prior version of this class relied on
 * {@code @Component} scanning for those collaborators, which worked in the full application and
 * in {@code @SpringBootTest}-based tests, but failed with
 * {@code NoSuchBeanDefinitionException: ... JwtService} under {@code @WebMvcTest} once real test
 * runs exercised it - confirmed against {@code EmployeeControllerTest} and
 * {@code AuthControllerTest}. Declaring these as {@code @Bean} methods here means they are
 * always registered together with this class, in every context: the full application, a
 * {@code @WebMvcTest} slice, or the {@code @SpringBootTest(webEnvironment = NONE)} persistence
 * tests elsewhere in this project.
 *
 * <p>{@link #passwordEncoder()}, {@link #jwtService(String, long)},
 * {@link #jwtAuthenticationFilter(JwtService, String)},
 * {@link #authenticationEntryPoint(ObjectMapper)}, and {@link #corsConfigurationSource(String)}
 * are unconditional - {@link AuthService} needs the first two regardless of context type, and the
 * others have no consumer besides {@link #securityFilterChain}. Only {@link #securityFilterChain}
 * itself is guarded with {@link ConditionalOnWebApplication}, since it is the one bean here that
 * depends on {@link HttpSecurity}, which is only available in a servlet web application context -
 * this project's persistence tests use {@code @SpringBootTest(webEnvironment = WebEnvironment.NONE)}
 * (a plain, non-web context), and without this guard, context startup for those tests would fail
 * trying to resolve {@code HttpSecurity}.
 *
 * <p>There is a single primary user role (HR Manager), so authorization here is simply
 * "authenticated or not" - no role/permission hierarchy is introduced.
 *
 * <p><b>CSRF</b> remains disabled - see docs/security-configuration.md for the reasoning (JWT
 * lives in a {@code SameSite=Lax} HttpOnly cookie, CORS is restricted to a single configured
 * trusted origin, and there are no authenticated state-changing GET endpoints). This is a
 * deliberate decision for this application's current scope, not an oversight, and should be
 * revisited if the frontend origin trust model changes.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtService jwtService(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-minutes:60}") long expirationMinutes) {
        return new JwtService(secret, expirationMinutes);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService,
            @Value("${app.security.jwt.cookie-name}") String cookieName) {
        return new JwtAuthenticationFilter(jwtService, cookieName);
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return new JwtAuthenticationEntryPoint(objectMapper);
    }

    /**
     * Restricts cross-origin requests to the single configured, trusted Angular origin.
     * {@code allowCredentials(true)} is required for the browser to send/accept the HttpOnly
     * auth cookie cross-port (localhost:4200 -> localhost:8080 in development); Spring rejects
     * combining {@code allowCredentials(true)} with a wildcard origin, so an explicit
     * single-origin list is the only configuration that works here anyway.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.security.cors.allowed-origin}") String allowedOrigin) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigin));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "Accept"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AuthenticationEntryPoint authenticationEntryPoint,
            CorsConfigurationSource corsConfigurationSource) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Preflight requests must be allowed through regardless of the target
                        // endpoint's own auth requirement, or the browser's OPTIONS request to a
                        // protected endpoint would be rejected with 401 before CORS is even
                        // relevant to the actual follow-up request.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/logout").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
