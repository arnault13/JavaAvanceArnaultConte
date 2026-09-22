package com.university.reservation.model.entity;

import com.university.reservation.model.enums.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "reservation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "organizer_id", nullable = false)
    private Organizer organizer;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime start;

    @Column(name = "end_time", nullable = false)
    private OffsetDateTime end;

    @Column(nullable = false)
    private Integer numberOfParticipants;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "reservation_equipment",
        joinColumns = @JoinColumn(name = "reservation_id"),
        inverseJoinColumns = @JoinColumn(name = "equipment_id")
    )
    @Builder.Default
    private Set<Equipment> requiredEquipments = new HashSet<>();

    @Column(nullable = false)
    private OffsetDateTime createdAt;
}
