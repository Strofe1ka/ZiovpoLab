package ru.ziovpo.backend.license.dto;

import java.util.UUID;

public record LicenseTypeResponse(UUID id, String name, int defaultDurationInDays, String description) {
}
