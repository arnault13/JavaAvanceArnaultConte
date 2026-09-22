package com.university.reservation.repository;

import com.university.reservation.model.entity.Organizer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrganizerRepository extends JpaRepository<Organizer, Long> {

    Optional<Organizer> findByEmailIgnoreCase(String email);

    @Query("SELECT COALESCE(MAX(o.floor), -1) FROM Organizer o WHERE o.building.id = :buildingId")
    int findMaxFloorByBuildingId(@Param("buildingId") Long buildingId);
}
