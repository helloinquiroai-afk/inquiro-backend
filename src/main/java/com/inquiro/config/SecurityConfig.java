package com.inquiro.config;

import com.inquiro.auth.AuthService;
import com.inquiro.auth.BearerTokenAuthenticationFilter;
import com.inquiro.communication.messenger.MetaSignatureValidator;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthService authService,
                                            MetaSignatureValidator signatureValidator,
                                            @Value("${inquiro.management-api-key:}") String managementKey)
            throws Exception {
        ManagementAccessFilter managementAccessFilter = new ManagementAccessFilter(managementKey, signatureValidator);
        BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter = new BearerTokenAuthenticationFilter(authService);
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/conversations/**", "/api/chat", "/webhook", "/messenger/webhook",
                                "/whatsapp/webhook").permitAll()
                        .requestMatchers("/api/auth/logout").authenticated()
                        .requestMatchers("/api/test/**", "/api/knowledge/**", "/h2-console/**").hasRole("OPERATOR")
                        .requestMatchers("/api/business/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> writeError(response, 401,
                                "Authentication required"))
                        .accessDeniedHandler((request, response, exception) -> writeError(response, 403,
                                "Access denied")))
                .addFilterBefore(managementAccessFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(bearerTokenAuthenticationFilter, ManagementAccessFilter.class);
        return http.build();
    }

    private static void writeError(jakarta.servlet.http.HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
