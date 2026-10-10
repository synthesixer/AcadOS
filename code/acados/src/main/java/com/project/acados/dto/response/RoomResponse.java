package com.project.acados.dto.response;

import com.project.acados.domain.entity.Room;
import lombok.*;

/**
 * Response DTO for Room entity.
 * Reference: class diagram.puml, Implement_Plan-AcadOS.md §16
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {

    private Long id;
    private String roomNumber;
    private String building;
    private Integer floor;
    private Integer capacity;
    private Boolean isAvailable;

    public static RoomResponse fromEntity(Room room) {
        if (room == null) {
            return null;
        }
        return RoomResponse.builder()
                .id(room.getId())
                .roomNumber(room.getRoomNumber())
                .building(room.getBuilding())
                .floor(room.getFloor())
                .capacity(room.getCapacity())
                .isAvailable(room.getIsAvailable())
                .build();
    }
}

