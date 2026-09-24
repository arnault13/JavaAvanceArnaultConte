package com.university.reservation.controller;

import com.university.reservation.dto.Dtos.CreateOrganizerRequest;
import com.university.reservation.dto.Dtos.OrganizerResponse;
import com.university.reservation.exception.ApiException;
import com.university.reservation.model.entity.Building;
import com.university.reservation.model.entity.Organizer;
import com.university.reservation.repository.BuildingRepository;
import com.university.reservation.repository.OrganizerRepository;
import com.university.reservation.service.MapperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
@RequestMapping("/api/organizers")
@RequiredArgsConstructor
public class OrganizerController {

    private final OrganizerRepository organizerRepository;
    private final BuildingRepository buildingRepository;
    private final MapperService mapperService;

    @PostMapping
    public ResponseEntity<OrganizerResponse> createOrganizer(
            @Valid @RequestBody CreateOrganizerRequest request) {

        organizerRepository.findByEmailIgnoreCase(request.email()).ifPresent(existing -> {
            throw new ApiException(
                    HttpStatus.CONFLICT, "RESOURCE_ALREADY_EXISTS",
                    "Un organisateur utilise deja cette adresse e-mail",
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

        Organizer organizer = organizerRepository.save(
                Organizer.builder()
                        .name(request.name())
                        .email(request.email())
                        .building(building)
                        .floor(request.floor())
                        .build()
        );

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(organizer.getId())
                .toUri();

        return ResponseEntity.created(location).body(mapperService.toDto(organizer));
    }

    @GetMapping
    public List<OrganizerResponse> listOrganizers() {
        return organizerRepository.findAll().stream()
                .sorted(Comparator.comparing((Organizer o) -> o.getName().toLowerCase())
                        .thenComparing(Organizer::getId))
                .map(mapperService::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{organizerId}")
    public OrganizerResponse getOrganizer(@PathVariable Long organizerId) {
        return organizerRepository.findById(organizerId)
                .map(mapperService::toDto)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "ORGANIZER_NOT_FOUND",
                        "L'organisateur " + organizerId + " n'existe pas",
                        Map.of("organizerId", organizerId)));
    }
}
