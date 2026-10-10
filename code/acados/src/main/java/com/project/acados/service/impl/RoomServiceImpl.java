package com.project.acados.service.impl;

import com.project.acados.domain.entity.Room;
import com.project.acados.dto.request.RoomRequest;
import com.project.acados.repository.RoomRepository;
import com.project.acados.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Implementation of RoomService.
 * Reference: class diagram.puml, Implement_Plan-AcadOS.md §10.2, §16
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Room> getRooms() {
        return roomRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Room getRoom(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found with id: " + id));
    }

    @Override
    public Room createRoom(RoomRequest req) {
        if (roomRepository.existsByBuildingAndRoomNumber(req.getBuilding(), req.getRoomNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("Room already exists: %s %s", req.getBuilding(), req.getRoomNumber()));
        }

        Room room = Room.builder()
                .building(req.getBuilding())
                .roomNumber(req.getRoomNumber())
                .floor(req.getFloor())
                .capacity(req.getCapacity())
                .isAvailable(req.getIsAvailable() != null ? req.getIsAvailable() : true)
                .build();

        return roomRepository.save(room);
    }

    @Override
    public Room updateRoom(Long id, RoomRequest req) {
        Room room = getRoom(id);

        boolean isRenamed = !room.getBuilding().equalsIgnoreCase(req.getBuilding())
                || !room.getRoomNumber().equalsIgnoreCase(req.getRoomNumber());

        if (isRenamed && roomRepository.existsByBuildingAndRoomNumber(req.getBuilding(), req.getRoomNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("Room already exists: %s %s", req.getBuilding(), req.getRoomNumber()));
        }

        room.setBuilding(req.getBuilding());
        room.setRoomNumber(req.getRoomNumber());
        room.setFloor(req.getFloor());
        room.setCapacity(req.getCapacity());
        if (req.getIsAvailable() != null) {
            room.setIsAvailable(req.getIsAvailable());
        }

        return roomRepository.save(room);
    }

    @Override
    public void deleteRoom(Long id) {
        Room room = getRoom(id);
        roomRepository.delete(room);
    }
}

