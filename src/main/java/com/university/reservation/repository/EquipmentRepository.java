package com.university.reservation.repository;

import com.university.reservation.model.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    Optional<Equipment> findByCode(String code);
    List<Equipment> findByCodeIn(List<String> codes);
}
