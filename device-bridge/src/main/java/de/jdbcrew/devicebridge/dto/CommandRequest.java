package de.jdbcrew.devicebridge.dto;

import jakarta.validation.constraints.NotBlank;

public record CommandRequest(
        @NotBlank String command
) {}