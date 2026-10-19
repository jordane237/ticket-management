package com.jordane237.ticketmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corps de la requete de creation d'un ticket.
 * Le client ne peut transmettre que le titre et la description.
 */
public class TicketCreateRequest {

    @NotBlank(message = "Le titre est obligatoire.")
    @Size(max = 100, message = "Le titre ne doit pas depasser 100 caracteres.")
    private String titre;

    private String description;

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
