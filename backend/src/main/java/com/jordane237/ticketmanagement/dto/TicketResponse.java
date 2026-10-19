package com.jordane237.ticketmanagement.dto;

import com.jordane237.ticketmanagement.entity.StatutTicket;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO de sortie representant un ticket dans les reponses de l'API.
 */
public record TicketResponse(
        @Schema(description = "Identifiant technique du ticket (genere par le serveur)", example = "1")
        Long id,

        @Schema(description = "Titre du ticket", example = "Probleme de connexion")
        String titre,

        @Schema(description = "Description facultative du ticket", example = "Impossible de se connecter au service")
        String description,

        @Schema(description = "Statut du ticket", example = "OUVERT")
        StatutTicket statut,

        @Schema(description = "Date de creation du ticket (generee par le serveur)", example = "2026-10-19T12:00:00")
        LocalDateTime dateCreation
) {
}
