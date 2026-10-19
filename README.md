# Gestion des tickets de support

Application full stack réalisée pour l'épreuve de rattrapage **KFOKAM48 — Batch 2 — Phase 3**, par **jordane237**.

Elle comprend :

- un **backend Java 21 / Spring Boot** avec PostgreSQL, exposant trois endpoints REST ;
- un **frontend React (TypeScript)** entièrement en français, connecté à l'API réelle ;
- une base **PostgreSQL** dans un conteneur Docker (aucune installation locale nécessaire) ;
- la documentation interactive **Swagger UI** (springdoc OpenAPI) ;
- le suivi Git sur la branche `rattrapage/jordane237` et une Pull Request vers `main`.

---

## 1. Structure du projet

```
.
├── backend/                          # API Spring Boot (Java 21, Maven)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/jordane237/ticketmanagement/
│       │   ├── entity/               # Ticket, StatutTicket (JPA)
│       │   ├── repository/           # Accès aux données (Spring Data JPA)
│       │   ├── service/              # Logique métier (RG-01, RG-02, RG-03)
│       │   ├── controller/           # Endpoints REST
│       │   ├── dto/                  # DTO d'entrée (TicketCreateRequest) et de sortie (TicketResponse)
│       │   ├── exception/            # Exceptions métier + GlobalExceptionHandler (@RestControllerAdvice)
│       │   └── config/               # OpenAPI (Swagger) + CORS
│       └── test/java/com/jordane237/ticketmanagement/
│           ├── api/                  # 16 tests d'intégration
│           └── config/               # TestcontainersConfig (PostgreSQL réel)
├── frontend/                         # Interface React + TypeScript (Vite)
│   ├── src/
│   │   ├── api/ticketService.ts        # Service API dédié (seul point d'accès HTTP)
│   │   ├── components/
│   │   │   ├── PageTickets.tsx         # Conteneur (états : chargement/données/vide/erreur)
│   │   │   ├── ListeTickets.tsx        # Tableau des tickets
│   │   │   ├── FormulaireTicket.tsx    # Formulaire de création
│   │   │   ├── FiltrageTickets.tsx     # Filtre Tous / OUVERT / EN_COURS / FERME
│   │   │   └── BadgeStatut.tsx         # Badges de statut
│   │   └── types.ts                  # Types TypeScript du domaine
├── compose.yaml                      # PostgreSQL en conteneur Docker
├── .env.example                      # Variables d'environnement à copier vers .env
└── docs/captures/                    # Captures d'écran authentiques
```

L'entité JPA n'est jamais renvoyée par les endpoints : les réponses utilisent le DTO `TicketResponse`, et la création utilise le DTO d'entrée `TicketCreateRequest`. Aucune logique métier ne se trouve dans le contrôleur.

---

## 2. Modèle de données

Table PostgreSQL : **`tickets`** (schéma `public`).

| Colonne | Type | Règles |
|---|---|---|
| `id` | `bigint` | Généré automatiquement (séquence) |
| `titre` | `varchar(100)` | Obligatoire, **unique** (contrainte en base) |
| `description` | `text` | Facultative |
| `statut` | `varchar(20)` | `OUVERT` (initial), `EN_COURS`, `FERME` |
| `date_creation` | `timestamp` | Générée par le serveur (`@PrePersist`), non modifiable |

Le client ne peut transmettre que `titre` et `description` : l'identifiant, le statut et la date de création transmis dans la requête sont **ignorés** (test 15).

---

## 3. Prérequis

