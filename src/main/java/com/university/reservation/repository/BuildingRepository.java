package com.university.reservation.repository;

import com.university.reservation.model.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BuildingRepository extends JpaRepository<Building, Long> {
    Optional<Building> findByNameIgnoreCase(String name);
}
