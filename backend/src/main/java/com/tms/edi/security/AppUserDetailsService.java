package com.tms.edi.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loads users from the app_users table for Spring Security authentication.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return jdbcTemplate.query(
                "SELECT username, password_hash, role, active FROM app_users WHERE username = ? AND active = TRUE",
                rs -> {
                    if (!rs.next()) {
                        throw new UsernameNotFoundException("User not found: " + username);
                    }
                    String role = "ROLE_" + rs.getString("role").toUpperCase();
                    return User.builder()
                            .username(rs.getString("username"))
                            .password(rs.getString("password_hash"))
                            .authorities(List.of(new SimpleGrantedAuthority(role)))
                            .accountExpired(false)
                            .accountLocked(false)
                            .credentialsExpired(false)
                            .disabled(false)
                            .build();
                },
                username
        );
    }
}
