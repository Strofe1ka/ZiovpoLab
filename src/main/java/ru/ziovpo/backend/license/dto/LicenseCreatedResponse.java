package ru.ziovpo.backend.license.dto;

import java.util.UUID;

public record LicenseCreatedResponse(UUID id, String code) {
}
