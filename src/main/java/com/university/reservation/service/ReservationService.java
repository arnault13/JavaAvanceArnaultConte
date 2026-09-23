package com.university.reservation.service;

import com.university.reservation.dto.Dtos.AutomaticReservationRequest;
import com.university.reservation.dto.Dtos.AutomaticReservationResponse;
import com.university.reservation.dto.Dtos.AvailableRoomResponse;
import com.university.reservation.dto.Dtos.CreateReservationRequest;
import com.university.reservation.dto.Dtos.ReservationResponse;
import com.university.reservation.dto.Dtos.RoomAssignmentScore;
import com.university.reservation.exception.ApiException;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final OrganizerRepository organizerRepository;
    private final EquipmentRepository equipmentRepository;
    private final ValidationService validationService;
    private final MapperService mapperService;

    @Transactional(readOnly = true)
    public List<AvailableRoomResponse> findAvailableRooms(
            OffsetDateTime start, OffsetDateTime end, int capacity, List<String> equipmentCodes) {

        validationService.validateReservationPeriod(start, end);
        List<Equipment> requiredEquipments = resolveEquipmentCodes(equipmentCodes);

        return roomRepository.findAll().stream()
                .filter(room -> room.getStatus() == RoomStatus.AVAILABLE)
                .filter(room -> room.getCapacity() >= capacity)
                .filter(room -> room.getEquipments().containsAll(requiredEquipments))
                .filter(room -> reservationRepository.countOverlappingConfirmedReservations(
                        room.getId(), start, end) == 0)
                .map(room -> {
                    List<com.university.reservation.dto.Dtos.EquipmentResponse> equipmentList =
                            room.getEquipments().stream()
                                    .sorted(Comparator.comparing(Equipment::getCode))
                                    .map(mapperService::toDto)
                                    .collect(Collectors.toList());
                    return new AvailableRoomResponse(
                            room.getId(),
                            room.getName(),
                            mapperService.toDto(room.getBuilding()),
                            room.getFloor(),
                            room.getCapacity(),
                            room.getStatus(),
                            equipmentList,
                            room.getCapacity() - capacity
                    );
                })
                .sorted(Comparator.comparing(AvailableRoomResponse::unusedCapacity)
                        .thenComparing(r -> r.name().toLowerCase())
                        .thenComparing(AvailableRoomResponse::id))
                .collect(Collectors.toList());
    }

    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request) {
        validationService.validateReservationPeriod(request.start(), request.end());

        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND",
                        "La salle " + request.roomId() + " n'existe pas",
                        Map.of("roomId", request.roomId())));

        Organizer organizer = organizerRepository.findById(request.organizerId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "ORGANIZER_NOT_FOUND",
                        "L'organisateur " + request.organizerId() + " n'existe pas",
                        Map.of("organizerId", request.organizerId())));

        List<Equipment> requiredEquipments = resolveEquipmentCodes(request.requiredEquipmentCodes());
        checkRoomEligibility(room, request.numberOfParticipants(), requiredEquipments, request.start(), request.end());

        Reservation reservation = Reservation.builder()
                .title(request.title())
                .status(ReservationStatus.CONFIRMED)
                .room(room)
                .organizer(organizer)
                .start(request.start())
                .end(request.end())
                .numberOfParticipants(request.numberOfParticipants())
                .requiredEquipments(new HashSet<>(requiredEquipments))
                .createdAt(OffsetDateTime.now())
                .build();

        return mapperService.toDto(reservationRepository.save(reservation));
    }

    @Transactional
    public AutomaticReservationResponse createAutomaticReservation(AutomaticReservationRequest request) {
        validationService.validateReservationPeriod(request.start(), request.end());

        Organizer organizer = organizerRepository.findById(request.organizerId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "ORGANIZER_NOT_FOUND",
                        "L'organisateur " + request.organizerId() + " n'existe pas",
                        Map.of("organizerId", request.organizerId())));

        List<Equipment> requiredEquipments = resolveEquipmentCodes(request.requiredEquipmentCodes());

        record RoomScore(Room room, int distance, int unusedCapacity, long score) {}

        List<RoomScore> candidates = roomRepository.findAll().stream()
                .filter(room -> room.getStatus() == RoomStatus.AVAILABLE)
                .filter(room -> room.getCapacity() >= request.numberOfParticipants())
                .filter(room -> room.getEquipments().containsAll(requiredEquipments))
                .filter(room -> reservationRepository.countOverlappingConfirmedReservations(
                        room.getId(), request.start(), request.end()) == 0)
                .map(room -> {
                    int unusedCapacity = room.getCapacity() - request.numberOfParticipants();
                    int distance = calculateDistance(room, organizer);
                    long score = (long) distance * 10 + unusedCapacity;
                    return new RoomScore(room, distance, unusedCapacity, score);
                })
                .sorted(Comparator.comparing(RoomScore::score)
                        .thenComparing(rs -> rs.room().getName().toLowerCase())
                        .thenComparing(rs -> rs.room().getId()))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "NO_COMPATIBLE_ROOM",
                    "Aucune salle disponible ne correspond aux criteres demandes",
                    Map.of());
        }

        RoomScore best = candidates.get(0);

        Reservation reservation = Reservation.builder()
                .title(request.title())
                .status(ReservationStatus.CONFIRMED)
                .room(best.room())
                .organizer(organizer)
                .start(request.start())
                .end(request.end())
                .numberOfParticipants(request.numberOfParticipants())
                .requiredEquipments(new HashSet<>(requiredEquipments))
                .createdAt(OffsetDateTime.now())
                .build();

        reservationRepository.save(reservation);

        ReservationResponse base = mapperService.toDto(reservation);
        RoomAssignmentScore assignmentScore = new RoomAssignmentScore(
                best.distance(), best.unusedCapacity(), best.score());

        return new AutomaticReservationResponse(
                base.id(), base.title(), base.status(), base.room(), base.organizer(),
                base.start(), base.end(), base.numberOfParticipants(),
                base.requiredEquipmentCodes(), base.createdAt(), assignmentScore
        );
    }

    @Transactional
    public ReservationResponse cancelReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND",
                        "La reservation " + reservationId + " n'existe pas",
                        Map.of("reservationId", reservationId)));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "RESERVATION_ALREADY_CANCELLED",
                    "La reservation " + reservationId + " est deja annulee",
                    Map.of("reservationId", reservationId));
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        return mapperService.toDto(reservationRepository.save(reservation));
    }

    public int calculateDistance(Room room, Organizer organizer) {
        int floorDiff = Math.abs(room.getFloor() - organizer.getFloor());
        if (room.getBuilding().getId().equals(organizer.getBuilding().getId())) {
            return floorDiff;
        }
        return 10 + floorDiff;
    }

    private void checkRoomEligibility(Room room, int numberOfParticipants,
                                       List<Equipment> requiredEquipments,
                                       OffsetDateTime start, OffsetDateTime end) {
        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "ROOM_UNAVAILABLE",
                    "La salle " + room.getName() + " est en maintenance",
                    Map.of("roomId", room.getId()));
        }

        if (room.getCapacity() < numberOfParticipants) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "ROOM_CAPACITY_EXCEEDED",
                    "La capacite de la salle est insuffisante",
                    Map.of("roomId", room.getId(),
                            "roomCapacity", room.getCapacity(),
                            "numberOfParticipants", numberOfParticipants));
        }

        List<String> missingCodes = requiredEquipments.stream()
                .filter(eq -> !room.getEquipments().contains(eq))
                .map(Equipment::getCode)
                .collect(Collectors.toList());

        if (!missingCodes.isEmpty()) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "MISSING_REQUIRED_EQUIPMENT",
                    "La salle ne possede pas tous les equipements demandes",
                    Map.of("roomId", room.getId(), "missingEquipmentCodes", missingCodes));
        }

        List<Reservation> conflicts = reservationRepository
                .findOverlappingConfirmedReservations(room.getId(), start, end);

        if (!conflicts.isEmpty()) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "ROOM_ALREADY_RESERVED",
                    "La salle " + room.getName() + " est deja reservee sur cette periode",
                    Map.of("roomId", room.getId(),
                            "conflictingReservationId", conflicts.get(0).getId()));
        }
    }

    private List<Equipment> resolveEquipmentCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }

        List<Equipment> found = equipmentRepository.findByCodeIn(codes);

        if (found.size() != codes.size()) {
            String missingCode = codes.stream()
                    .filter(code -> found.stream().noneMatch(eq -> eq.getCode().equals(code)))
                    .findFirst()
                    .orElse("inconnu");
            throw new ApiException(
                    HttpStatus.NOT_FOUND, "EQUIPMENT_NOT_FOUND",
                    "L'equipement " + missingCode + " n'existe pas",
                    Map.of("equipmentCode", missingCode));
        }

        return found;
    }
}
