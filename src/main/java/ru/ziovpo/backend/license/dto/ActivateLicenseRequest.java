package ru.ziovpo.backend.license.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ActivateLicenseRequest(
        @NotBlank String code,
        @NotNull UUID deviceId
) {
}
