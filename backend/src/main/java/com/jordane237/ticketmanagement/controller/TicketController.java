package com.jordane237.ticketmanagement.controller;

import com.jordane237.ticketmanagement.dto.TicketCreateRequest;
import com.jordane237.ticketmanagement.dto.TicketResponse;
import com.jordane237.ticketmanagement.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Points d'entree REST de l'API de gestion des tickets.
 * Aucune logique metier ici : tout est delegue au service.
 */
@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets", description = "Gestion des tickets de support")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Creer un ticket",
            description = "Cree un ticket avec le statut initial OUVERT et une date de "
                    + "creation generee par le serveur. RG-01 et RG-02 sont appliquees.")
    @ApiResponse(responseCode = "201", description = "Ticket cree")
    @ApiResponse(responseCode = "400", description = "RG-01 : titre obligatoire ou trop long")
    @ApiResponse(responseCode = "409", description = "RG-02 : titre deja utilise")
    public TicketResponse creer(@Valid @RequestBody TicketCreateRequest requete) {
        return ticketService.creer(requete);
    }

    @GetMapping
    @Operation(summary = "Lister les tickets",
            description = "Retourne tous les tickets, ou seulement ceux du statut demande "
                    + "(parametre statut = OUVERT, EN_COURS ou FERME).")
    @ApiResponse(responseCode = "200", description = "Liste des tickets (eventuellement vide)")
    @ApiResponse(responseCode = "400", description = "Statut de filtre invalide")
    public List<TicketResponse> lister(
            @Parameter(description = "Filtre par statut (OUVERT, EN_COURS, FERME). Omettre pour tout voir.")
            @RequestParam(required = false) String statut) {
        return ticketService.lister(statut);
    }

    @PatchMapping("/{id}/fermer")
    @Operation(summary = "Fermer un ticket",
            description = "Passe un ticket OUVERT ou EN_COURS a l'etat FERME (RG-03).")
    @ApiResponse(responseCode = "200", description = "Ticket ferme")
    @ApiResponse(responseCode = "404", description = "Ticket introuvable")
    @ApiResponse(responseCode = "409", description = "RG-03 : ticket deja ferme")
    public TicketResponse fermer(@PathVariable Long id) {
        return ticketService.fermer(id);
    }
}
