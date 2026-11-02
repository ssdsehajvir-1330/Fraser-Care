package com.cmpt276.frasercare;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import javax.sql.DataSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/login.html", "/signup.html",
                 "/favicon.ico", "/css/**",
                 "/api/register", "/csrf", "/js/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
            .loginPage("/login.html")
            .loginProcessingUrl("/login")
            .defaultSuccessUrl("/", true)
            .permitAll()
        );

        return http.build();
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
            "SELECT username FROM auth_users WHERE username = ?"
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
