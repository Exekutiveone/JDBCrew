package de.jdbcrew.devicebridge.service;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

@Service
public class DbService {

    private final Map<String, JdbcTemplate> jdbcByKey;
    private final Set<String> supported; // dynamisch aus Config

    public DbService(Map<String, JdbcTemplate> jdbcTemplates) {
        this.jdbcByKey = jdbcTemplates;
        // erlaubte Keys aus application.yml (dbs: …)
        this.supported = Set.copyOf(
                jdbcTemplates.keySet().stream()
                        .map(k -> k.toLowerCase(Locale.ROOT))
                        .toList()
        );
    }

    public boolean isSupportedDb(String key) {
        return key != null && supported.contains(key.toLowerCase(Locale.ROOT));
    }

    public Set<String> supportedDatabases() {
        return supported;
    }

    private JdbcTemplate jdbc(String key) {
        JdbcTemplate jt = jdbcByKey.get(key.toLowerCase(Locale.ROOT));
        if (jt == null) throw new IllegalArgumentException("Unknown database: " + key);
        return jt;
    }

    @Transactional
    public void replaceItems(String dbKey, List<String> names) {
        JdbcTemplate jt = jdbc(dbKey);
        jt.update("DELETE FROM items WHERE db = ?", dbKey);
        if (names == null || names.isEmpty()) return;
        jt.batchUpdate("INSERT INTO items (db, name) VALUES (?, ?)", new BatchPreparedStatementSetter() {
            @Override public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, dbKey);
                ps.setString(2, names.get(i));
            }
            @Override public int getBatchSize() { return names.size(); }
        });
    }

    public void deleteItems(String dbKey) {
        jdbc(dbKey).update("DELETE FROM items WHERE db = ?", dbKey);
    }

    public List<Map<String, Object>> fetchItems(String dbKey, String nameFilter) {
        JdbcTemplate jt = jdbc(dbKey);
        if (!StringUtils.hasText(nameFilter)) {
            return jt.queryForList("SELECT id, name FROM items WHERE db = ? ORDER BY id", dbKey);
        }
        return jt.queryForList(
                "SELECT id, name FROM items WHERE db = ? AND LOWER(name) LIKE ? ORDER BY id",
                dbKey, "%" + nameFilter.toLowerCase(Locale.ROOT) + "%"
        );
    }

    public boolean ping(String dbKey) {
        Integer one = jdbc(dbKey).queryForObject("SELECT 1", Integer.class);
        return one != null && one == 1;
    }

    public List<Map<String, Object>> query(String dbKey, String sql) {
        return jdbc(dbKey).queryForList(sql);
    }

    public List<Map<String, Object>> fetchSchema(String dbKey) {
        JdbcTemplate jt = jdbc(dbKey);
        // Versuch MariaDB/MySQL
        try {
            return jt.queryForList("""
                SELECT table_name AS name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                ORDER BY table_name
            """);
        } catch (Exception ignore) {
            // Fallback SQLite
            return jt.queryForList("""
                SELECT name
                FROM sqlite_master
                WHERE type='table' AND name NOT LIKE 'sqlite_%'
                ORDER BY name
            """);
        }
    }
}
