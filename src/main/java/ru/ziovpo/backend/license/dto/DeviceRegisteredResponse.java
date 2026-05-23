package ru.ziovpo.backend.license.dto;

import java.util.UUID;

public record DeviceRegisteredResponse(UUID id, String name, String macAddress) {
}