- **Docker** + **Docker Compose** (aucune installation locale de PostgreSQL n'est nécessaire)
- **JDK 21** et **Maven 3.8+**
- **Node.js 20+** et **npm**

---

## 4. Lancement (commandes exactes)

### 4.1 Base PostgreSQL (Docker)

```bash
cp .env.example .env          # première fois uniquement
docker compose up -d
docker compose ps             # ticket-postgres doit être "healthy"
```

Variables configurées (voir `.env.example`) : base `ticket_db`, utilisateur `ticket_user`, port hôte **5433**, volume persistant `ticket_postgres_data`, healthcheck `pg_isready`.

### 4.2 Backend (port 8080)

```bash
cd backend
mvn spring-boot:run
```

Code source 100% écrit par l'agent (aucun fichier pré-généré externe). Le datasource se configure via des variables facultatives (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`) dont les valeurs par défaut correspondent au compose ci-dessus.

### 4.3 Frontend (port 5173)

```bash
cd frontend
npm install                   # première fois uniquement
npm run dev
```

Interface : <http://localhost:5173> — les appels `/api/**` sont redirigés par le proxy Vite vers <http://localhost:8080>.

### 4.4 Swagger UI

Ouvrir : <http://localhost:8080/swagger-ui.html>

La documentation décrit les trois endpoints, les paramètres, les DTO, ainsi que les réponses de succès (201/200) et d'erreur (400/404/409).

---

## 5. API

| Méthode | URL | Description | Succès | Erreurs |
|---|---|---|---|---|
| `POST` | `/api/tickets` | Créer un ticket (`titre`, `description` facultative) | **201** + ticket créé | **400** RG-01, **409** RG-02 |
| `GET` | `/api/tickets` | Lister tous les tickets | **200** (tableau JSON, vide possible) | **400** si `statut` invalide |
| `GET` | `/api/tickets?statut=OUVERT\|EN_COURS\|FERME` | Filtrage côté backend | **200** | **400** |
| `PATCH` | `/api/tickets/{id}/fermer` | Fermer un ticket (RG-03) | **200** + ticket `FERME` | **404** inconnu, **409** déjà `FERME` |

Exemple de création :

```json
POST /api/tickets
{
  "titre": "Problème de connexion",
  "description": "Impossible de se connecter au service"
}
```

Format commun de toutes les erreurs :

```json
{
  "status": 409,
  "error": "CONFLIT_METIER",
  "message": "RG-02 : un ticket avec ce titre existe deja.",
  "timestamp": "2026-10-19T13:15:32"
}
```

---

## 6. Règles métier

- **RG-01 — validation du titre** : obligatoire, au maximum 100 caractères, non vide et pas seulement des espaces ; sinon **HTTP 400** avec un message précis (« Le titre est obligatoire. », « Le titre ne doit pas depasser 100 caracteres. »). La validation ne se fait que **côté serveur** — volontairement, le formulaire n'impose aucune limite de saisie pour laisser le serveur renvoyer son refus 400.
- **RG-02 — unicité du titre** : deux tickets ne peuvent pas avoir le même titre (comparaison stricte et uniforme : contrainte `UNIQUE` en base + vérification dans le service). **HTTP 409** avec le message « RG-02 : un ticket avec ce titre existe deja. » Les violations détectées par la base (`DataIntegrityViolationException`) sont également converties en HTTP 409 RG-02.
- **RG-03 — fermeture** : un ticket `OUVERT` ou `EN_COURS` peut être fermé ; un ticket `FERME` ne peut plus changer d'état (**HTTP 409** avec le message « RG-03 : ce ticket est deja ferme et ne peut plus changer d'etat. »). Identifiant inconnu ⇒ **HTTP 404**. La règle est implémentée dans le service, protégée par des transactions `@Transactional`.

Aucun endpoint de modification libre du statut n'existe (aucun `POST /{id}` ou `PUT` de statut).

---

## 7. Vérification SQL (persistance réelle)

1. Créer un ticket depuis Swagger (`POST /api/tickets`), par exemple `titre = "Ticket facture doublee"`.
2. Se connecter à la base **dans le conteneur** :

```bash
docker compose exec postgres psql -U ticket_user -d ticket_db
```

3. Vérifier que le ticket est bien présent :

```sql
SELECT id, titre, description, statut, date_creation
FROM tickets
ORDER BY date_creation DESC;
```

Nom réel de la table : **`tickets`** (déclaré avec `@Table(name = "tickets")`).

Vérification effectuée le 2026-10-19 (résultat réel) :

```
 id |        titre         |      description      | statut |       date_creation
----+----------------------+-----------------------+--------+----------------------------
  2 | Connexion WiFi instable | Deconnexions repetees au bureau 204 | OUVERT | 2026-10-19 13:18:54.859312
  1 | Ticket demo KFOKAM48 | Cree via Swagger/curl | FERME  | 2026-10-19 13:15:31.646361
```

---

## 8. Tests

### 8.1 Backend (16 tests d'intégration, PostgreSQL réel)

```bash
cd backend
mvn test
```

Les tests s'exécutent contre une **vraie base PostgreSQL** créée par **Testcontainers** (aucun H2) et couvrent les 16 scénarios demandés :

| # | Scénario | Résultat |
|---|---|---|
| 1 | Création valide → 201 + corps complet | ✅ |
| 2 | Statut initial `OUVERT` | ✅ |
| 3 | `dateCreation` générée côté serveur | ✅ |
| 4 | Titre manquant → 400 | ✅ |
| 5 | Titre composé d'espaces → 400 | ✅ |
| 6 | Titre > 100 caractères → 400 | ✅ |
| 7 | Titre en double → 409 + `RG-02` | ✅ |
| 8 | Liste complète → 200 (tableau JSON) | ✅ |
| 9 | Filtre par chacun des statuts | ✅ |
| 10 | Statut de filtre invalide → 400 explicite | ✅ |
| 11 | Fermeture d'un `OUVERT` → 200 | ✅ |
| 12 | Fermeture d'un `EN_COURS` → 200 | ✅ |
| 13 | Re-fermeture d'un `FERME` → 409 + `RG-03` | ✅ |
| 14 | Identifiant inconnu → 404 | ✅ |
| 15 | Champs serveur (id/statut/date) non imposables par le client | ✅ |
| 16 | Conflit d'unicité détecté par la base | ✅ |

