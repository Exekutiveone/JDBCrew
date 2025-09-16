package de.jdbcrew.devicebridge.controller;

import de.jdbcrew.devicebridge.dto.CommandRequest;
import de.jdbcrew.devicebridge.dto.StatusResponse;
import de.jdbcrew.devicebridge.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DeviceController {

    private final Map<String, DeviceService> servicesByTarget;

    public DeviceController(Map<String, DeviceService> services) {
        Map<String, DeviceService> resolved = new LinkedHashMap<>();
        if (services != null) {
            for (DeviceService service : services.values()) {
                if (service == null) {
                    continue;
                }
                String target = service.getTarget();
                if (target == null || target.isBlank()) {
                    continue;
                }
                resolved.putIfAbsent(target.toLowerCase(Locale.ROOT), service);
            }
        }
        this.servicesByTarget = Map.copyOf(resolved);
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/devices/{target}/status")
    public StatusResponse status(@PathVariable String target) {
        return resolve(target).getStatus();
    }

    @PostMapping("/devices/{target}/command")
    public StatusResponse command(@PathVariable String target, @Valid @RequestBody CommandRequest request) {
        return resolve(target).runCommand(request.command());
    }

    private DeviceService resolve(String target) {
        if (target == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown target: null");
        }
        DeviceService service = servicesByTarget.get(target.toLowerCase(Locale.ROOT));
        if (service == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown target: " + target);
        }
        return service;
    }
}

