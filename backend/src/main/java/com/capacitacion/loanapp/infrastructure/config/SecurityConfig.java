package com.capacitacion.loanapp.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * TODO (M4 - Seguridad): Esta configuración es INSEGURA intencionalmente.
 *
 * Problemas a resolver en M4:
 *  1. anyRequest().permitAll() → cualquier request sin auth
 *  2. Sin JWT stateless
 *  3. Sin RBAC con @PreAuthorize
 *  4. Sin rate limiting en /api/auth
 *
 * Implementar en M4:
 *  - JWT con RS256 (clave asimétrica)
 *  - .sessionManagement(STATELESS)
 *  - .addFilterBefore(jwtFilter, ...)
 *  - @EnableMethodSecurity + @PreAuthorize en controladores
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()
                .anyRequest().permitAll()  // TODO (M4): .authenticated()
            )
            .build();
    }
}
