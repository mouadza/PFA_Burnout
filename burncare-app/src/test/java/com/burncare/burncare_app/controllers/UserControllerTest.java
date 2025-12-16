package com.burncare.burncare_app.controllers;

import com.burncare.burncare_app.dto.AuthResponse;
import com.burncare.burncare_app.dto.ChangePasswordRequest;
import com.burncare.burncare_app.dto.UpdateProfileRequest;
import com.burncare.burncare_app.entities.Profession;
import com.burncare.burncare_app.entities.Role;
import com.burncare.burncare_app.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false) // On désactive la sécurité pour le test unitaire
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void updateProfile_ShouldReturnUpdatedResponse() throws Exception {
        // ARRANGE
        // Simulation de la requête (Adaptez le constructeur selon votre DTO réel)
        UpdateProfileRequest request = new UpdateProfileRequest("jean@test.com", "Jean", "NouveauNom");

        // Simulation de la réponse attendue
        AuthResponse response = new AuthResponse(
                "token-gardé",
                "Jean",
                "NouveauNom",
                "jean@test.com",
                Role.USER,
                Profession.MEDECIN
        );

        when(userService.updateProfile(any(UpdateProfileRequest.class))).thenReturn(response);

        // ACT & ASSERT
        mockMvc.perform(put("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("NouveauNom"))
                .andExpect(jsonPath("$.email").value("jean@test.com"));
    }

    @Test
    void changePassword_ShouldReturnOk() throws Exception {
        // ARRANGE
        // Simulation de la requête
        ChangePasswordRequest request = new ChangePasswordRequest("jean@test.com", "NewPass123!");

        // Le service ne retourne rien (void), on ne fait rien
        doNothing().when(userService).changePassword(any(ChangePasswordRequest.class));

        // ACT & ASSERT
        mockMvc.perform(put("/api/user/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()); // Vérifie que le code HTTP est 200
    }
}