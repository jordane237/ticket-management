package com.jordane237.ticketmanagement.service;

import com.jordane237.ticketmanagement.entity.StatutTicket;
import com.jordane237.ticketmanagement.entity.Ticket;
import com.jordane237.ticketmanagement.dto.TicketCreateRequest;
import com.jordane237.ticketmanagement.dto.TicketResponse;
import com.jordane237.ticketmanagement.exception.BadRequestException;
import com.jordane237.ticketmanagement.exception.DuplicateTitleException;
import com.jordane237.ticketmanagement.exception.IllegalStatusTransitionException;
import com.jordane237.ticketmanagement.exception.NotFoundException;
import com.jordane237.ticketmanagement.repository.TicketRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Logique metier des tickets.
 * Regles appliquees : RG-01 (validation du titre),
 * RG-02 (unicite du titre), RG-03 (fermeture).
 */
@Service
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    /**
     * Creation d'un ticket (RG-01 + RG-02).
     * Statut initial OUVERT et date de creation generes par le serveur.
     * L'identifiant, le statut et la date transmis par le client sont ignores.
     */
    @Transactional
    public TicketResponse creer(TicketCreateRequest requete) {
        // Normalisation : les espaces de tete/queue sont supprimes avant le controle.
        String titreNormalise = requete.getTitre() == null ? null : requete.getTitre().trim();
        if (titreNormalise == null || titreNormalise.isEmpty()) {
            throw new BadRequestException("Le titre est obligatoire.");
        }
        if (titreNormalise.length() > 100) {
            throw new BadRequestException("Le titre ne doit pas depasser 100 caracteres.");
        }
        if (ticketRepository.existsByTitreIgnoreCase(titreNormalise)) {
            throw new DuplicateTitleException(
                    "RG-02 : un ticket avec ce titre existe deja.");
        }

        String descriptionNettoye = requete.getDescription();
        if (descriptionNettoye != null && descriptionNettoye.isBlank()) {
            descriptionNettoye = null;
        }

        Ticket ticket = new Ticket();
        ticket.setDescription(descriptionNettoye);
        ticket.setTitre(titreNormalise);
        ticket.setStatut(StatutTicket.OUVERT); // statut initial impose par le serveur

        try {
            Ticket enregistre = ticketRepository.saveAndFlush(ticket);
            return versDto(enregistre);
        } catch (DataIntegrityViolationException ex) {
            // Conflit d'unicite detecte par la base (course avec une autre requete).
            throw new DuplicateTitleException(
                    "RG-02 : un ticket avec ce titre existe deja (contrainte de la base de donnees).");
        }
    }

    /**
     * Liste des tickets, filtree par statut cote backend si le parametre est present.
     * Un statut invalide produit une erreur 400 explicite.
     */
    @Transactional(readOnly = true)
    public List<TicketResponse> lister(String statutParam) {
        if (statutParam == null || statutParam.isBlank()) {
            return ticketRepository.findAllByOrderByDateCreationDesc().stream()
                    .map(this::versDto)
                    .toList();
        }
        StatutTicket statut;
        try {
            statut = StatutTicket.valueOf(statutParam.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(
                    "Le statut fourni \"" + statutParam + "\" est invalide. "
                            + "Valeurs autorisees : OUVERT, EN_COURS, FERME.");
        }
        return ticketRepository.findByStatutOrderByDateCreationDesc(statut).stream()
                .map(this::versDto)
                .toList();
    }

    /**
     * Fermeture d'un ticket (RG-03).
     * Un ticket OUVERT ou EN_COURS devient FERME ; un ticket deja FERME est refuse.
     */
    @Transactional
    public TicketResponse fermer(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Ticket introuvable pour l'identifiant " + id + "."));
        if (ticket.getStatut() == StatutTicket.FERME) {
            throw new IllegalStatusTransitionException(
                    "RG-03 : ce ticket est deja ferme et ne peut plus changer d'etat.");
        }
        ticket.setStatut(StatutTicket.FERME);
        return versDto(ticketRepository.save(ticket));
    }

    private TicketResponse versDto(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitre(),
                ticket.getDescription(),
                ticket.getStatut(),
                ticket.getDateCreation());
    }
}
