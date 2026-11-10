
package com.cmpt276.frasercare;

import java.util.Set;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // Where each role goes after logging in
    public static final String CUSTOMER_HOME = "/customer-home.html";
    public static final String STAFF_HOME = "/staff-dashboard.html";
    public static final String ADMIN_HOME = "/admin-users.html";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public pages
                .requestMatchers(
                    "/", "/index.html", "/login.html", "/signup.html",
                    "/favicon.ico", "/css/**", "/js/**", "/error",
                    "/api/register", "/api/me", "/csrf"
                ).permitAll()

                // Admin only
                .requestMatchers(ADMIN_HOME, "/api/admin/**")
                    .hasRole("ADMIN")

                // Staff (admins can see it too)
                .requestMatchers(STAFF_HOME, "/api/staff/**")
                    .hasAnyRole("STAFF", "ADMIN")

                // Customer only
                .requestMatchers(CUSTOMER_HOME, "/pickups.html" , "/api/pickups/**")
                    .hasRole("CUSTOMER")

                // Everything else just needs a login
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/self-check/**")
            )
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .successHandler((request, response, authentication) ->
                    response.sendRedirect(landingPageFor(
                        AuthorityUtils.authorityListToSet(
                            authentication.getAuthorities())))
                )
                .failureUrl("/login.html?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login.html?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }

    // Picks the landing page for a user's roles
    public static String landingPageFor(Set<String> roles) {
        if (roles.contains("ROLE_ADMIN")) {
            return ADMIN_HOME;
        }

        if (roles.contains("ROLE_STAFF")) {
            return STAFF_HOME;
        }

        return CUSTOMER_HOME;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JdbcUserDetailsManager userDetailsManager(DataSource dataSource) {
        JdbcUserDetailsManager manager =
            new JdbcUserDetailsManager(dataSource);

        manager.setUsersByUsernameQuery(
            "SELECT username, password, enabled FROM auth_users WHERE username = ?"
        );

        manager.setAuthoritiesByUsernameQuery(
            "SELECT username, authority FROM auth_authorities WHERE username = ?"
        );

        manager.setUserExistsSql(
            // Spring Security 7 expects a count here, not a row
            "SELECT COUNT(*) FROM auth_users WHERE username = ?"
        );

        manager.setCreateUserSql(
            "INSERT INTO auth_users (username, password, enabled) VALUES (?, ?, ?)"
        );

        manager.setCreateAuthoritySql(
            "INSERT INTO auth_authorities (username, authority) VALUES (?, ?)"
        );

        return manager;
    }
}
