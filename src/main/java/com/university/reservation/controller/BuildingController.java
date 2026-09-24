package com.university.reservation.controller;

import com.university.reservation.dto.Dtos.BuildingResponse;
import com.university.reservation.dto.Dtos.CreateBuildingRequest;
import com.university.reservation.dto.Dtos.UpdateBuildingRequest;
import com.university.reservation.exception.ApiException;
import com.university.reservation.model.entity.Building;
import com.university.reservation.repository.BuildingRepository;
import com.university.reservation.repository.OrganizerRepository;
import com.university.reservation.repository.RoomRepository;
import com.university.reservation.service.MapperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/buildings")
@RequiredArgsConstructor
public class BuildingController {

    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;
    private final OrganizerRepository organizerRepository;
    private final MapperService mapperService;

    @PostMapping
    public ResponseEntity<BuildingResponse> createBuilding(@Valid @RequestBody CreateBuildingRequest request) {
        buildingRepository.findByNameIgnoreCase(request.name()).ifPresent(existing -> {
            throw new ApiException(
                    HttpStatus.CONFLICT, "RESOURCE_ALREADY_EXISTS",
                    "Un batiment utilise deja ce nom",
                    Map.of());
        });

        Building building = buildingRepository.save(
                Building.builder()
                        .name(request.name())
                        .numberOfFloors(request.numberOfFloors())
                        .build()
        );

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(building.getId())
                .toUri();

        return ResponseEntity.created(location).body(mapperService.toDto(building));
    }

    @GetMapping
    public List<BuildingResponse> listBuildings() {
        return buildingRepository.findAll().stream()
                .sorted(Comparator.comparing(b -> b.getName().toLowerCase()))
                .map(mapperService::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{buildingId}")
    public BuildingResponse getBuilding(@PathVariable Long buildingId) {
        return buildingRepository.findById(buildingId)
                .map(mapperService::toDto)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "BUILDING_NOT_FOUND",
                        "Le batiment " + buildingId + " n'existe pas",
                        Map.of("buildingId", buildingId)));
    }

    @PutMapping("/{buildingId}")
    public BuildingResponse updateBuilding(
            @PathVariable Long buildingId,
            @Valid @RequestBody UpdateBuildingRequest request) {

        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "BUILDING_NOT_FOUND",
                        "Le batiment " + buildingId + " n'existe pas",
                        Map.of("buildingId", buildingId)));

        buildingRepository.findByNameIgnoreCase(request.name()).ifPresent(existing -> {
            if (!existing.getId().equals(buildingId)) {
                throw new ApiException(
                        HttpStatus.CONFLICT, "RESOURCE_ALREADY_EXISTS",
                        "Un batiment utilise deja ce nom",
                        Map.of());
            }
        });

        int maxRoomFloor = roomRepository.findMaxFloorByBuildingId(buildingId);
        int maxOrganizerFloor = organizerRepository.findMaxFloorByBuildingId(buildingId);
        int highestOccupiedFloor = Math.max(maxRoomFloor, maxOrganizerFloor);

        if (highestOccupiedFloor >= request.numberOfFloors()) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "BUILDING_FLOOR_COUNT_CONFLICT",
                    "Une salle ou un organisateur occupe un etage qui serait supprime",
                    Map.of("highestOccupiedFloor", highestOccupiedFloor));
        }

        building.setName(request.name());
        building.setNumberOfFloors(request.numberOfFloors());
        return mapperService.toDto(buildingRepository.save(building));
    }
}
