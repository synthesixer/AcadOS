package com.project.acados.repository;

import com.project.acados.domain.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Room entity.
 * Reference: class diagram.puml, Implement_Plan-AcadOS.md §7
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByBuildingAndRoomNumber(String building, String roomNumber);

    boolean existsByBuildingAndRoomNumber(String building, String roomNumber);

    List<Room> findByIsAvailableTrue();

    List<Room> findByCapacityGreaterThanEqualAndIsAvailableTrue(Integer capacity);
}

