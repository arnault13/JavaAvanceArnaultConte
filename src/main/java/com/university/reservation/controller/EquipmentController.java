package com.university.reservation.controller;

import com.university.reservation.dto.Dtos.CreateEquipmentRequest;
import com.university.reservation.dto.Dtos.EquipmentResponse;
import com.university.reservation.exception.ApiException;
import com.university.reservation.model.entity.Equipment;
import com.university.reservation.repository.EquipmentRepository;
import com.university.reservation.service.MapperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentRepository equipmentRepository;
    private final MapperService mapperService;

    @PostMapping
    public ResponseEntity<EquipmentResponse> createEquipment(
            @Valid @RequestBody CreateEquipmentRequest request) {

        equipmentRepository.findByCode(request.code()).ifPresent(existing -> {
            throw new ApiException(
                    HttpStatus.CONFLICT, "RESOURCE_ALREADY_EXISTS",
                    "Un equipement utilise deja ce code",
                    Map.of());
        });

        Equipment equipment = equipmentRepository.save(
                Equipment.builder()
                        .code(request.code())
                        .label(request.label())
                        .build()
        );

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(equipment.getId())
                .toUri();

        return ResponseEntity.created(location).body(mapperService.toDto(equipment));
    }

    @GetMapping
    public List<EquipmentResponse> listEquipment() {
        return equipmentRepository.findAll().stream()
                .sorted(Comparator.comparing(Equipment::getCode))
                .map(mapperService::toDto)
                .collect(Collectors.toList());
    }
}
