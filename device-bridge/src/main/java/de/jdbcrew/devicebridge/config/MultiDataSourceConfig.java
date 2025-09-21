package de.jdbcrew.devicebridge.config;

//Damit erzeugt Spring beim Start pro Eintrag in dbs: einen eigenen JdbcTemplate und legt sie in eine Map <dbKey, JdbcTemplate>.

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class MultiDataSourceConfig {
    private static final Logger log = LoggerFactory.getLogger(MultiDataSourceConfig.class);

    @Bean
    @ConfigurationProperties(prefix = "dbs")
    public Map<String, DbProps> dbTargets() {
        return new HashMap<>(); // wird automatisch aus application.yml befüllt
    }

    @Bean
    public Map<String, JdbcTemplate> jdbcTemplates(Map<String, DbProps> dbTargets) {
        Map<String, JdbcTemplate> map = new HashMap<>();
        for (var e : dbTargets.entrySet()) {
            String key = e.getKey().toLowerCase();
            DbProps p = e.getValue();

            HikariConfig cfg = new HikariConfig();
            cfg.setPoolName("db-" + key);
            cfg.setJdbcUrl(p.getUrl());
            if (p.getDriverClassName() != null && !p.getDriverClassName().isBlank()) {
                cfg.setDriverClassName(p.getDriverClassName());
            }
            if (p.getUsername() != null) cfg.setUsername(p.getUsername());
            if (p.getPassword() != null) cfg.setPassword(p.getPassword());
            cfg.setMaximumPoolSize(10);
            cfg.setMinimumIdle(0);
            // Start even if the DB is temporarily unreachable; acquire lazily
            cfg.setInitializationFailTimeout(-1);
            try {
                DataSource ds = new HikariDataSource(cfg);
                JdbcTemplate jt = new JdbcTemplate(ds);
                map.put(key, jt);

                // Lightweight init: for SQLite targets, ensure local schema exists so uploads work.
                if (p.getUrl() != null && p.getUrl().toLowerCase().startsWith("jdbc:sqlite:")) {
                    try (Connection c = ds.getConnection()) {
                        var resource = new ClassPathResource("schema.sql");
                        if (resource.exists()) {
                            ScriptUtils.executeSqlScript(c, resource);
                            log.info("Initialized SQLite schema for '{}' using schema.sql", key);
                        } else {
                            log.warn("schema.sql not found on classpath; skipping SQLite schema init for '{}'", key);
                        }
                    } catch (SQLException | ScriptException se) {
                        log.error("Failed to init SQLite schema for '{}': {}", key, se.getMessage());
                    }
                }
            } catch (RuntimeException ex) {
                log.warn("Skipping DB target '{}' ({}): {}", key, p.getUrl(), ex.getMessage());
            }
        }
        return Map.copyOf(map);
    }

    public static class DbProps {
        private String url;
        private String username;
        private String password;
        private String driverClassName;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getDriverClassName() { return driverClassName; }
        public void setDriverClassName(String driverClassName) { this.driverClassName = driverClassName; }
    }
}
