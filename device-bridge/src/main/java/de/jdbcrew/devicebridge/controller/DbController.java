package de.jdbcrew.devicebridge.controller;

import de.jdbcrew.devicebridge.service.DbService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/db/{db}")
public class DbController {

    private static final MediaType CSV_MEDIA_TYPE = MediaType.parseMediaType("text/csv");

    private final DbService dbService;

    public DbController(DbService dbService) {
        this.dbService = dbService;
    }

    // Upload final telemetry CSV: timestamp + multiple metric columns
    @PostMapping("/upload")
    public ResponseEntity<Void> upload(@PathVariable String db, @RequestParam("file") MultipartFile file) {
        ensureSupported(db);
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leere Datei");
        }
        List<DbService.TelemetryRow> rows = parseTelemetryCsv(file);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Keine gültigen Zeilen gefunden");
        }
        int count = dbService.importTelemetry(db, rows);
        if (count <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Keine Messwerte importiert");
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // Minimal CSV export placeholder to avoid errors if clicked
    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable String db) {
        ensureSupported(db);
        StreamingResponseBody body = outputStream -> {
            try (Writer writer = new java.io.OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                writer.write("sensor_id,ts,metric,value_num,value_bool,unit\n");
                writer.flush();
            }
        };
        return ResponseEntity.ok()
                .contentType(CSV_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"export-" + db + ".csv\"")
                .body(body);
    }

    // Simple data view of latest entries
    @GetMapping("/data")
    public ResponseEntity<List<Map<String, Object>>> data(@PathVariable String db,
                                                          @RequestParam(value = "filter", required = false) String filterParam,
                                                          HttpServletRequest request) {
        ensureSupported(db);
        try {
            String sql = "SELECT id, ts, servo10_y_deg, servo11_x_deg, led12_pct, led13_pct, led14_pct, led15_pct, temp_c, press_hpa, hum_perc, mag_x, mag_y, mag_z, accel_x, accel_y, accel_z, gyro_x, gyro_y, gyro_z FROM telemetry ORDER BY id DESC LIMIT 200";
            List<Map<String, Object>> rows = dbService.query(db, sql);
            formatTimestampColumn(rows, "ts");
            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/schema")
    public ResponseEntity<List<Map<String, Object>>> schema(@PathVariable String db) {
        ensureSupported(db);
        return ResponseEntity.ok(dbService.fetchSchema(db));
    }

    private void ensureSupported(String db) {
        if (!dbService.isSupportedDb(db)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unbekannte Datenbank: " + db);
        }
    }

    private List<DbService.TelemetryRow> parseTelemetryCsv(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) return List.of();
            List<String> cols = splitCsv(header);
            Map<String, Integer> idx = new HashMap<>();
            for (int i = 0; i < cols.size(); i++) idx.put(cols.get(i).trim().toLowerCase(Locale.ROOT), i);
            String[] required = {"timestamp","servo10_y_deg","servo11_x_deg","led12_pct","led13_pct","led14_pct","led15_pct","temp_c","press_hpa","hum_perc","mag_x","mag_y","mag_z","accel_x","accel_y","accel_z","gyro_x","gyro_y","gyro_z"};
            for (String r : required) if (!idx.containsKey(r)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fehlende Spalte: " + r);

            List<DbService.TelemetryRow> rows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                List<String> v = splitCsv(line);
                java.sql.Timestamp ts = parseTimestamp(get(v, idx.get("timestamp")));
                if (ts == null) continue;
                rows.add(new DbService.TelemetryRow(
                        ts,
                        parseDouble(get(v, idx.get("servo10_y_deg"))),
                        parseDouble(get(v, idx.get("servo11_x_deg"))),
                        parseDouble(get(v, idx.get("led12_pct"))),
                        parseDouble(get(v, idx.get("led13_pct"))),
                        parseDouble(get(v, idx.get("led14_pct"))),
                        parseDouble(get(v, idx.get("led15_pct"))),
                        parseDouble(get(v, idx.get("temp_c"))),
                        parseDouble(get(v, idx.get("press_hpa"))),
                        parseDouble(get(v, idx.get("hum_perc"))),
                        parseDouble(get(v, idx.get("mag_x"))),
                        parseDouble(get(v, idx.get("mag_y"))),
                        parseDouble(get(v, idx.get("mag_z"))),
                        parseDouble(get(v, idx.get("accel_x"))),
                        parseDouble(get(v, idx.get("accel_y"))),
                        parseDouble(get(v, idx.get("accel_z"))),
                        parseDouble(get(v, idx.get("gyro_x"))),
                        parseDouble(get(v, idx.get("gyro_y"))),
                        parseDouble(get(v, idx.get("gyro_z")))
                ));
            }
            return rows;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datei konnte nicht gelesen werden", e);
        }
    }

    private List<String> splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                else { inQuotes = !inQuotes; }
            } else if (ch == ',' && !inQuotes) { out.add(cur.toString().trim()); cur.setLength(0); }
            else { cur.append(ch); }
        }
        out.add(cur.toString().trim());
        return out;
    }
    private String get(List<String> list, int idx) { return idx >= 0 && idx < list.size() ? list.get(idx) : ""; }
    private String emptyToNull(String s) { return StringUtils.hasText(s) ? s : null; }
    private Double parseDouble(String s) { try { return StringUtils.hasText(s) ? Double.valueOf(s) : null; } catch (Exception e) { return null; } }
    private Integer parseInt(String s) { try { return StringUtils.hasText(s) ? Integer.valueOf(s) : null; } catch (Exception e) { return null; } }
    private java.sql.Timestamp parseTimestamp(String s) {
        if (!StringUtils.hasText(s)) return null;
        try { return java.sql.Timestamp.from(java.time.Instant.parse(s)); } catch (Exception ignored) {}
        try { return java.sql.Timestamp.valueOf(java.time.LocalDateTime.parse(s)); } catch (Exception ignored) {}
        try { var odt = java.time.OffsetDateTime.parse(s); return java.sql.Timestamp.from(odt.toInstant()); } catch (Exception ignored) {}
        try {
            var fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS]");
            return java.sql.Timestamp.valueOf(java.time.LocalDateTime.parse(s, fmt));
        } catch (Exception ignored) {}
        return null;
    }

    private void formatTimestampColumn(List<Map<String, Object>> rows, String col) {
        if (rows == null || rows.isEmpty()) return;
        DateTimeFormatter outFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ZoneId zone = ZoneId.systemDefault();
        for (Map<String, Object> r : rows) {
            Object v = r.get(col);
            if (v == null) continue;
            String formatted = null;
            if (v instanceof java.sql.Timestamp ts) {
                formatted = outFmt.format(ts.toInstant().atZone(zone).toLocalDateTime());
            } else if (v instanceof Number num) {
                long ms = num.longValue();
                try {
                    formatted = outFmt.format(Instant.ofEpochMilli(ms).atZone(zone).toLocalDateTime());
                } catch (Exception ignore) { /* leave null */ }
            } else if (v instanceof String s) {
                // try parse numeric millis
                try {
                    long ms = Long.parseLong(s.trim());
                    formatted = outFmt.format(Instant.ofEpochMilli(ms).atZone(zone).toLocalDateTime());
                } catch (Exception ignore) {
                    // fall back: pass through
                    formatted = s;
                }
            }
            if (formatted != null) {
                r.put(col, formatted);
            }
        }
    }
}
