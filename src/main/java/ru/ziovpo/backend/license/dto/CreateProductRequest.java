package ru.ziovpo.backend.license.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateProductRequest(@NotBlank String name) {
}
