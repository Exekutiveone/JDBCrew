package de.jdbcrew.devicebridge.controller;

import de.jdbcrew.devicebridge.service.DbService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/db/{db}")
public class DbController {

    private static final MediaType CSV_MEDIA_TYPE = MediaType.parseMediaType("text/csv");

    private final DbService dbService;

    public DbController(DbService dbService) {
        this.dbService = dbService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Void> upload(@PathVariable String db, @RequestParam("file") MultipartFile file) {
        ensureSupported(db);
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leere Datei");
        }
        List<String> names = parseNames(file);
        if (names.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Keine gültigen Einträge gefunden");
        }
        dbService.replaceItems(db, names);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable String db) {
        ensureSupported(db);
        List<Map<String, Object>> rows = dbService.fetchItems(db, null);
        StreamingResponseBody body = outputStream -> {
            try (Writer writer = new java.io.OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                writer.write("id,name\n");
                for (Map<String, Object> row : rows) {
                    writer.write(toCsvRow(row));
                }
                writer.flush();
            }
        };
        return ResponseEntity.ok()
                .contentType(CSV_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"export-" + db + ".csv\"")
                .body(body);
    }

    @GetMapping("/data")
    public ResponseEntity<List<Map<String, Object>>> data(@PathVariable String db,
                                                          @RequestParam(value = "filter", required = false) String filterParam,
                                                          HttpServletRequest request) {
        ensureSupported(db);
        String expression = resolveFilterExpression(filterParam, request.getQueryString());
        String nameFilter = parseNameFilter(expression);
        return ResponseEntity.ok(dbService.fetchItems(db, nameFilter));
    }

    @GetMapping("/schema")
    public ResponseEntity<List<Map<String, Object>>> schema(@PathVariable String db) {
        ensureSupported(db);
        return ResponseEntity.ok(dbService.fetchSchema());
    }

    private void ensureSupported(String db) {
        if (!dbService.isSupportedDb(db)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unbekannte Datenbank: " + db);
        }
    }

    private List<String> parseNames(MultipartFile file) {
        List<String> names = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                String cleaned = line.replace("\uFEFF", "").trim();
                if (cleaned.isEmpty()) {
                    firstLine = false;
                    continue;
                }
                if (firstLine) {
                    String lower = cleaned.toLowerCase(Locale.ROOT);
                    if (lower.equals("name") || lower.startsWith("name,")) {
                        firstLine = false;
                        continue;
                    }
                }
                String value = extractValue(cleaned);
                if (!value.isEmpty()) {
                    names.add(value);
                }
                firstLine = false;
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datei konnte nicht gelesen werden", e);
        }
        return names;
    }

    private String extractValue(String line) {
        String value = line;
        int comma = value.lastIndexOf(',');
        if (comma >= 0) {
            value = value.substring(comma + 1);
        }
        value = value.trim();
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            value = value.substring(1, value.length() - 1).replace("\"\"", "\"");
        }
        return value;
    }

    private String resolveFilterExpression(String explicit, String rawQuery) {
        if (StringUtils.hasText(explicit)) {
            return explicit;
        }
        if (!StringUtils.hasText(rawQuery)) {
            return null;
        }
        String decoded = URLDecoder.decode(rawQuery, StandardCharsets.UTF_8);
        if (decoded.startsWith("filter=")) {
            return decoded.substring("filter=".length());
        }
        return decoded;
    }

    private String parseNameFilter(String expression) {
        if (!StringUtils.hasText(expression)) {
            return null;
        }
        String trimmed = expression.trim();
        if (!trimmed.contains("=")) {
            return trimmed;
        }
        String[] parts = trimmed.split("=", 2);
        String key = parts[0].trim();
        String value = parts.length > 1 ? parts[1].trim() : "";
        if (!StringUtils.hasText(value)) {
            return null;
        }
        if ("name".equalsIgnoreCase(key)) {
            return value;
        }
        return null;
    }

    private String toCsvRow(Map<String, Object> row) {
        String id = escapeCsv(row.get("id"));
        String name = escapeCsv(row.get("name"));
        return id + ',' + name + '\n';
    }

    private String escapeCsv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        boolean needsQuotes = text.contains(",") || text.contains("\n") || text.contains("\r") || text.contains("\"");
        if (needsQuotes) {
            text = text.replace("\"", "\"\"");
            return '"' + text + '"';
        }
        return text;
    }
}
