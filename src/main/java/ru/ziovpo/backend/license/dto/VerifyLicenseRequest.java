package ru.ziovpo.backend.license.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record VerifyLicenseRequest(
        @NotBlank String code,
        @NotNull UUID deviceId
) {
}
