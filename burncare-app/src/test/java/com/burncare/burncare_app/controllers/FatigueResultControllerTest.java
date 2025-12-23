package com.burncare.burncare_app.controllers;

import com.burncare.burncare_app.dto.FatigueResultRequest;
import com.burncare.burncare_app.dto.FatigueResultResponse;
import com.burncare.burncare_app.services.FatigueResultService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FatigueResultController.class)
class FatigueResultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FatigueResultService fatigueResultService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String KEYCLOAK_ID = "test-user-123";

    @Test
    void shouldSaveFatigueResult() throws Exception {
        // ARRANGE
        FatigueResultRequest request = new FatigueResultRequest();
        request.setFatigueScore(75);
        request.setRiskLabel("Élevé");
        request.setRiskTitle("Risque Élevé");
        request.setMessage("Faites attention");
        request.setConfidence(0.95);
        request.setRecommendationText("Prenez des vacances");

        FatigueResultResponse response = FatigueResultResponse.builder()
                .id(1L)
                .fatigueScore(75)
                .riskLabel("Élevé")
                .riskTitle("Risque Élevé")
                .message("Faites attention")
                .confidence(0.95)
                .recommendationText("Prenez des vacances")
                .createdAt(Instant.now())
                .build();

        when(fatigueResultService.saveForUser(eq(KEYCLOAK_ID), any(FatigueResultRequest.class)))
                .thenReturn(response);

        // ACT & ASSERT
        mockMvc.perform(post("/api/fatigue-results")
                        .with(jwt().jwt(jwt -> jwt.subject(KEYCLOAK_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.fatigueScore").value(75))
                .andExpect(jsonPath("$.riskLabel").value("Élevé"))
                .andExpect(jsonPath("$.riskTitle").value("Risque Élevé"))
                .andExpect(jsonPath("$.message").value("Faites attention"))
                .andExpect(jsonPath("$.confidence").value(0.95))
                .andExpect(jsonPath("$.recommendationText").value("Prenez des vacances"));
    }

    @Test
    void shouldGetMyFatigueResults() throws Exception {
        // ARRANGE
        FatigueResultResponse response1 = FatigueResultResponse.builder()
                .id(1L)
                .fatigueScore(75)
                .riskLabel("Élevé")
                .createdAt(Instant.now())
                .build();

        FatigueResultResponse response2 = FatigueResultResponse.builder()
                .id(2L)
                .fatigueScore(50)
                .riskLabel("Moyen")
                .createdAt(Instant.now().minusSeconds(3600))
                .build();

        List<FatigueResultResponse> responses = Arrays.asList(response1, response2);

        when(fatigueResultService.getResultsForUser(KEYCLOAK_ID))
                .thenReturn(responses);

        // ACT & ASSERT
        mockMvc.perform(get("/api/fatigue-results/me")
                        .with(jwt().jwt(jwt -> jwt.subject(KEYCLOAK_ID)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].fatigueScore").value(75))
                .andExpect(jsonPath("$[0].riskLabel").value("Élevé"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].fatigueScore").value(50))
                .andExpect(jsonPath("$[1].riskLabel").value("Moyen"));
    }

    @Test
    void shouldReturnEmptyListWhenNoResults() throws Exception {
        // ARRANGE
        when(fatigueResultService.getResultsForUser(KEYCLOAK_ID))
                .thenReturn(List.of());

        // ACT & ASSERT
        mockMvc.perform(get("/api/fatigue-results/me")
                        .with(jwt().jwt(jwt -> jwt.subject(KEYCLOAK_ID)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldHandleNullFieldsInRequest() throws Exception {
        // ARRANGE
        FatigueResultRequest request = new FatigueResultRequest();
        request.setFatigueScore(null);
        request.setRiskLabel(null);

        FatigueResultResponse response = FatigueResultResponse.builder()
                .id(1L)
                .fatigueScore(null)
                .riskLabel(null)
                .createdAt(Instant.now())
                .build();

        when(fatigueResultService.saveForUser(eq(KEYCLOAK_ID), any(FatigueResultRequest.class)))
                .thenReturn(response);

        // ACT & ASSERT
        mockMvc.perform(post("/api/fatigue-results")
                        .with(jwt().jwt(jwt -> jwt.subject(KEYCLOAK_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void shouldRequireAuthenticationForSave() throws Exception {
        // ARRANGE
        FatigueResultRequest request = new FatigueResultRequest();
        request.setFatigueScore(75);

        // ACT & ASSERT
        // Spring Security may return 403 (Forbidden) instead of 401 (Unauthorized) 
        // when using @WebMvcTest without proper security configuration
        var result = mockMvc.perform(post("/api/fatigue-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
        
        assertThat(result.getResponse().getStatus())
                .isIn(401, 403); // Accept either Unauthorized or Forbidden
    }

    @Test
    void shouldRequireAuthenticationForGet() throws Exception {
        // ACT & ASSERT
        mockMvc.perform(get("/api/fatigue-results/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldHandleComplexRecommendations() throws Exception {
        // ARRANGE
        FatigueResultRequest request = new FatigueResultRequest();
        request.setFatigueScore(75);
        request.setRiskLabel("Élevé");
        request.setRecommendations(Arrays.asList("Repos", "Vacances", "Exercise"));

        FatigueResultResponse response = FatigueResultResponse.builder()
                .id(1L)
                .fatigueScore(75)
                .riskLabel("Élevé")
                .recommendationsJson("[\"Repos\",\"Vacances\",\"Exercise\"]")
                .createdAt(Instant.now())
                .build();

        when(fatigueResultService.saveForUser(eq(KEYCLOAK_ID), any(FatigueResultRequest.class)))
                .thenReturn(response);

        // ACT & ASSERT
        mockMvc.perform(post("/api/fatigue-results")
                        .with(jwt().jwt(jwt -> jwt.subject(KEYCLOAK_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.fatigueScore").value(75));
    }
}

