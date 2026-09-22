package com.university.reservation.repository;

import com.university.reservation.model.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("SELECT COUNT(r) FROM Reservation r " +
           "WHERE r.room.id = :roomId " +
           "AND r.status = 'CONFIRMED' " +
           "AND r.start < :end " +
           "AND r.end > :start")
    long countOverlappingConfirmedReservations(
            @Param("roomId") Long roomId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end);

    @Query("SELECT r FROM Reservation r " +
           "WHERE r.room.id = :roomId " +
           "AND r.status = 'CONFIRMED' " +
           "AND r.start < :end " +
           "AND r.end > :start " +
           "ORDER BY r.id ASC")
    List<Reservation> findOverlappingConfirmedReservations(
            @Param("roomId") Long roomId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end);

    @Query("SELECT r FROM Reservation r " +
           "WHERE (:roomId IS NULL OR r.room.id = :roomId) " +
           "AND (:organizerId IS NULL OR r.organizer.id = :organizerId) " +
           "AND (:from IS NULL OR r.end > :from) " +
           "AND (:to IS NULL OR r.start < :to) " +
           "ORDER BY r.start ASC, r.id ASC")
    List<Reservation> findWithFilters(
            @Param("roomId") Long roomId,
            @Param("organizerId") Long organizerId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);
}
