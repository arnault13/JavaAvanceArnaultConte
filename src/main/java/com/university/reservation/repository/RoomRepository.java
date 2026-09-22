package com.university.reservation.repository;

import com.university.reservation.model.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByNameIgnoreCase(String name);

    @Query("SELECT COALESCE(MAX(r.floor), -1) FROM Room r WHERE r.building.id = :buildingId")
    int findMaxFloorByBuildingId(@Param("buildingId") Long buildingId);
}
