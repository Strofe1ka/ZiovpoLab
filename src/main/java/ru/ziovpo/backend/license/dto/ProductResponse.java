package ru.ziovpo.backend.license.dto;

import java.util.UUID;

public record ProductResponse(UUID id, String name, boolean blocked) {
}
