package com.cmpt276.frasercare.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/*
 * Looks up customer names for appointments.
 * appointments.customer_id matches auth_users.user_id.
 */
@Service
public class CustomerNameService {

    private final NamedParameterJdbcTemplate jdbc;

    public CustomerNameService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Returns customer ID -> name. Uses the email if no name was saved.
    public Map<Long, String> getNames(Collection<Long> customerIds) {
        Map<Long, String> names = new HashMap<>();

        if (customerIds == null || customerIds.isEmpty()) {
            return names;
        }

        jdbc.query(
            "SELECT user_id, full_name, username FROM auth_users WHERE user_id IN (:ids)",
            new MapSqlParameterSource("ids", customerIds),
            rs -> {
                String fullName = rs.getString("full_name");

                names.put(
                    rs.getLong("user_id"),
                    fullName == null || fullName.isBlank()
                        ? rs.getString("username")
                        : fullName
                );
            }
        );

        return names;
    }
}
