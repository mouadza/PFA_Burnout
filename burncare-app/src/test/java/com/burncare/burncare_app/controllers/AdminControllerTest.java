package com.burncare.burncare_app.controllers;

import com.burncare.burncare_app.dto.UserDTO;
import com.burncare.burncare_app.entities.Role;
import com.burncare.burncare_app.entities.User;
import com.burncare.burncare_app.services.AdminService;
import com.burncare.burncare_app.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// On teste uniquement AdminController
@WebMvcTest(controllers = AdminController.class)
// On désactive la sécurité (Keycloak) pour tester juste la logique du contrôleur
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper; // Pour convertir les objets Java en JSON

    @Test
    void getAllUsers_ShouldReturnListOfUsers() throws Exception {
        // ARRANGE
        User u1 = new User(); u1.setId(1L); u1.setFirstName("Alice"); u1.setEmail("alice@test.com");
        User u2 = new User(); u2.setId(2L); u2.setFirstName("Bob"); u2.setEmail("bob@test.com");
        List<User> users = Arrays.asList(u1, u2);

        when(adminService.getAllUsers()).thenReturn(users);

        // ACT & ASSERT
        mockMvc.perform(get("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].email").value("alice@test.com"));
    }

    @Test
    void updateUser_ShouldReturnUpdatedUser() throws Exception {
        // ARRANGE
        Long userId = 1L;
        UserDTO userDTO = new UserDTO();
        userDTO.setFirstName("AliceUpdated");
        userDTO.setRole("ADMIN");
        userDTO.setEnabled(true);

        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setFirstName("AliceUpdated");
        updatedUser.setRole(Role.ADMIN);

        when(userService.updateUser(eq(userId), any(UserDTO.class))).thenReturn(updatedUser);

        // ACT & ASSERT
        mockMvc.perform(put("/api/admin/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("AliceUpdated"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void deleteUser_ShouldReturnNoContent() throws Exception {
        // ARRANGE
        Long userId = 123L;
        doNothing().when(adminService).deleteUser(userId);

        // ACT & ASSERT
        mockMvc.perform(delete("/api/admin/users/{id}", userId))
                .andExpect(status().isNoContent()); // Code 204
    }
}