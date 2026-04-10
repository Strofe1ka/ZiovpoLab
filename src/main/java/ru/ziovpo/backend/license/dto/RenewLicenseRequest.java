package ru.ziovpo.backend.license.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record RenewLicenseRequest(
        @NotBlank String code,
        @Positive Integer extendDays
) {
}
