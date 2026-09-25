package com.university.reservation.controller;

import com.university.reservation.repository.BuildingRepository;
import com.university.reservation.repository.EquipmentRepository;
import com.university.reservation.repository.OrganizerRepository;
import com.university.reservation.repository.ReservationRepository;
import com.university.reservation.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private OrganizerRepository organizerRepository;

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @BeforeEach
    void cleanDatabase() {
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
        organizerRepository.deleteAll();
        buildingRepository.deleteAll();
        equipmentRepository.deleteAll();
    }

    @Test
    void givenValidBuilding_whenCreate_thenReturns201() throws Exception {
        mockMvc.perform(post("/api/buildings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Batiment Alpha",
                          "numberOfFloors": 5
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Batiment Alpha"))
                .andExpect(jsonPath("$.numberOfFloors").value(5));
    }

    @Test
    void givenExistingBuilding_whenCreateRoom_thenReturns201() throws Exception {
        long buildingId = createBuilding("Batiment Beta", 3);

        mockMvc.perform(post("/api/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Salle Orion",
                          "buildingId": %d,
                          "floor": 0,
                          "capacity": 30,
                          "equipmentCodes": []
                        }
                        """.formatted(buildingId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Salle Orion"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.capacity").value(30))
                .andExpect(jsonPath("$.equipment").isArray());
    }

    @Test
    void givenValidRoomAndOrganizer_whenCreateReservation_thenReturns201Confirmed() throws Exception {
        long buildingId = createBuilding("Batiment Reserv", 5);
        long roomId = createRoom("Salle Test", buildingId, 2, 30);
        long organizerId = createOrganizer("Bob Dupont", "bob@example.org", buildingId, 2);

        mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "Sprint Review",
                          "organizerId": %d,
                          "roomId": %d,
                          "start": "2027-01-15T10:00:00+01:00",
                          "end": "2027-01-15T11:00:00+01:00",
                          "numberOfParticipants": 20,
                          "requiredEquipmentCodes": []
                        }
                        """.formatted(organizerId, roomId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.title").value("Sprint Review"))
                .andExpect(jsonPath("$.room.id").value(roomId))
                .andExpect(jsonPath("$.organizer.id").value(organizerId))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void givenRoomAlreadyReserved_whenCreateConflictingReservation_thenReturns409() throws Exception {
        long buildingId = createBuilding("Batiment Conflit", 5);
        long roomId = createRoom("Salle Conflit", buildingId, 1, 30);
        long organizerId = createOrganizer("Carol Lee", "carol@example.org", buildingId, 1);

        mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "Premiere reunion",
                          "organizerId": %d,
                          "roomId": %d,
                          "start": "2027-03-10T14:00:00+01:00",
                          "end": "2027-03-10T16:00:00+01:00",
                          "numberOfParticipants": 10,
                          "requiredEquipmentCodes": []
                        }
                        """.formatted(organizerId, roomId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "Deuxieme reunion",
                          "organizerId": %d,
                          "roomId": %d,
                          "start": "2027-03-10T15:00:00+01:00",
                          "end": "2027-03-10T17:00:00+01:00",
                          "numberOfParticipants": 5,
                          "requiredEquipmentCodes": []
                        }
                        """.formatted(organizerId, roomId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROOM_ALREADY_RESERVED"));
    }

    @Test
    void givenConsecutiveReservations_whenBothCreated_thenBothAccepted() throws Exception {
        long buildingId = createBuilding("Batiment Consec", 4);
        long roomId = createRoom("Salle Consec", buildingId, 0, 20);
        long organizerId = createOrganizer("Dave Wilson", "dave@example.org", buildingId, 0);

        mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "Reunion 1",
                          "organizerId": %d,
                          "roomId": %d,
                          "start": "2027-04-20T10:00:00+02:00",
                          "end": "2027-04-20T11:00:00+02:00",
                          "numberOfParticipants": 10,
                          "requiredEquipmentCodes": []
                        }
                        """.formatted(organizerId, roomId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "Reunion 2",
                          "organizerId": %d,
                          "roomId": %d,
                          "start": "2027-04-20T11:00:00+02:00",
                          "end": "2027-04-20T12:00:00+02:00",
                          "numberOfParticipants": 10,
                          "requiredEquipmentCodes": []
                        }
                        """.formatted(organizerId, roomId)))
                .andExpect(status().isCreated());
    }

    @Test
    void givenCancelledReservation_whenNewReservationOnSamePeriod_thenAccepted() throws Exception {
        long buildingId = createBuilding("Batiment Cancel", 3);
        long roomId = createRoom("Salle Cancel", buildingId, 0, 20);
        long organizerId = createOrganizer("Eve Brown", "eve@example.org", buildingId, 0);

        MvcResult reservationResult = mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "Reunion a annuler",
                          "organizerId": %d,
                          "roomId": %d,
                          "start": "2027-05-05T09:00:00+02:00",
                          "end": "2027-05-05T10:00:00+02:00",
                          "numberOfParticipants": 10,
                          "requiredEquipmentCodes": []
                        }
                        """.formatted(organizerId, roomId)))
                .andExpect(status().isCreated())
                .andReturn();

        String reservationJson = reservationResult.getResponse().getContentAsString();
        long reservationId = Long.parseLong(reservationJson.split("\"id\":")[1].split(",")[0].trim());

        mockMvc.perform(patch("/api/reservations/" + reservationId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "Nouvelle reunion",
                          "organizerId": %d,
                          "roomId": %d,
                          "start": "2027-05-05T09:00:00+02:00",
                          "end": "2027-05-05T10:00:00+02:00",
                          "numberOfParticipants": 10,
                          "requiredEquipmentCodes": []
                        }
                        """.formatted(organizerId, roomId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void givenMultipleAvailableRooms_whenSearchAvailability_thenResultIsSortedCorrectly()
            throws Exception {
        long buildingId = createBuilding("Batiment Sort", 5);
        createRoom("Zenith", buildingId, 0, 25);
        createRoom("Alpha", buildingId, 0, 22);

        mockMvc.perform(get("/api/rooms/available")
                .param("start", "2027-07-01T10:00:00+02:00")
                .param("end", "2027-07-01T11:00:00+02:00")
                .param("capacity", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Alpha"))
                .andExpect(jsonPath("$[0].unusedCapacity").value(2))
                .andExpect(jsonPath("$[1].name").value("Zenith"))
                .andExpect(jsonPath("$[1].unusedCapacity").value(5));
    }

    @Test
    void givenInvalidBuildingRequest_whenCreate_thenReturnsValidationErrorFormat() throws Exception {
        mockMvc.perform(post("/api/buildings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "",
                          "numberOfFloors": 5
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").isString())
                .andExpect(jsonPath("$.fieldErrors").isMap())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void givenCompatibleRooms_whenAutomaticReservation_thenCreatesReservationWithScore()
            throws Exception {
        long buildingId = createBuilding("Batiment Auto", 4);
        createRoom("Salle Auto 1", buildingId, 1, 25);
        long organizerId = createOrganizer("Grace Kim", "grace@example.org", buildingId, 1);

        mockMvc.perform(post("/api/reservations/automatic")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "Reunion auto",
                          "organizerId": %d,
                          "start": "2027-08-01T14:00:00+02:00",
                          "end": "2027-08-01T15:00:00+02:00",
                          "numberOfParticipants": 20,
                          "requiredEquipmentCodes": []
                        }
                        """.formatted(organizerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.assignment").isMap())
                .andExpect(jsonPath("$.assignment.score").isNumber())
                .andExpect(jsonPath("$.assignment.distance").isNumber())
                .andExpect(jsonPath("$.assignment.unusedCapacity").isNumber());
    }

    private long createBuilding(String name, int floors) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/buildings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "%s", "numberOfFloors": %d}
                        """.formatted(name, floors)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result.getResponse().getContentAsString());
    }

    private long createRoom(String name, long buildingId, int floor, int capacity) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "%s",
                          "buildingId": %d,
                          "floor": %d,
                          "capacity": %d,
                          "equipmentCodes": []
                        }
                        """.formatted(name, buildingId, floor, capacity)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result.getResponse().getContentAsString());
    }

    private long createOrganizer(String name, String email, long buildingId, int floor)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/organizers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "%s",
                          "email": "%s",
                          "buildingId": %d,
                          "floor": %d
                        }
                        """.formatted(name, email, buildingId, floor)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result.getResponse().getContentAsString());
    }

    private long extractId(String json) {
        return Long.parseLong(json.split("\"id\":")[1].split("[,}]")[0].trim());
    }
}
