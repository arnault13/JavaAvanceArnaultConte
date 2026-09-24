package com.university.reservation.controller;

import com.university.reservation.dto.Dtos.AutomaticReservationRequest;
import com.university.reservation.dto.Dtos.AutomaticReservationResponse;
import com.university.reservation.dto.Dtos.CreateReservationRequest;
import com.university.reservation.dto.Dtos.ReservationResponse;
import com.university.reservation.exception.ApiException;
import com.university.reservation.repository.ReservationRepository;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationRepository reservationRepository;
    private final MapperService mapperService;

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request) {

        ReservationResponse response = reservationService.createReservation(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/automatic")
    public ResponseEntity<AutomaticReservationResponse> createAutomaticReservation(
            @Valid @RequestBody AutomaticReservationRequest request) {

        AutomaticReservationResponse response = reservationService.createAutomaticReservation(request);

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/reservations/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<ReservationResponse> listReservations(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long organizerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {

        if (from != null && to != null && !from.isBefore(to)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "INVALID_RESERVATION_PERIOD",
                    "Le parametre from doit etre anterieur au parametre to",
                    Map.of());
        }

        return reservationRepository.findWithFilters(roomId, organizerId, from, to).stream()
                .map(mapperService::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{reservationId}")
    public ReservationResponse getReservation(@PathVariable Long reservationId) {
        return reservationRepository.findById(reservationId)
                .map(mapperService::toDto)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND",
                        "La reservation " + reservationId + " n'existe pas",
                        Map.of("reservationId", reservationId)));
    }

    @PatchMapping("/{reservationId}/cancel")
    public ReservationResponse cancelReservation(@PathVariable Long reservationId) {
        return reservationService.cancelReservation(reservationId);
    }
}
