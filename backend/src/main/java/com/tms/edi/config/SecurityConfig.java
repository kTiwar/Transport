package com.tms.edi.config;

import com.tms.edi.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // File endpoints
                .requestMatchers(HttpMethod.GET,    "/api/v1/files/**").hasAnyRole("ADMIN","OPERATOR","VIEWER")
                .requestMatchers(HttpMethod.POST,   "/api/v1/files/upload").hasAnyRole("ADMIN","OPERATOR")
                .requestMatchers(HttpMethod.POST,   "/api/v1/files/*/process").hasAnyRole("ADMIN","OPERATOR")
                .requestMatchers(HttpMethod.POST,   "/api/v1/files/*/retry").hasAnyRole("ADMIN","OPERATOR")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/files/**").hasRole("ADMIN")
                // Mapping endpoints
                .requestMatchers(HttpMethod.GET,    "/api/v1/mappings/**").hasAnyRole("ADMIN","OPERATOR","VIEWER")
                .requestMatchers(HttpMethod.POST,   "/api/v1/mappings/**").hasAnyRole("ADMIN","OPERATOR")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/mappings/**").hasAnyRole("ADMIN","OPERATOR")
                // Partner endpoints — admin only
                .requestMatchers("/api/v1/partners/**").hasRole("ADMIN")
                // Errors
                .requestMatchers(HttpMethod.GET, "/api/v1/errors/**").hasAnyRole("ADMIN","OPERATOR","VIEWER")
                .requestMatchers(HttpMethod.PUT, "/api/v1/errors/**").hasAnyRole("ADMIN","OPERATOR")
                // Monitoring
                .requestMatchers("/api/v1/monitoring/**").hasAnyRole("ADMIN","OPERATOR","VIEWER")
                // Go4IMP import staging + TMS orders (JWT via same client as /api/v1)
                .requestMatchers(HttpMethod.GET,  "/api/v1/import-orders/**").hasAnyRole("ADMIN","OPERATOR","VIEWER")
                .requestMatchers(HttpMethod.POST, "/api/v1/import-orders/**").hasAnyRole("ADMIN","OPERATOR")
                .requestMatchers(HttpMethod.GET,  "/api/v1/tms-orders/**").hasAnyRole("ADMIN","OPERATOR","VIEWER")
                // Route optimization (OSRM / Nominatim / OR-Tools pipeline)
                .requestMatchers(HttpMethod.GET,  "/api/v1/routing/**").hasAnyRole("ADMIN","OPERATOR","VIEWER")
                .requestMatchers("/api/v1/routing/**").hasAnyRole("ADMIN","OPERATOR")
                .requestMatchers(HttpMethod.GET,  "/api/v1/address-master/**").hasAnyRole("ADMIN","OPERATOR","VIEWER")
                .requestMatchers("/api/v1/address-master/**").hasAnyRole("ADMIN","OPERATOR")
                .requestMatchers(HttpMethod.GET,  "/api/v1/reference-master/**").hasAnyRole("ADMIN","OPERATOR","VIEWER")
                .requestMatchers("/api/v1/reference-master/**").hasAnyRole("ADMIN","OPERATOR")
                .requestMatchers(HttpMethod.GET,  "/api/v1/master-parties/**").hasAnyRole("ADMIN","OPERATOR","VIEWER")
                .requestMatchers("/api/v1/master-parties/**").hasAnyRole("ADMIN","OPERATOR")
                .requestMatchers(HttpMethod.GET,  "/api/v1/address-lookups/**").hasAnyRole("ADMIN","OPERATOR","VIEWER")
                .requestMatchers("/api/v1/address-lookups/**").hasAnyRole("ADMIN","OPERATOR")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of("*"));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS","PATCH"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
