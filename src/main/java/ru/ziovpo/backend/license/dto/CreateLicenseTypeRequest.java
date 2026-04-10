package ru.ziovpo.backend.license.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateLicenseTypeRequest(
        @NotBlank String name,
        @Positive int defaultDurationInDays,
        String description
) {
}
