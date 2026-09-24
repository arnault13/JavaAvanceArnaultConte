package com.university.reservation.controller;

import com.university.reservation.dto.Dtos.AvailableRoomResponse;
import com.university.reservation.dto.Dtos.CreateRoomRequest;
import com.university.reservation.dto.Dtos.ReplaceRoomEquipmentRequest;
import com.university.reservation.dto.Dtos.RoomResponse;
import com.university.reservation.dto.Dtos.UpdateRoomRequest;
import com.university.reservation.dto.Dtos.UpdateRoomStatusRequest;
import com.university.reservation.exception.ApiException;
import com.university.reservation.model.entity.Building;
import com.university.reservation.model.entity.Equipment;
import com.university.reservation.model.entity.Room;
import com.university.reservation.model.enums.RoomStatus;
import com.university.reservation.repository.BuildingRepository;
import com.university.reservation.repository.EquipmentRepository;
import com.university.reservation.repository.RoomRepository;
import com.university.reservation.service.MapperService;
import com.university.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomRepository roomRepository;
    private final BuildingRepository buildingRepository;
    private final EquipmentRepository equipmentRepository;
    private final ReservationService reservationService;
    private final MapperService mapperService;

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        roomRepository.findByNameIgnoreCase(request.name()).ifPresent(existing -> {
            throw new ApiException(
                    HttpStatus.CONFLICT, "RESOURCE_ALREADY_EXISTS",
                    "Une salle utilise deja ce nom",
                    Map.of());
        });

        Building building = buildingRepository.findById(request.buildingId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "BUILDING_NOT_FOUND",
                        "Le batiment " + request.buildingId() + " n'existe pas",
                        Map.of("buildingId", request.buildingId())));

        if (request.floor() >= building.getNumberOfFloors()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "L'etage " + request.floor() + " est invalide pour ce batiment",
                    Map.of());
        }

        List<Equipment> equipments = resolveEquipmentCodes(request.equipmentCodes());

        Room room = roomRepository.save(
                Room.builder()
                        .name(request.name())
                        .building(building)
                        .floor(request.floor())
                        .capacity(request.capacity())
                        .status(RoomStatus.AVAILABLE)
                        .equipments(new HashSet<>(equipments))
                        .build()
        );

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(room.getId())
                .toUri();

        return ResponseEntity.created(location).body(mapperService.toDto(room));
    }

    @GetMapping
    public List<RoomResponse> listRooms() {
        return roomRepository.findAll().stream()
                .sorted(Comparator.comparing(r -> r.getName().toLowerCase()))
                .map(mapperService::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/available")
    public List<AvailableRoomResponse> findAvailableRooms(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end,
            @RequestParam int capacity,
            @RequestParam(required = false) List<String> equipment) {
        return reservationService.findAvailableRooms(start, end, capacity, equipment);
    }

    @GetMapping("/{roomId}")
    public RoomResponse getRoom(@PathVariable Long roomId) {
        return roomRepository.findById(roomId)
                .map(mapperService::toDto)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND",
                        "La salle " + roomId + " n'existe pas",
                        Map.of("roomId", roomId)));
    }

    @PutMapping("/{roomId}")
    public RoomResponse updateRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody UpdateRoomRequest request) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND",
                        "La salle " + roomId + " n'existe pas",
                        Map.of("roomId", roomId)));

        roomRepository.findByNameIgnoreCase(request.name()).ifPresent(existing -> {
            if (!existing.getId().equals(roomId)) {
                throw new ApiException(
                        HttpStatus.CONFLICT, "RESOURCE_ALREADY_EXISTS",
                        "Une salle utilise deja ce nom",
                        Map.of());
            }
        });

        Building building = buildingRepository.findById(request.buildingId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "BUILDING_NOT_FOUND",
                        "Le batiment " + request.buildingId() + " n'existe pas",
                        Map.of("buildingId", request.buildingId())));

        if (request.floor() >= building.getNumberOfFloors()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "L'etage " + request.floor() + " est invalide pour ce batiment",
                    Map.of());
        }

        room.setName(request.name());
        room.setBuilding(building);
        room.setFloor(request.floor());
        room.setCapacity(request.capacity());
        return mapperService.toDto(roomRepository.save(room));
    }

    @PatchMapping("/{roomId}/status")
    public RoomResponse updateRoomStatus(
            @PathVariable Long roomId,
            @Valid @RequestBody UpdateRoomStatusRequest request) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND",
                        "La salle " + roomId + " n'existe pas",
                        Map.of("roomId", roomId)));

        room.setStatus(request.status());
        return mapperService.toDto(roomRepository.save(room));
    }

    @PutMapping("/{roomId}/equipment")
    public RoomResponse replaceRoomEquipment(
            @PathVariable Long roomId,
            @Valid @RequestBody ReplaceRoomEquipmentRequest request) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND",
                        "La salle " + roomId + " n'existe pas",
                        Map.of("roomId", roomId)));

        List<Equipment> equipments = resolveEquipmentCodes(request.equipmentCodes());
        room.setEquipments(new HashSet<>(equipments));
        return mapperService.toDto(roomRepository.save(room));
    }

    private List<Equipment> resolveEquipmentCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        List<Equipment> found = equipmentRepository.findByCodeIn(codes);
        if (found.size() != codes.size()) {
            String missingCode = codes.stream()
                    .filter(code -> found.stream().noneMatch(eq -> eq.getCode().equals(code)))
                    .findFirst().orElse("inconnu");
            throw new ApiException(
                    HttpStatus.NOT_FOUND, "EQUIPMENT_NOT_FOUND",
                    "L'equipement " + missingCode + " n'existe pas",
                    Map.of("equipmentCode", missingCode));
        }
        return found;
    }
}
