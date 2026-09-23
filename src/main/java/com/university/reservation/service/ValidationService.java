package com.university.reservation.service;

import com.university.reservation.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class ValidationService {

    public void validateReservationPeriod(OffsetDateTime start, OffsetDateTime end) {
        if (start == null || end == null) {
            return;
        }

        if (!start.isBefore(end)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_RESERVATION_PERIOD",
                    "La date de debut doit etre anterieure a la date de fin",
                    Map.of()
            );
        }

        if (!start.isAfter(OffsetDateTime.now())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_RESERVATION_PERIOD",
                    "La date de debut ne doit pas etre dans le passe",
                    Map.of()
            );
        }

        if (Duration.between(start, end).compareTo(Duration.ofHours(8)) > 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_RESERVATION_PERIOD",
                    "Une reservation ne peut pas depasser huit heures",
                    Map.of()
            );
        }
    }
}
