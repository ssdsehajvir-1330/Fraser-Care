package com.cmpt276.frasercare;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Component;

/*
 * Creates one admin, one staff and one customer account on startup
 * so the TA can log in as each user type. Accounts that already
 * exist are left alone. Passwords can be changed with env vars.
 */
@Component
public class TestAccountSeeder implements ApplicationRunner {

    private final JdbcUserDetailsManager users;
    private final PasswordEncoder encoder;
    private final JdbcTemplate jdbc;

    @Value("${frasercare.seed.admin-password:Admin123!}")
    private String adminPassword;

    @Value("${frasercare.seed.staff-password:Staff123!}")
    private String staffPassword;

    @Value("${frasercare.seed.customer-password:Customer123!}")
    private String customerPassword;

    public TestAccountSeeder(
            JdbcUserDetailsManager users,
            PasswordEncoder encoder,
            JdbcTemplate jdbc) {
        this.users = users;
        this.encoder = encoder;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        createIfMissing("admin@frasercare.ca", "Test Admin", adminPassword, "ADMIN");
        createIfMissing("staff@frasercare.ca", "Test Staff", staffPassword, "STAFF");
        createIfMissing("customer@frasercare.ca", "Test Customer", customerPassword, "CUSTOMER");
    }

    private void createIfMissing(
            String email,
            String fullName,
            String password,
            String role) {

        if (users.userExists(email)) {
            return;
        }

        users.createUser(User.withUsername(email)
            .password(encoder.encode(password))
            .roles(role)
            .build());

        jdbc.update(
            "UPDATE auth_users SET full_name = ? WHERE username = ?",
            fullName, email
        );
    }
}
