package de.jdbcrew.devicebridge.service;


import de.jdbcrew.devicebridge.dto.StatusResponse;

public interface DeviceService {
    String getTarget();
    StatusResponse getStatus();
    StatusResponse runCommand(String command);
}
