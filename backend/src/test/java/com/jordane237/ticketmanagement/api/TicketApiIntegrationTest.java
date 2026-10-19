package com.jordane237.ticketmanagement.api;

import com.jordane237.ticketmanagement.TicketManagementApplication;
import com.jordane237.ticketmanagement.config.TestcontainersConfig;
import com.jordane237.ticketmanagement.entity.StatutTicket;
import com.jordane237.ticketmanagement.entity.Ticket;
import com.jordane237.ticketmanagement.repository.TicketRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests d'integration executes contre une vraie base PostgreSQL (Testcontainers).
 * Couverture : scenarios 1 a 16 du cahier des charges.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {TicketManagementApplication.class, TestcontainersConfig.class})
@ActiveProfiles("test")
class TicketApiIntegrationTest {

    private static final String API = "/api/tickets";

    @Autowired
    TestRestTemplate rest;

    @Autowired
    TicketRepository ticketRepository;

    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void nettoyerBase() {
        ticketRepository.deleteAll();
        ticketRepository.flush();
    }

    // ---------- Utilitaires ----------

    private ResponseEntity<String> postTicket(Map<String, Object> corps) {
        HttpHeaders entetes = new HttpHeaders();
        entetes.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> requete = new HttpEntity<>(corps, entetes);
        return rest.exchange(API, HttpMethod.POST, requete, String.class);
    }

    private ResponseEntity<String> postTicket(String titre, String description) {
        Map<String, Object> corps = new HashMap<>();
        corps.put("titre", titre);
        if (description != null) {
            corps.put("description", description);
        }
        return postTicket(corps);
    }

    private ResponseEntity<String> getTickets(String statut) {
        String url = (statut == null) ? API : API + "?statut=" + statut;
        return rest.getForEntity(url, String.class);
    }

    private ResponseEntity<String> fermerTicket(Long id) {
        return rest.exchange(API + "/" + id + "/fermer", HttpMethod.PATCH, HttpEntity.EMPTY, String.class);
    }

    private JsonNode json(ResponseEntity<String> reponse) throws Exception {
        return mapper.readTree(reponse.getBody());
    }

    private static String corpsTicketInjection(String titre) {
        return """
                {"titre":"%s","description":"description test","id":9999,"statut":"FERME","dateCreation":"1999-01-01T00:00:00"}
                """.formatted(titre).trim();
    }

    // ---------- Scenarios 1 a 16 ----------

    // 1) Creation valide : 201 + corps correct.
    @Test
    void scenario1_creationValide_retourne201() throws Exception {
        ResponseEntity<String> rep = postTicket("Probleme de connexion", "Impossible de se connecter au service");
        assertThat(rep.getStatusCode().value()).isEqualTo(201);
        JsonNode corps = json(rep);
        assertThat(corps.get("titre").asText()).isEqualTo("Probleme de connexion");
        assertThat(corps.get("description").asText()).isEqualTo("Impossible de se connecter au service");
        assertThat(corps.get("id").asLong()).isPositive();
    }

    // 2) Statut initial OUVERT.
    @Test
    void scenario2_statutInitialEstOuvert() throws Exception {
        ResponseEntity<String> rep = postTicket("Panne imprimante", null);
        assertThat(rep.getStatusCode().value()).isEqualTo(201);
        assertThat(json(rep).get("statut").asText()).isEqualTo("OUVERT");
    }

