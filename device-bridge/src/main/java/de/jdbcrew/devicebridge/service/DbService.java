package de.jdbcrew.devicebridge.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DbService {

    private final JdbcTemplate jdbc;

    public DbService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean ping() {
        Integer one = jdbc.queryForObject("SELECT 1", Integer.class);
        return one != null && one == 1;
    }

    public List<Map<String, Object>> query(String sql) {
        // Demo: rohes SQL. Später bitte parametrisieren/whitelisten!
        return jdbc.queryForList(sql);
    }
}
