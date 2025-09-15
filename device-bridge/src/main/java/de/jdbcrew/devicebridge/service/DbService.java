package de.jdbcrew.devicebridge.service;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class DbService {

    private final JdbcTemplate jdbc;
    private static final Set<String> SUPPORTED_DATABASES = Set.of("db1", "db2", "db3");

    public DbService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean isSupportedDb(String key) {
        return SUPPORTED_DATABASES.contains(key);
    }

    public Set<String> supportedDatabases() {
        return SUPPORTED_DATABASES;
    }

    @Transactional
    public void replaceItems(String db, List<String> names) {
        requireSupportedDb(db);
        jdbc.update("DELETE FROM items WHERE db = ?", db);
        if (names == null || names.isEmpty()) {
            return;
        }
        jdbc.batchUpdate("INSERT INTO items (db, name) VALUES (?, ?)", new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, db);
                ps.setString(2, names.get(i));
            }

            @Override
            public int getBatchSize() {
                return names.size();
            }
        });
    }

    public void deleteItems(String db) {
        requireSupportedDb(db);
        jdbc.update("DELETE FROM items WHERE db = ?", db);
    }

    public List<Map<String, Object>> fetchItems(String db, String nameFilter) {
        requireSupportedDb(db);
        StringBuilder sql = new StringBuilder("SELECT id, name FROM items WHERE db = ?");
        List<Object> params = new ArrayList<>();
        params.add(db);
        if (StringUtils.hasText(nameFilter)) {
            sql.append(" AND LOWER(name) LIKE ?");
            params.add('%' + nameFilter.toLowerCase(Locale.ROOT) + '%');
        }
        sql.append(" ORDER BY id");
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    public boolean ping() {
        Integer one = jdbc.queryForObject("SELECT 1", Integer.class);
        return one != null && one == 1;
    }

    public List<Map<String, Object>> query(String sql) {
        // Demo: rohes SQL. Später bitte parametrisieren/whitelisten!
        return jdbc.queryForList(sql);
    }

    private void requireSupportedDb(String db) {
        if (!isSupportedDb(db)) {
            throw new IllegalArgumentException("Unknown database: " + db);
        }
    }
}
