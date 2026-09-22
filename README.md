# Système de réservation de salles

Application backend REST de gestion et de réservation de salles de réunion.

## Prérequis

- Java 17
- Maven 3.8+

## Lancement

```bash
mvn spring-boot:run
```

L'application démarre sur `http://localhost:8080`.  
La console H2 est accessible sur `http://localhost:8080/h2-console` avec l'URL JDBC `jdbc:h2:mem:reservationdb`.

## Endpoints

### Bâtiments
- `POST /api/buildings` : créer un bâtiment
- `GET /api/buildings` : liste des bâtiments
- `GET /api/buildings/{id}` : détail d'un bâtiment
- `PUT /api/buildings/{id}` : modifier un bâtiment

### Salles
- `POST /api/rooms` : créer une salle
- `GET /api/rooms` : liste des salles
- `GET /api/rooms/available` : rechercher les salles disponibles
- `GET /api/rooms/{id}` : détail d'une salle
- `PUT /api/rooms/{id}` : modifier une salle
- `PATCH /api/rooms/{id}/status` : modifier le statut (AVAILABLE / MAINTENANCE)
- `PUT /api/rooms/{id}/equipment` : modifier les équipements d'une salle

### Équipements
- `POST /api/equipment` : ajouter un équipement
- `GET /api/equipment` : liste des équipements

### Organisateurs
- `POST /api/organizers` : créer un organisateur
- `GET /api/organizers` : liste des organisateurs
- `GET /api/organizers/{id}` : détail d'un organisateur

### Réservations
- `POST /api/reservations` : réserver une salle spécifique
- `POST /api/reservations/automatic` : attribution automatique de salle
- `GET /api/reservations` : liste et filtres (roomId, organizerId, from, to)
- `GET /api/reservations/{id}` : détail d'une réservation
- `PATCH /api/reservations/{id}/cancel` : annuler une réservation

## Tests

```bash
mvn test
```
