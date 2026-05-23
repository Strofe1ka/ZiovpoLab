package ru.ziovpo.backend.license.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record CreateLicenseRequest(
        @NotNull UUID userId,
        UUID ownerId,
        @NotNull UUID productId,
        @NotNull UUID typeId,
        @NotNull @Min(1) Integer deviceCount,
        String description,
        LocalDate endingDate
) {
}
