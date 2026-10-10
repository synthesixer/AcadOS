package com.project.acados.service;

import com.project.acados.domain.entity.Room;
import com.project.acados.dto.request.RoomRequest;

import java.util.List;

/**
 * Service interface for Room management.
 * Reference: class diagram.puml, Implement_Plan-AcadOS.md §10.2, §16
 */
public interface RoomService {

    List<Room> getRooms();

    Room getRoom(Long id);

    Room createRoom(RoomRequest req);

    Room updateRoom(Long id, RoomRequest req);

    void deleteRoom(Long id);
}

