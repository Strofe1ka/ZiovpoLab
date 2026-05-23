package ru.ziovpo.backend.auth.dto;

public record TokenResponse(String accessToken, String refreshToken) {
}
