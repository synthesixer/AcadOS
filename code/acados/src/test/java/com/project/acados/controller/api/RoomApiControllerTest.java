package com.project.acados.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.acados.domain.entity.Room;
import com.project.acados.dto.request.RoomRequest;
import com.project.acados.security.TokenProvider;
import com.project.acados.service.RoomService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoomApiController.class)
@org.springframework.context.annotation.Import(com.project.acados.config.SecurityConfig.class)
@AutoConfigureMockMvc
@DisplayName("RoomApiController Unit Tests")
class RoomApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoomService roomService;

    @MockBean
    private TokenProvider tokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("GET /api/v1/rooms: should return room list")
    void getRoomsShouldReturnList() throws Exception {
        Room room = Room.builder()
                .id(1L)
                .building("SC-01")
                .roomNumber("401")
                .floor(4)
                .capacity(50)
                .isAvailable(true)
                .build();

        when(roomService.getRooms()).thenReturn(List.of(room));

        mockMvc.perform(get("/api/v1/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].building").value("SC-01"))
                .andExpect(jsonPath("$[0].roomNumber").value("401"))
                .andExpect(jsonPath("$[0].capacity").value(50));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/rooms: ADMIN can create room")
    void adminCanCreateRoom() throws Exception {
        RoomRequest request = RoomRequest.builder()
                .building("SC-02")
                .roomNumber("305")
                .floor(3)
                .capacity(60)
                .isAvailable(true)
                .build();

        Room created = Room.builder()
                .id(2L)
                .building("SC-02")
                .roomNumber("305")
                .floor(3)
                .capacity(60)
                .isAvailable(true)
                .build();

        when(roomService.createRoom(any(RoomRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/rooms")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.roomNumber").value("305"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/v1/rooms/{id}: ADMIN can update room")
    void adminCanUpdateRoom() throws Exception {
        RoomRequest request = RoomRequest.builder()
                .building("SC-01")
                .roomNumber("401")
                .floor(4)
                .capacity(55)
                .isAvailable(false)
                .build();

        Room updated = Room.builder()
                .id(1L)
                .building("SC-01")
                .roomNumber("401")
                .floor(4)
                .capacity(55)
                .isAvailable(false)
                .build();

        when(roomService.updateRoom(eq(1L), any(RoomRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/rooms/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(55))
                .andExpect(jsonPath("$.isAvailable").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/v1/rooms/{id}: ADMIN can delete room")
    void adminCanDeleteRoom() throws Exception {
        doNothing().when(roomService).deleteRoom(1L);

        mockMvc.perform(delete("/api/v1/rooms/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(roomService).deleteRoom(1L);
    }
}

