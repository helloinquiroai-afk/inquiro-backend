package com.inquiro.config;

import com.inquiro.auth.AuthService;
import com.inquiro.auth.BearerTokenAuthenticationFilter;
import com.inquiro.communication.messenger.MetaSignatureValidator;
import com.inquiro.security.RateLimitFilter;
import com.inquiro.security.RateLimitProperties;
import com.inquiro.security.RateLimitService;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthService authService,
            MetaSignatureValidator signatureValidator,
            @Value("${inquiro.management-api-key:}") String managementKey,
            RateLimitService rateLimitService,
            RateLimitProperties rateLimitProperties,
            @Value("${inquiro.security.allowed-origins:}") String allowedOrigins)
            throws Exception {

        ManagementAccessFilter managementAccessFilter =
                new ManagementAccessFilter(managementKey, signatureValidator);
        BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter =
                new BearerTokenAuthenticationFilter(authService);

        http
                .csrf(csrf -> csrf.disable())
                 .cors(cors -> cors.configurationSource(corsConfigurationSource(allowedOrigins)))
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .contentTypeOptions(content -> {})
                        // Referrer policy is configured at the ingress/reverse-proxy layer.
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(false)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/public/**", "/api/conversations/**", "/api/chat", "/webhook", "/messenger/webhook",
                                "/whatsapp/webhook").permitAll()
                        .requestMatchers("/api/auth/logout").authenticated()
                        .requestMatchers("/api/test/**", "/h2-console/**").hasRole("OPERATOR")
                        .requestMatchers("/api/business/**", "/api/knowledge/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> writeError(response, 401,
                                "Authentication required"))
                        .accessDeniedHandler((request, response, exception) -> writeError(response, 403,
                                "Access denied")))
                .addFilterBefore(managementAccessFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(bearerTokenAuthenticationFilter, ManagementAccessFilter.class)
                .addFilterAfter(new RateLimitFilter(rateLimitService, rateLimitProperties), BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${inquiro.security.allowed-origins:}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = allowedOrigins.isBlank()
                ? List.of()
                : java.util.Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(origin -> !origin.isBlank())
                    .toList();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Inquiro-Management-Key"));
        config.setExposedHeaders(List.of());
        config.setAllowCredentials(false);
        CorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        ((UrlBasedCorsConfigurationSource) source).registerCorsConfiguration("/**", config);

        CorsConfiguration publicConfig = new CorsConfiguration();
        publicConfig.setAllowedOriginPatterns(List.of("*"));
        publicConfig.setAllowedMethods(List.of("POST", "DELETE", "OPTIONS"));
        publicConfig.setAllowedHeaders(List.of("Content-Type"));
        publicConfig.setAllowCredentials(false);
        ((UrlBasedCorsConfigurationSource) source).registerCorsConfiguration("/api/public/**", publicConfig);
        return source;
    }

    private static void writeError(jakarta.servlet.http.HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
