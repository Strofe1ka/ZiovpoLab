package ru.ziovpo.backend.license.ticket;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@JsonPropertyOrder(alphabetic = true)
public record Ticket(
        Instant serverTimestamp,
        long ticketTtlSeconds,
        LocalDate licenseActivationDate,
        LocalDate licenseExpirationDate,
        UUID userId,
        UUID deviceId,
        boolean licenseBlocked
) {
}
