package com.jordane237.ticketmanagement.repository;

import com.jordane237.ticketmanagement.entity.StatutTicket;
import com.jordane237.ticketmanagement.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    boolean existsByTitre(String titre);

    boolean existsByTitreIgnoreCase(String titre);

    List<Ticket> findByStatutOrderByDateCreationDesc(StatutTicket statut);

    List<Ticket> findAllByOrderByDateCreationDesc();
}
