package de.jdbcrew.devicebridge.service;

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

    // ============= Sensor Import API =============
    public record SensorMeasurement(
            String device,
            String kind,
            String label,
            java.sql.Timestamp ts,
            String location,
            String metric,
            Double valueNum,
            Integer valueBool,
            String unit,
            String valueText,
            String metaJson
    ) {}

    public record TelemetryRow(
            java.sql.Timestamp ts,
            Double servo10_y_deg,
            Double servo11_x_deg,
            Double led12_pct,
            Double led13_pct,
            Double led14_pct,
            Double led15_pct,
            Double temp_c,
            Double press_hpa,
            Double hum_perc,
            Double mag_x,
            Double mag_y,
            Double mag_z,
            Double accel_x,
            Double accel_y,
            Double accel_z,
            Double gyro_x,
            Double gyro_y,
            Double gyro_z
    ) {}

    @Transactional
    public int importTelemetry(String dbKey, List<TelemetryRow> rows) {
        if (rows == null || rows.isEmpty()) return 0;
        JdbcTemplate jt = jdbc(dbKey);
        final String sql = "INSERT INTO telemetry (ts, servo10_y_deg, servo11_x_deg, led12_pct, led13_pct, led14_pct, led15_pct, temp_c, press_hpa, hum_perc, mag_x, mag_y, mag_z, accel_x, accel_y, accel_z, gyro_x, gyro_y, gyro_z) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        int[][] res = jt.batchUpdate(sql, rows, rows.size(), (ps, r) -> {
            ps.setTimestamp(1, r.ts());
            setNullableDouble(ps, 2, r.servo10_y_deg());
            setNullableDouble(ps, 3, r.servo11_x_deg());
            setNullableDouble(ps, 4, r.led12_pct());
            setNullableDouble(ps, 5, r.led13_pct());
            setNullableDouble(ps, 6, r.led14_pct());
            setNullableDouble(ps, 7, r.led15_pct());
            setNullableDouble(ps, 8, r.temp_c());
            setNullableDouble(ps, 9, r.press_hpa());
            setNullableDouble(ps, 10, r.hum_perc());
            setNullableDouble(ps, 11, r.mag_x());
            setNullableDouble(ps, 12, r.mag_y());
            setNullableDouble(ps, 13, r.mag_z());
            setNullableDouble(ps, 14, r.accel_x());
            setNullableDouble(ps, 15, r.accel_y());
            setNullableDouble(ps, 16, r.accel_z());
            setNullableDouble(ps, 17, r.gyro_x());
            setNullableDouble(ps, 18, r.gyro_y());
            setNullableDouble(ps, 19, r.gyro_z());
        });
        int count = 0; 
        for (int[] arr : res) {
            if (arr == null) continue;
            for (int i : arr) count += (i >= 0 ? i : 0);
        }
        return count;
    }

    private void setNullableDouble(PreparedStatement ps, int idx, Double v) throws SQLException {
        if (v == null) ps.setNull(idx, java.sql.Types.DOUBLE); else ps.setDouble(idx, v);
    }

    @Transactional
    public int importMeasurements(String dbKey, List<SensorMeasurement> rows) {
        if (rows == null || rows.isEmpty()) return 0;
        JdbcTemplate jt = jdbc(dbKey);
        Map<String, Long> deviceCache = new HashMap<>();
        Map<String, Long> sensorCache = new HashMap<>();
        int inserted = 0;
        for (SensorMeasurement r : rows) {
            long deviceId = deviceCache.computeIfAbsent(r.device(), name -> ensureDevice(jt, name));
            String sensorKey = deviceId + "|" + (r.kind() == null ? "" : r.kind()) + "|" + (r.label() == null ? "" : r.label());
            long sensorId = sensorCache.computeIfAbsent(sensorKey, k -> ensureSensor(jt, deviceId, r.kind(), r.label()));
            inserted += insertMeasurement(jt, sensorId, r);
        }
        return inserted;
    }

    private long ensureDevice(JdbcTemplate jt, String name) {
        if (!StringUtils.hasText(name)) throw new IllegalArgumentException("device name required");
        List<Long> ids = jt.query("SELECT id FROM devices WHERE name = ?", (rs, i) -> rs.getLong(1), name);
        if (!ids.isEmpty()) return ids.get(0);
        // insert
        jt.update(con -> {
            PreparedStatement ps = con.prepareStatement("INSERT INTO devices (name) VALUES (?)");
            ps.setString(1, name);
            return ps;
        });
        return jt.queryForObject("SELECT id FROM devices WHERE name = ?", Long.class, name);
    }

    private long ensureSensor(JdbcTemplate jt, long deviceId, String kind, String label) {
        List<Long> ids = jt.query(
                "SELECT id FROM sensors WHERE device_id = ? AND kind = ? AND COALESCE(label,'') = COALESCE(?, '')",
                (rs, i) -> rs.getLong(1), deviceId, kind, label);
        if (!ids.isEmpty()) return ids.get(0);
        jt.update(con -> {
            PreparedStatement ps = con.prepareStatement("INSERT INTO sensors (device_id, kind, label) VALUES (?, ?, ?)");
            ps.setLong(1, deviceId);
            ps.setString(2, kind);
            if (label == null || label.isBlank()) ps.setNull(3, java.sql.Types.VARCHAR); else ps.setString(3, label);
            return ps;
        });
        return jt.queryForObject(
                "SELECT id FROM sensors WHERE device_id = ? AND kind = ? AND COALESCE(label,'') = COALESCE(?, '')",
                Long.class, deviceId, kind, label);
    }

    private int insertMeasurement(JdbcTemplate jt, long sensorId, SensorMeasurement r) {
        return jt.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO measurements (sensor_id, ts, location, metric, value_num, value_bool, value_text, unit, meta_json) " +
                            "VALUES (?,?,?,?,?,?,?,?,?)");
            ps.setLong(1, sensorId);
            if (r.ts() != null) ps.setTimestamp(2, r.ts()); else ps.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
            if (StringUtils.hasText(r.location())) ps.setString(3, r.location()); else ps.setNull(3, java.sql.Types.VARCHAR);
            ps.setString(4, r.metric());
            if (r.valueNum() != null) ps.setDouble(5, r.valueNum()); else ps.setNull(5, java.sql.Types.DOUBLE);
            if (r.valueBool() != null) ps.setInt(6, r.valueBool()); else ps.setNull(6, java.sql.Types.INTEGER);
            if (StringUtils.hasText(r.valueText())) ps.setString(7, r.valueText()); else ps.setNull(7, java.sql.Types.VARCHAR);
            if (StringUtils.hasText(r.unit())) ps.setString(8, r.unit()); else ps.setNull(8, java.sql.Types.VARCHAR);
            if (StringUtils.hasText(r.metaJson())) ps.setString(9, r.metaJson()); else ps.setNull(9, java.sql.Types.VARCHAR);
            return ps;
        });
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
