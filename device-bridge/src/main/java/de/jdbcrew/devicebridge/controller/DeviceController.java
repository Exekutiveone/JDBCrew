package de.jdbcrew.devicebridge.controller;

import de.jdbcrew.devicebridge.dto.CommandRequest;
import de.jdbcrew.devicebridge.dto.StatusResponse;
import de.jdbcrew.devicebridge.service.DeviceService;
import de.jdbcrew.devicebridge.service.RaspberryPiService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class DeviceController {

    private final RaspberryPiService piService;

    public DeviceController(RaspberryPiService piService) {
        this.piService = piService;
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
    if ("pi".equalsIgnoreCase(target)) return piService;
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown target: " + target);
}
}