Dernière exécution : **Tests run: 16, Failures: 0, Errors: 0** (BUILD SUCCESS).

> Remarque d'environnement : Docker 29 exige une API client ≥ 1.40 ; le fichier `~/.docker-java.properties` contenant `api.version=1.44` permet à Testcontainers 1.x de fonctionner (problème connu et documenté par Testcontainers).

### 8.2 Frontend

```bash
cd frontend
npm run build     # vérifie le typage TypeScript et le build de production
```

Scénarios d'usage vérifiés sur l'interface : chargement, liste, liste vide (« Aucun ticket »), erreur réseau (backend arrêté) avec bouton **Réessayer**, création, refus 400 et 409 affichés avec les messages du serveur, filtre par statut, fermeture avec confirmation, refus 409 RG-03 affiché.

---

## 9. Captures d'écran (authentiques, prises avec Chrome headless)

| Fichier | Contenu |
|---|---|
| `docs/captures/swagger-ui.png` | Swagger UI avec les trois endpoints |
| `docs/captures/interface-tickets.png` | Interface React avec la liste et les filtres |
| `docs/captures/interface-refus-409.png` | Interface React affichant le refus **HTTP 409 / RG-02** (« un ticket avec ce titre existe deja ») après une soumission en doublon |

---

## 10. Erreurs courantes

| Symptôme | Cause | Solution |
|---|---|---|
| Backend : `Connection to localhost:5433 refused` | Conteneur PostgreSQL arrêté | `docker compose up -d` puis vérifier `docker compose ps` |
| Port 5433 déjà utilisé sur votre machine | Autre service sur ce port | Modifier `POSTGRES_PORT` dans `.env` et redémarrer (`docker compose down && docker compose up -d`) |
| Port 8080 occupé | Autre application Java | Lancer avec `SERVER_PORT=8081 mvn spring-boot:run` |
| Frontend : « Le serveur est injoignable » | Backend non démarré | Démarrer le backend (`mvn spring-boot:run`) puis cliquer **Réessayer** |
| `Connection 8080 refused` sur Swagger | Backend pas encore démarré | Attendre la fin du démarrage Spring Boot |
| Testcontainers : `client version 1.32 is too old` | Docker ≥ 29 et API minimale 1.40/1.44 | Créer `~/.docker-java.properties` avec `api.version=1.44` |

---

## 11. Procédure de démonstration (devant le formateur)

1. `docker compose up -d` → PostgreSQL démarré (`docker compose ps` : healthy).
2. `cd backend && mvn spring-boot:run` → backend sur le port 8080.
3. `cd frontend && npm run dev` → interface sur le port 5173.
4. Ouvrir <http://localhost:8080/swagger-ui.html> : montrer les trois endpoints.
5. Créer un ticket dans Swagger (`POST /api/tickets`).
6. `docker compose exec postgres psql -U ticket_user -d ticket_db -c "SELECT id, titre, statut FROM tickets;"` → le ticket est en base.
7. Rafraîchir l'interface <http://localhost:5173> : le ticket apparaît.
8. Créer un ticket depuis le formulaire → il s'ajoute sans recharger la page.
9. Resoumettre le **même titre** → le message **409 RG-02** du serveur s'affiche près du formulaire.
10. Soumettre un titre de plus de 100 caractères → message **400** du serveur.
11. Utiliser les quatre filtres (Tous / OUVERT / EN_COURS / FERME).
12. Fermer un ticket `OUVERT` (confirmation) → devient `FERME`, liste mise à jour.
13. (Un ticket déjà `FERME`) ne montre **aucun** bouton de fermeture ; une fermeture forcée par API renvoie **409 RG-03**.
14. `curl -i -X PATCH http://localhost:8080/api/tickets/9999/fermer` → **404**.
15. Arrêter le backend (Ctrl+C) → l'interface affiche « Le serveur est injoignable… » + bouton **Réessayer** ; redémarrer le backend et cliquer **Réessayer** → récupération.
16. Montrer la vérification SQL (à nouveau) et la branche/Pull Request sur GitHub.

---

## 12. Git

- Dépôt : <https://github.com/jordane237/ticket-management>
- Branche de travail : `rattrapage/jordane237` → Pull Request vers `main`.
- Commits : voir l'historique de la branche (backend / frontend + Docker / docs + tests).
