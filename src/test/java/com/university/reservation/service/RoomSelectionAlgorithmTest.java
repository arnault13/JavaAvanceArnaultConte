package com.university.reservation.service;

import com.university.reservation.dto.Dtos.AutomaticReservationRequest;
import com.university.reservation.dto.Dtos.AutomaticReservationResponse;
import com.university.reservation.exception.ApiException;
import com.university.reservation.model.entity.Building;
import com.university.reservation.model.entity.Equipment;
import com.university.reservation.model.entity.Organizer;
import com.university.reservation.model.entity.Reservation;
import com.university.reservation.model.entity.Room;
import com.university.reservation.model.enums.ReservationStatus;
import com.university.reservation.model.enums.RoomStatus;
import com.university.reservation.repository.EquipmentRepository;
import com.university.reservation.repository.OrganizerRepository;
import com.university.reservation.repository.ReservationRepository;
import com.university.reservation.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomSelectionAlgorithmTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private OrganizerRepository organizerRepository;
    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private ValidationService validationService;
    @Mock
    private MapperService mapperService;

    @InjectMocks
    private ReservationService reservationService;

    private Building buildingA;
    private Building buildingB;
    private Organizer organizer;
    private OffsetDateTime futureStart;
    private OffsetDateTime futureEnd;

    @BeforeEach
    void setUp() {
        buildingA = Building.builder().id(1L).name("Batiment A").numberOfFloors(10).build();
        buildingB = Building.builder().id(2L).name("Batiment B").numberOfFloors(10).build();

        organizer = Organizer.builder().id(1L).name("Alice Martin")
                .email("alice@example.org").building(buildingA).floor(3).build();

        futureStart = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        futureEnd = futureStart.plusHours(2);
    }

    @Test
    void givenSameBuildingRoom_whenCalculateDistance_thenEqualsFloorDifference() {
        Room room = buildRoom(1L, "Salle A", buildingA, 5, 30, RoomStatus.AVAILABLE, Set.of());

        int distance = reservationService.calculateDistance(room, organizer);

        assertThat(distance).isEqualTo(2);
    }

    @Test
    void givenDifferentBuildingRoom_whenCalculateDistance_thenAddsTenPenalty() {
        Room room = buildRoom(1L, "Salle B", buildingB, 3, 30, RoomStatus.AVAILABLE, Set.of());

        int distance = reservationService.calculateDistance(room, organizer);

        assertThat(distance).isEqualTo(10);
    }

    @Test
    void givenDifferentBuildingAndFloor_whenCalculateDistance_thenPenaltyPlusFloorDiff() {
        Room room = buildRoom(1L, "Salle C", buildingB, 7, 30, RoomStatus.AVAILABLE, Set.of());

        int distance = reservationService.calculateDistance(room, organizer);

        assertThat(distance).isEqualTo(14);
    }

    @Test
    void givenRoomWithExactCapacity_whenAutomaticReservation_thenRoomIsSelected() {
        Room room = buildRoom(1L, "Orion", buildingA, 3, 20, RoomStatus.AVAILABLE, Set.of());
        Reservation savedReservation = buildSavedReservation(room, organizer);

        when(organizerRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(roomRepository.findAll()).thenReturn(List.of(room));
        when(reservationRepository.countOverlappingConfirmedReservations(any(), any(), any()))
                .thenReturn(0L);
        when(reservationRepository.save(any())).thenReturn(savedReservation);
        when(mapperService.toDto(any(Reservation.class))).thenCallRealMethod();
        when(mapperService.toSummaryDto(any(Room.class))).thenCallRealMethod();
        when(mapperService.toSummaryDto(any(Organizer.class))).thenCallRealMethod();
        when(mapperService.toDto(any(Building.class))).thenCallRealMethod();

        AutomaticReservationRequest request = new AutomaticReservationRequest(
                "Reunion", 1L, futureStart, futureEnd, 20, List.of());

        AutomaticReservationResponse response = reservationService.createAutomaticReservation(request);

        assertThat(response).isNotNull();
        assertThat(response.assignment().unusedCapacity()).isEqualTo(0);
    }

    @Test
    void givenRoomTooSmall_whenAutomaticReservation_thenThrowsNoCompatibleRoom() {
        Room room = buildRoom(1L, "Petite salle", buildingA, 3, 10, RoomStatus.AVAILABLE, Set.of());

        when(organizerRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(roomRepository.findAll()).thenReturn(List.of(room));

        AutomaticReservationRequest request = new AutomaticReservationRequest(
                "Reunion", 1L, futureStart, futureEnd, 20, List.of());

        assertThatThrownBy(() -> reservationService.createAutomaticReservation(request))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("NO_COMPATIBLE_ROOM");
    }

    @Test
    void givenRoomInMaintenance_whenAutomaticReservation_thenRoomExcluded() {
        Room room = buildRoom(1L, "Salle fermee", buildingA, 3, 30, RoomStatus.MAINTENANCE, Set.of());

        when(organizerRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(roomRepository.findAll()).thenReturn(List.of(room));

        AutomaticReservationRequest request = new AutomaticReservationRequest(
                "Reunion", 1L, futureStart, futureEnd, 20, List.of());

        assertThatThrownBy(() -> reservationService.createAutomaticReservation(request))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("NO_COMPATIBLE_ROOM");
    }

    @Test
    void givenRoomAlreadyReserved_whenAutomaticReservation_thenRoomExcluded() {
        Room room = buildRoom(1L, "Salle occupee", buildingA, 3, 30, RoomStatus.AVAILABLE, Set.of());

        when(organizerRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(roomRepository.findAll()).thenReturn(List.of(room));
        when(reservationRepository.countOverlappingConfirmedReservations(any(), any(), any()))
                .thenReturn(1L);

        AutomaticReservationRequest request = new AutomaticReservationRequest(
                "Reunion", 1L, futureStart, futureEnd, 20, List.of());

        assertThatThrownBy(() -> reservationService.createAutomaticReservation(request))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("NO_COMPATIBLE_ROOM");
    }

    @Test
    void givenRoomMissingEquipment_whenAutomaticReservation_thenRoomExcluded() {
        Equipment projector = Equipment.builder().id(1L).code("PROJECTOR").label("Projecteur").build();
        Room room = buildRoom(1L, "Salle sans equipement", buildingA, 3, 30, RoomStatus.AVAILABLE, Set.of());

        when(organizerRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(roomRepository.findAll()).thenReturn(List.of(room));
        when(equipmentRepository.findByCodeIn(List.of("PROJECTOR"))).thenReturn(List.of(projector));

        AutomaticReservationRequest request = new AutomaticReservationRequest(
                "Reunion", 1L, futureStart, futureEnd, 20, List.of("PROJECTOR"));

        assertThatThrownBy(() -> reservationService.createAutomaticReservation(request))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("NO_COMPATIBLE_ROOM");
    }

    @Test
    void givenTwoRoomsWithDifferentScores_whenAutomaticReservation_thenSelectsLowestScore() {
        Room roomA = buildRoom(1L, "Salle A", buildingA, 3, 22, RoomStatus.AVAILABLE, Set.of());
        Room roomB = buildRoom(2L, "Salle B", buildingA, 6, 20, RoomStatus.AVAILABLE, Set.of());

        Reservation savedReservation = buildSavedReservation(roomA, organizer);

        when(organizerRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(roomRepository.findAll()).thenReturn(List.of(roomB, roomA));
        when(reservationRepository.countOverlappingConfirmedReservations(any(), any(), any()))
                .thenReturn(0L);
        when(reservationRepository.save(any())).thenReturn(savedReservation);
        when(mapperService.toDto(any(Reservation.class))).thenCallRealMethod();
        when(mapperService.toSummaryDto(any(Room.class))).thenCallRealMethod();
        when(mapperService.toSummaryDto(any(Organizer.class))).thenCallRealMethod();
        when(mapperService.toDto(any(Building.class))).thenCallRealMethod();

        AutomaticReservationRequest request = new AutomaticReservationRequest(
                "Reunion", 1L, futureStart, futureEnd, 20, List.of());

        AutomaticReservationResponse response = reservationService.createAutomaticReservation(request);

        assertThat(response.assignment().score()).isEqualTo(2L);
        assertThat(response.assignment().distance()).isEqualTo(0);
        assertThat(response.assignment().unusedCapacity()).isEqualTo(2);
    }

    @Test
    void givenTwoRoomsWithEqualScore_whenAutomaticReservation_thenAlphabeticOrderDecides() {
        Room roomZephyr = buildRoom(1L, "Zephyr", buildingA, 3, 20, RoomStatus.AVAILABLE, Set.of());
        Room roomApollo = buildRoom(2L, "Apollo", buildingA, 3, 20, RoomStatus.AVAILABLE, Set.of());

        Reservation savedReservation = buildSavedReservation(roomApollo, organizer);

        when(organizerRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(roomRepository.findAll()).thenReturn(List.of(roomZephyr, roomApollo));
        when(reservationRepository.countOverlappingConfirmedReservations(any(), any(), any()))
                .thenReturn(0L);
        when(reservationRepository.save(any())).thenReturn(savedReservation);
        when(mapperService.toDto(any(Reservation.class))).thenCallRealMethod();
        when(mapperService.toSummaryDto(any(Room.class))).thenCallRealMethod();
        when(mapperService.toSummaryDto(any(Organizer.class))).thenCallRealMethod();
        when(mapperService.toDto(any(Building.class))).thenCallRealMethod();

        AutomaticReservationRequest request = new AutomaticReservationRequest(
                "Reunion", 1L, futureStart, futureEnd, 20, List.of());

        AutomaticReservationResponse response = reservationService.createAutomaticReservation(request);

        assertThat(response.room().id()).isEqualTo(2L);
    }

    @Test
    void givenTwoRoomsWithEqualScoreAndName_whenAutomaticReservation_thenLowestIdDecides() {
        Room room1 = buildRoom(1L, "orion", buildingA, 3, 20, RoomStatus.AVAILABLE, Set.of());
        Room room2 = buildRoom(2L, "ORION", buildingA, 3, 20, RoomStatus.AVAILABLE, Set.of());

        Reservation savedReservation = buildSavedReservation(room1, organizer);

        when(organizerRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(roomRepository.findAll()).thenReturn(List.of(room2, room1));
        when(reservationRepository.countOverlappingConfirmedReservations(any(), any(), any()))
                .thenReturn(0L);
        when(reservationRepository.save(any())).thenReturn(savedReservation);
        when(mapperService.toDto(any(Reservation.class))).thenCallRealMethod();
        when(mapperService.toSummaryDto(any(Room.class))).thenCallRealMethod();
        when(mapperService.toSummaryDto(any(Organizer.class))).thenCallRealMethod();
        when(mapperService.toDto(any(Building.class))).thenCallRealMethod();

        AutomaticReservationRequest request = new AutomaticReservationRequest(
                "Reunion", 1L, futureStart, futureEnd, 20, List.of());

        AutomaticReservationResponse response = reservationService.createAutomaticReservation(request);

        assertThat(response.room().id()).isEqualTo(1L);
    }

    @Test
    void givenNoCompatibleRoom_whenAutomaticReservation_thenThrowsNoCompatibleRoom() {
        when(organizerRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(roomRepository.findAll()).thenReturn(List.of());

        AutomaticReservationRequest request = new AutomaticReservationRequest(
                "Reunion", 1L, futureStart, futureEnd, 20, List.of());

        assertThatThrownBy(() -> reservationService.createAutomaticReservation(request))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("NO_COMPATIBLE_ROOM");
    }

    private Room buildRoom(Long id, String name, Building building, int floor,
                           int capacity, RoomStatus status, Set<Equipment> equipments) {
        return Room.builder()
                .id(id)
                .name(name)
                .building(building)
                .floor(floor)
                .capacity(capacity)
                .status(status)
                .equipments(new HashSet<>(equipments))
                .build();
    }

    private Reservation buildSavedReservation(Room room, Organizer org) {
        return Reservation.builder()
                .id(99L)
                .title("Reunion")
                .status(ReservationStatus.CONFIRMED)
                .room(room)
                .organizer(org)
                .start(futureStart)
                .end(futureEnd)
                .numberOfParticipants(20)
                .requiredEquipments(new HashSet<>())
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
    }
}
