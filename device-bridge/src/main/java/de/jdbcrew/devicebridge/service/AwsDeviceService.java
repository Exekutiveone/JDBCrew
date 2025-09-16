package de.jdbcrew.devicebridge.service;

import de.jdbcrew.devicebridge.dto.StatusResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class AwsDeviceService implements DeviceService {

    private final String baseUrl;
    private final RestClient client;
    private final boolean mock;

    public AwsDeviceService(
            @Value("${devices.aws.base-url:}") String baseUrl,
            RestClient.Builder builder
    ) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.mock = this.baseUrl.isEmpty();
        this.client = builder.build();
    }

    @Override
    public String getTarget() {
        return "aws";
    }

    @Override
    public StatusResponse getStatus() {
        if (mock) {
            return StatusResponse.ok(getTarget(), Map.of(
                    "region", "eu-central-1",
                    "instances", 3,
                    "status", "healthy"
            ));
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = client.get()
                    .uri(baseUrl + "/api/status")
                    .retrieve()
                    .body(Map.class);
            return StatusResponse.ok(getTarget(), data);
        } catch (Exception e) {
            return StatusResponse.error(getTarget(), "AWS unreachable: " + e.getMessage());
        }
    }

    @Override
    public StatusResponse runCommand(String command) {
        if (mock) {
            return StatusResponse.ok(getTarget(), Map.of(
                    "stdout", "(mock aws) ran: " + command
            ));
        }
        try {
            Map<String, Object> payload = Map.of("command", command);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = client.post()
                    .uri(baseUrl + "/api/command")
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
            return StatusResponse.ok(getTarget(), data);
        } catch (Exception e) {
            return StatusResponse.error(getTarget(), "AWS command failed: " + e.getMessage());
        }
    }
}

