package com.project.acados.service;

import com.project.acados.domain.entity.Room;
import com.project.acados.dto.request.RoomRequest;
import com.project.acados.repository.RoomRepository;
import com.project.acados.service.impl.RoomServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomServiceImpl roomService;

    private Room sampleRoom;

    @BeforeEach
    void setUp() {
        sampleRoom = Room.builder()
                .id(1L)
                .building("SC01")
                .roomNumber("SC0101")
                .floor(1)
                .capacity(50)
                .isAvailable(true)
                .build();
    }

    @Test
    @DisplayName("getRooms: should return all rooms list")
    void testGetRooms() {
        when(roomRepository.findAll()).thenReturn(List.of(sampleRoom));

        List<Room> result = roomService.getRooms();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRoomNumber()).isEqualTo("SC0101");
        verify(roomRepository).findAll();
    }

    @Test
    @DisplayName("getRoom: should return room when id exists")
    void testGetRoom_Success() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(sampleRoom));

        Room result = roomService.getRoom(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getBuilding()).isEqualTo("SC01");
        verify(roomRepository).findById(1L);
    }

    @Test
    @DisplayName("getRoom: should throw NOT_FOUND when id does not exist")
    void testGetRoom_NotFound() {
        when(roomRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.getRoom(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Room not found with id: 999");

        verify(roomRepository).findById(999L);
    }

    @Test
    @DisplayName("createRoom: should save and return room when building and room number are unique")
    void testCreateRoom_Success() {
        RoomRequest req = RoomRequest.builder()
                .building("SC02")
                .roomNumber("SC0201")
                .floor(2)
                .capacity(60)
                .isAvailable(true)
                .build();

        when(roomRepository.existsByBuildingAndRoomNumber("SC02", "SC0201")).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> {
            Room r = invocation.getArgument(0);
            r.setId(2L);
            return r;
        });

        Room created = roomService.createRoom(req);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(2L);
        assertThat(created.getBuilding()).isEqualTo("SC02");
        assertThat(created.getRoomNumber()).isEqualTo("SC0201");
        assertThat(created.getCapacity()).isEqualTo(60);
        assertThat(created.getIsAvailable()).isTrue();
        verify(roomRepository).existsByBuildingAndRoomNumber("SC02", "SC0201");
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    @DisplayName("createRoom: should throw CONFLICT when building and room number already exist")
    void testCreateRoom_Conflict() {
        RoomRequest req = RoomRequest.builder()
                .building("SC01")
                .roomNumber("SC0101")
                .floor(1)
                .capacity(50)
                .build();

        when(roomRepository.existsByBuildingAndRoomNumber("SC01", "SC0101")).thenReturn(true);

        assertThatThrownBy(() -> roomService.createRoom(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Room already exists: SC01 SC0101");

        verify(roomRepository).existsByBuildingAndRoomNumber("SC01", "SC0101");
        verify(roomRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateRoom: should update and save room when room is not renamed or new name is unique")
    void testUpdateRoom_Success() {
        RoomRequest req = RoomRequest.builder()
                .building("SC01")
                .roomNumber("SC0101")
                .floor(1)
                .capacity(70)
                .isAvailable(false)
                .build();

        when(roomRepository.findById(1L)).thenReturn(Optional.of(sampleRoom));
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        Room updated = roomService.updateRoom(1L, req);

        assertThat(updated.getCapacity()).isEqualTo(70);
        assertThat(updated.getIsAvailable()).isFalse();
        verify(roomRepository).findById(1L);
        verify(roomRepository).save(sampleRoom);
    }

    @Test
    @DisplayName("updateRoom: should throw CONFLICT when renamed to an already existing room")
    void testUpdateRoom_Conflict() {
        RoomRequest req = RoomRequest.builder()
                .building("SC02")
                .roomNumber("SC0201")
                .floor(2)
                .capacity(60)
                .build();

        when(roomRepository.findById(1L)).thenReturn(Optional.of(sampleRoom));
        when(roomRepository.existsByBuildingAndRoomNumber("SC02", "SC0201")).thenReturn(true);

        assertThatThrownBy(() -> roomService.updateRoom(1L, req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Room already exists: SC02 SC0201");

        verify(roomRepository).findById(1L);
        verify(roomRepository).existsByBuildingAndRoomNumber("SC02", "SC0201");
        verify(roomRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteRoom: should delete existing room")
    void testDeleteRoom_Success() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(sampleRoom));

        roomService.deleteRoom(1L);

        verify(roomRepository).findById(1L);
        verify(roomRepository).delete(sampleRoom);
    }

    @Test
    @DisplayName("deleteRoom: should throw NOT_FOUND when room does not exist")
    void testDeleteRoom_NotFound() {
        when(roomRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.deleteRoom(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Room not found with id: 999");

        verify(roomRepository).findById(999L);
        verify(roomRepository, never()).delete(any());
    }
}

