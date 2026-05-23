package ru.ziovpo.backend.license.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterDeviceRequest(
        @NotBlank String name,
        @NotBlank String macAddress
) {
}
