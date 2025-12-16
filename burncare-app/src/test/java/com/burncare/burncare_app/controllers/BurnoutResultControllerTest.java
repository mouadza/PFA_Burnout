package com.burncare.burncare_app.controllers;

import com.burncare.burncare_app.dto.BurnoutResultRequest;
import com.burncare.burncare_app.dto.BurnoutResultResponse;
import com.burncare.burncare_app.services.BurnoutResultService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BurnoutResultController.class)
@AutoConfigureMockMvc
class BurnoutResultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BurnoutResultService burnoutResultService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void save_ShouldReturnSavedResult() throws Exception {
        // ARRANGE
        String keycloakId = "user-123";

        BurnoutResultRequest request = new BurnoutResultRequest(
                65,
                "Élevé",
                "Risque Élevé",
                "Attention...",
                "Consultez...",
                Arrays.asList(3, 3, 4)
        );

        BurnoutResultResponse response = new BurnoutResultResponse(
                1L,
                65,
                "Élevé",
                "Risque Élevé",
                "2025-01-01"
        );

        when(burnoutResultService.saveForUser(eq(keycloakId), any(BurnoutResultRequest.class)))
                .thenReturn(response);

        // ACT & ASSERT
        // ✅ CORRECTION : Utilisation de .jwt(builder -> builder.subject(...))
        mockMvc.perform(post("/api/burnout-results")
                        .with(jwt().jwt(builder -> builder.subject(keycloakId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.burnoutScore").value(65));
    }

    @Test
    void getMyResults_ShouldReturnList() throws Exception {
        // ARRANGE
        String keycloakId = "user-123";

        BurnoutResultResponse r1 = new BurnoutResultResponse(1L, 65, "Élevé", "Titre1", "2025-01-01");
        BurnoutResultResponse r2 = new BurnoutResultResponse(2L, 30, "Faible", "Titre2", "2025-01-02");
        List<BurnoutResultResponse> results = Arrays.asList(r1, r2);

        when(burnoutResultService.getResultsForUser(keycloakId)).thenReturn(results);

        // ACT & ASSERT
        // ✅ CORRECTION ICI AUSSI
        mockMvc.perform(get("/api/burnout-results/me")
                        .with(jwt().jwt(builder -> builder.subject(keycloakId)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].burnoutScore").value(65));
    }

    @Test
    void getMyResults_ShouldReturn401_WhenNotAuthenticated() throws Exception {
        // ACT & ASSERT
        // Pas de token -> 401
        mockMvc.perform(get("/api/burnout-results/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}