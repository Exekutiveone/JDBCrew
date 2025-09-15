package de.jdbcrew.devicebridge.dto;

import java.util.Map;

public record StatusResponse(
        String target,
        boolean reachable,
        String message,
        Map<String, Object> data
) {
    public static StatusResponse ok(String target, Map<String, Object> data) {
        return new StatusResponse(target, true, "OK", data);
    }
    public static StatusResponse error(String target, String message) {
        return new StatusResponse(target, false, message, Map.of());
    }
}
