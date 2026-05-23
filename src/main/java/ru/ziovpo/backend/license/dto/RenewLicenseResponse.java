package ru.ziovpo.backend.license.dto;

import java.time.LocalDate;
import java.util.UUID;

public record RenewLicenseResponse(UUID licenseId, LocalDate endingDate) {
}
