package com.university.reservation.service;

import com.university.reservation.dto.Dtos.BuildingResponse;
import com.university.reservation.dto.Dtos.EquipmentResponse;
import com.university.reservation.dto.Dtos.OrganizerResponse;
import com.university.reservation.dto.Dtos.OrganizerSummaryResponse;
import com.university.reservation.dto.Dtos.ReservationResponse;
import com.university.reservation.dto.Dtos.RoomResponse;
import com.university.reservation.dto.Dtos.RoomSummaryResponse;
import com.university.reservation.model.entity.Building;
import com.university.reservation.model.entity.Equipment;
import com.university.reservation.model.entity.Organizer;
import com.university.reservation.model.entity.Reservation;
import com.university.reservation.model.entity.Room;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MapperService {

    public BuildingResponse toDto(Building building) {
        return new BuildingResponse(building.getId(), building.getName(), building.getNumberOfFloors());
    }

    public EquipmentResponse toDto(Equipment equipment) {
        return new EquipmentResponse(equipment.getId(), equipment.getCode(), equipment.getLabel());
    }

    public RoomSummaryResponse toSummaryDto(Room room) {
        return new RoomSummaryResponse(
                room.getId(),
                room.getName(),
                toDto(room.getBuilding()),
                room.getFloor(),
                room.getCapacity(),
                room.getStatus()
        );
    }

    public RoomResponse toDto(Room room) {
        List<EquipmentResponse> equipment = room.getEquipments().stream()
                .sorted(Comparator.comparing(Equipment::getCode))
                .map(this::toDto)
                .collect(Collectors.toList());

        return new RoomResponse(
                room.getId(),
                room.getName(),
                toDto(room.getBuilding()),
                room.getFloor(),
                room.getCapacity(),
                room.getStatus(),
                equipment
        );
    }

    public OrganizerSummaryResponse toSummaryDto(Organizer organizer) {
        return new OrganizerSummaryResponse(
                organizer.getId(),
                organizer.getName(),
                toDto(organizer.getBuilding()),
                organizer.getFloor()
        );
    }

    public OrganizerResponse toDto(Organizer organizer) {
        return new OrganizerResponse(
                organizer.getId(),
                organizer.getName(),
                toDto(organizer.getBuilding()),
                organizer.getFloor(),
                organizer.getEmail()
        );
    }

    public ReservationResponse toDto(Reservation reservation) {
        List<String> equipmentCodes = reservation.getRequiredEquipments().stream()
                .map(Equipment::getCode)
                .sorted()
                .collect(Collectors.toList());

        return new ReservationResponse(
                reservation.getId(),
                reservation.getTitle(),
                reservation.getStatus(),
                toSummaryDto(reservation.getRoom()),
                toSummaryDto(reservation.getOrganizer()),
                reservation.getStart(),
                reservation.getEnd(),
                reservation.getNumberOfParticipants(),
                equipmentCodes,
                reservation.getCreatedAt()
        );
    }
}