    // 3) Date de creation generee par le serveur.
    @Test
    void scenario3_dateGenereeCoteServeur() throws Exception {
        LocalDateTime avant = LocalDateTime.now().minusMinutes(1);
        ResponseEntity<String> rep = postTicket("Ecran bleu", null);
        LocalDateTime date = LocalDateTime.parse(json(rep).get("dateCreation").asText(),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        assertThat(date).isNotNull();
        assertThat(date).isAfter(avant);
        assertThat(date).isBefore(LocalDateTime.now().plusMinutes(1));
    }

    // 4) Titre manquant : 400 + message explicite.
    @Test
    void scenario4_titreManquant_retourne400() throws Exception {
        ResponseEntity<String> rep = postTicket(new HashMap<>());
        assertThat(rep.getStatusCode().value()).isEqualTo(400);
        assertThat(json(rep).get("message").asText()).contains("Le titre est obligatoire");
        assertThat(json(rep).get("status").asInt()).isEqualTo(400);
    }

    // 5) Titre compose uniquement d'espaces : 400.
    @Test
    void scenario5_titreEspaces_retourne400() throws Exception {
        ResponseEntity<String> rep = postTicket("   ", null);
        assertThat(rep.getStatusCode().value()).isEqualTo(400);
        assertThat(json(rep).get("message").asText()).contains("Le titre est obligatoire");
    }

    // 6) Titre trop long (101 caracteres) : 400.
    @Test
    void scenario6_titreTropLong_retourne400() throws Exception {
        String titre = "A".repeat(101);
        ResponseEntity<String> rep = postTicket(titre, null);
        assertThat(rep.getStatusCode().value()).isEqualTo(400);
        assertThat(json(rep).get("message").asText()).contains("ne doit pas depasser 100");
    }

    // 7) Titre en double : 409 + RG-02.
    @Test
    void scenario7_titreEnDouble_retourne409avecRG02() throws Exception {
        postTicket("Cache perdu", null);
        ResponseEntity<String> rep = postTicket("Cache perdu", "autre description");
        assertThat(rep.getStatusCode().value()).isEqualTo(409);
        JsonNode corps = json(rep);
        assertThat(corps.get("message").asText()).contains("RG-02");
        assertThat(corps.get("error").asText()).isEqualTo("CONFLIT_METIER");
        assertThat(corps.get("status").asInt()).isEqualTo(409);
    }

    // 8) Liste complete : 200 + tableau JSON.
    @Test
    void scenario8_listeComplete_retourne200() throws Exception {
        postTicket("Ticket A", null);
        postTicket("Ticket B", null);
        ResponseEntity<String> rep = getTickets(null);
        assertThat(rep.getStatusCode().value()).isEqualTo(200);
        JsonNode liste = json(rep);
        assertThat(liste.isArray()).isTrue();
        assertThat(liste.size()).isEqualTo(2);
    }

    // 9) Filtre par statut (chaque valeur, filtrage cote backend).
    @Test
    void scenario9_filtreParStatut() throws Exception {
        Long idA = extraireId(postTicket("Ticket A", null));
        Long idB = extraireId(postTicket("Ticket B", null));
        fermerTicket(idA);
        passeEnCours(idB);

        assertThat(nbTickets(getTickets("OUVERT"))).isZero();
        assertThat(nbTickets(getTickets("FERME"))).isEqualTo(1);
        assertThat(nbTickets(getTickets("EN_COURS"))).isEqualTo(1);
        // Aucun parametre : les deux tickets.
        assertThat(nbTickets(getTickets(null))).isEqualTo(2);
    }

    // 10) Statut de filtre invalide : 400 + message explicite.
    @Test
    void scenario10_filtreStatutInvalide_retourne400() throws Exception {
        ResponseEntity<String> rep = getTickets("NIMPORTEQUOI");
        assertThat(rep.getStatusCode().value()).isEqualTo(400);
        assertThat(json(rep).get("message").asText()).contains("invalide");
        assertThat(json(rep).get("message").asText()).contains("OUVERT, EN_COURS, FERME");
    }

    // 11) Fermeture d'un ticket OUVERT : 200 + statut FERME.
    @Test
    void scenario11_fermetureOuvert_retourne200() throws Exception {
        Long id = extraireId(postTicket("Ticket a fermer", null));
        ResponseEntity<String> rep = fermerTicket(id);
        assertThat(rep.getStatusCode().value()).isEqualTo(200);
        assertThat(json(rep).get("statut").asText()).isEqualTo("FERME");
        assertThat(json(rep).get("id").asLong()).isEqualTo(id);
    }

    // 12) Fermeture d'un ticket EN_COURS : 200.
    @Test
    void scenario12_fermetureEnCours_retourne200() throws Exception {
        Long id = extraireId(postTicket("Ticket en cours", null));
        passeEnCours(id);
        ResponseEntity<String> rep = fermerTicket(id);
        assertThat(rep.getStatusCode().value()).isEqualTo(200);
        assertThat(json(rep).get("statut").asText()).isEqualTo("FERME");
    }

    // 13) Nouvelle fermeture d'un ticket FERME : 409 + RG-03.
    @Test
    void scenario13_reFermetureTicketFerme_retourne409avecRG03() throws Exception {
        Long id = extraireId(postTicket("Ticket deja ferme", null));
        assertThat(fermerTicket(id).getStatusCode().value()).isEqualTo(200);
        ResponseEntity<String> rep = fermerTicket(id);
        assertThat(rep.getStatusCode().value()).isEqualTo(409);
        JsonNode corps = json(rep);
        assertThat(corps.get("message").asText()).contains("RG-03");
        assertThat(corps.get("error").asText()).isEqualTo("CONFLIT_METIER");
    }

    // 14) Identifiant inconnu : 404.
    @Test
    void scenario14_identifiantInconnu_retourne404() throws Exception {
        ResponseEntity<String> rep = fermerTicket(9999L);
        assertThat(rep.getStatusCode().value()).isEqualTo(404);
        assertThat(json(rep).get("message").asText()).contains("9999");
        assertThat(json(rep).get("error").asText()).isEqualTo("RESSOURCE_INTROUVABLE");
    }

    // 15) Champs serveur non imposables par le client (id, statut, dateCreation ignores).
    @Test
    void scenario15_champsServeurNonImposables() throws Exception {
        HttpHeaders entetes = new HttpHeaders();
        entetes.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requete = new HttpEntity<>(corpsTicketInjection("Injection des champs serveur"), entetes);
        ResponseEntity<String> rep = rest.exchange(API, HttpMethod.POST, requete, String.class);
        assertThat(rep.getStatusCode().value()).isEqualTo(201);
        JsonNode corps = json(rep);
        assertThat(corps.get("id").asLong()).isNotEqualTo(9999);
        assertThat(corps.get("statut").asText()).isEqualTo("OUVERT");
        assertThat(corps.get("dateCreation").asText()).startsWith("20");
    }

    // 16) Conflit d'unicite detecte par la base : violation de la contrainte (sauvegarde directe).
    @Test
    void scenario16_conflitUniciteEnBase() throws Exception {
        postTicket("Titre unique", null);
        Ticket doublon = new Ticket("Titre unique", "le meme titre.");
        doublon.setStatut(StatutTicket.OUVERT);
        // La contrainte d'unicite definie en base permet pas de conserver deux tickets de meme titre.
        assertThatThrownBy(() -> {
            ticketRepository.saveAndFlush(doublon);
        }).isInstanceOf(Exception.class);
    }

    // ---------- Utilitaires internes ----------

    private Long extraireId(ResponseEntity<String> rep) throws Exception {
        return json(rep).get("id").asLong();
    }

    private int nbTickets(ResponseEntity<String> rep) throws Exception {
        return json(rep).size();
    }

    private void passeEnCours(Long id) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow();
        ticket.setStatut(StatutTicket.EN_COURS);
        ticketRepository.save(ticket);
    }
}
