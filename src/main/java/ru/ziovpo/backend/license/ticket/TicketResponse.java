package ru.ziovpo.backend.license.ticket;

public record TicketResponse(Ticket ticket, String signatureBase64) {
}
