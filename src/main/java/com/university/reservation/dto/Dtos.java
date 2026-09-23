package com.university.reservation.dto;

import com.university.reservation.model.enums.ReservationStatus;
import com.university.reservation.model.enums.RoomStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public final class Dtos {

    private Dtos() {}

    public record CreateBuildingRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Min(1) @Max(200) Integer numberOfFloors
    ) {}

    public record UpdateBuildingRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Min(1) @Max(200) Integer numberOfFloors
    ) {}

    public record BuildingResponse(Long id, String name, Integer numberOfFloors) {}

    public record CreateEquipmentRequest(
        @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,49}$") String code,
        @NotBlank @Size(max = 100) String label
    ) {}

    public record EquipmentResponse(Long id, String code, String label) {}

    public record CreateRoomRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Min(1) Long buildingId,
        @NotNull @Min(0) @Max(199) Integer floor,
        @NotNull @Min(1) Integer capacity,
        List<@Pattern(regexp = "^[A-Z][A-Z0-9_]{1,49}$") String> equipmentCodes
    ) {}

    public record UpdateRoomRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Min(1) Long buildingId,
        @NotNull @Min(0) @Max(199) Integer floor,
        @NotNull @Min(1) Integer capacity
    ) {}

    public record UpdateRoomStatusRequest(@NotNull RoomStatus status) {}

    public record ReplaceRoomEquipmentRequest(
        @NotNull List<@Pattern(regexp = "^[A-Z][A-Z0-9_]{1,49}$") String> equipmentCodes
    ) {}

    public record RoomSummaryResponse(
        Long id, String name, BuildingResponse building, Integer floor, Integer capacity, RoomStatus status
    ) {}

    public record RoomResponse(
        Long id, String name, BuildingResponse building, Integer floor,
        Integer capacity, RoomStatus status, List<EquipmentResponse> equipment
    ) {}

    public record AvailableRoomResponse(
        Long id, String name, BuildingResponse building, Integer floor,
        Integer capacity, RoomStatus status, List<EquipmentResponse> equipment, Integer unusedCapacity
    ) {}

    public record CreateOrganizerRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull @Min(1) Long buildingId,
        @NotNull @Min(0) @Max(199) Integer floor
    ) {}

    public record OrganizerSummaryResponse(Long id, String name, BuildingResponse building, Integer floor) {}

    public record OrganizerResponse(Long id, String name, BuildingResponse building, Integer floor, String email) {}

    public record CreateReservationRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull @Min(1) Long organizerId,
        @NotNull OffsetDateTime start,
        @NotNull OffsetDateTime end,
        @NotNull @Min(1) Integer numberOfParticipants,
        List<@Pattern(regexp = "^[A-Z][A-Z0-9_]{1,49}$") String> requiredEquipmentCodes,
        @NotNull @Min(1) Long roomId
    ) {}

    public record AutomaticReservationRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull @Min(1) Long organizerId,
        @NotNull OffsetDateTime start,
        @NotNull OffsetDateTime end,
        @NotNull @Min(1) Integer numberOfParticipants,
        List<@Pattern(regexp = "^[A-Z][A-Z0-9_]{1,49}$") String> requiredEquipmentCodes
    ) {}

    public record ReservationResponse(
        Long id, String title, ReservationStatus status,
        RoomSummaryResponse room, OrganizerSummaryResponse organizer,
        OffsetDateTime start, OffsetDateTime end,
        Integer numberOfParticipants, List<String> requiredEquipmentCodes,
        OffsetDateTime createdAt
    ) {}

    public record RoomAssignmentScore(Integer distance, Integer unusedCapacity, Long score) {}

    public record AutomaticReservationResponse(
        Long id, String title, ReservationStatus status,
        RoomSummaryResponse room, OrganizerSummaryResponse organizer,
        OffsetDateTime start, OffsetDateTime end,
        Integer numberOfParticipants, List<String> requiredEquipmentCodes,
        OffsetDateTime createdAt, RoomAssignmentScore assignment
    ) {}
}
