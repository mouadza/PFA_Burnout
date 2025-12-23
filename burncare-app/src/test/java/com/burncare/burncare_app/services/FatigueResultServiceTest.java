package com.burncare.burncare_app.services;

import com.burncare.burncare_app.dto.FatigueResultRequest;
import com.burncare.burncare_app.dto.FatigueResultResponse;
import com.burncare.burncare_app.entities.FatigueResult;
import com.burncare.burncare_app.repositories.FatigueResultRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FatigueResultServiceTest {

    @Mock
    private FatigueResultRepository fatigueResultRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private FatigueResultService fatigueResultService;

    private FatigueResultRequest request;
    private FatigueResult savedEntity;
    private String keycloakId;

    @BeforeEach
    void setUp() {
        keycloakId = "test-user-123";
        
        request = new FatigueResultRequest();
        request.setFatigueScore(75);
        request.setRiskLabel("Élevé");
        request.setRiskTitle("Risque Élevé");
        request.setMessage("Faites attention");
        request.setConfidence(0.95);
        request.setRecommendations(Arrays.asList("Repos", "Vacances"));
        request.setRecommendationText("Prenez des vacances");

        savedEntity = new FatigueResult();
        savedEntity.setId(1L);
        savedEntity.setUserId(keycloakId);
        savedEntity.setFatigueScore(75);
        savedEntity.setRiskLabel("Élevé");
        savedEntity.setRiskTitle("Risque Élevé");
        savedEntity.setMessage("Faites attention");
        savedEntity.setConfidence(0.95);
        savedEntity.setRecommendationsJson("[{\"action\":\"Repos\"}]");
        savedEntity.setRecommendationText("Prenez des vacances");
        savedEntity.setCreatedAt(Instant.now());
    }

    @Test
    void shouldSaveFatigueResultForUser() throws JsonProcessingException {
        // ARRANGE
        when(objectMapper.writeValueAsString(request.getRecommendations()))
                .thenReturn("[{\"action\":\"Repos\"}]");
        when(fatigueResultRepository.save(any(FatigueResult.class))).thenReturn(savedEntity);

        // ACT
        FatigueResultResponse response = fatigueResultService.saveForUser(keycloakId, request);

        // ASSERT
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFatigueScore()).isEqualTo(75);
        assertThat(response.getRiskLabel()).isEqualTo("Élevé");
        assertThat(response.getRiskTitle()).isEqualTo("Risque Élevé");
        assertThat(response.getMessage()).isEqualTo("Faites attention");
        assertThat(response.getConfidence()).isEqualTo(0.95);
        assertThat(response.getRecommendationText()).isEqualTo("Prenez des vacances");
        assertThat(response.getCreatedAt()).isNotNull();

        verify(fatigueResultRepository, times(1)).save(any(FatigueResult.class));
        verify(objectMapper, times(1)).writeValueAsString(request.getRecommendations());
    }

    @Test
    void shouldHandleJsonProcessingException() throws JsonProcessingException {
        // ARRANGE
        when(objectMapper.writeValueAsString(request.getRecommendations()))
                .thenThrow(new JsonProcessingException("Error") {});
        
        // Capture the entity that is saved and verify it has "[]" for recommendationsJson
        when(fatigueResultRepository.save(any(FatigueResult.class))).thenAnswer(invocation -> {
            FatigueResult saved = invocation.getArgument(0);
            // Verify that the exception was caught and default value was set
            assertThat(saved.getRecommendationsJson()).isEqualTo("[]");
            // Return the saved entity with the correct value
            saved.setId(1L);
            saved.setCreatedAt(Instant.now());
            return saved;
        });

        // ACT
        FatigueResultResponse response = fatigueResultService.saveForUser(keycloakId, request);

        // ASSERT
        assertThat(response).isNotNull();
        assertThat(response.getRecommendationsJson()).isEqualTo("[]");
        verify(fatigueResultRepository, times(1)).save(any(FatigueResult.class));
    }

    @Test
    void shouldHandleNullRecommendations() throws JsonProcessingException {
        // ARRANGE
        request.setRecommendations(null);
        when(objectMapper.writeValueAsString(null))
                .thenThrow(new JsonProcessingException("Error") {});
        
        // Capture the entity that is saved and verify it has "[]" for recommendationsJson
        when(fatigueResultRepository.save(any(FatigueResult.class))).thenAnswer(invocation -> {
            FatigueResult saved = invocation.getArgument(0);
            // Verify that the exception was caught and default value was set
            assertThat(saved.getRecommendationsJson()).isEqualTo("[]");
            // Return the saved entity with the correct value
            saved.setId(1L);
            saved.setCreatedAt(Instant.now());
            return saved;
        });

        // ACT
        FatigueResultResponse response = fatigueResultService.saveForUser(keycloakId, request);

        // ASSERT
        assertThat(response).isNotNull();
        assertThat(response.getRecommendationsJson()).isEqualTo("[]");
    }

    @Test
    void shouldHandleComplexRecommendationsObject() throws JsonProcessingException {
        // ARRANGE
        Map<String, Object> complexRecommendations = Map.of(
                "actions", Arrays.asList("Repos", "Exercise"),
                "priority", "high"
        );
        request.setRecommendations(complexRecommendations);
        when(objectMapper.writeValueAsString(complexRecommendations))
                .thenReturn("{\"actions\":[\"Repos\",\"Exercise\"],\"priority\":\"high\"}");
        when(fatigueResultRepository.save(any(FatigueResult.class))).thenReturn(savedEntity);

        // ACT
        FatigueResultResponse response = fatigueResultService.saveForUser(keycloakId, request);

        // ASSERT
        assertThat(response).isNotNull();
        verify(objectMapper, times(1)).writeValueAsString(complexRecommendations);
    }

    @Test
    void shouldSetCreatedAtWhenSaving() throws JsonProcessingException {
        // ARRANGE
        when(objectMapper.writeValueAsString(request.getRecommendations()))
                .thenReturn("[]");
        when(fatigueResultRepository.save(any(FatigueResult.class))).thenAnswer(invocation -> {
            FatigueResult result = invocation.getArgument(0);
            assertThat(result.getCreatedAt()).isNotNull();
            return savedEntity;
        });

        // ACT
        fatigueResultService.saveForUser(keycloakId, request);

        // ASSERT
        verify(fatigueResultRepository, times(1)).save(any(FatigueResult.class));
    }

    @Test
    void shouldGetResultsForUser() {
        // ARRANGE
        FatigueResult result1 = new FatigueResult();
        result1.setId(1L);
        result1.setUserId(keycloakId);
        result1.setFatigueScore(75);
        result1.setRiskLabel("Élevé");
        result1.setCreatedAt(Instant.now().minusSeconds(3600));

        FatigueResult result2 = new FatigueResult();
        result2.setId(2L);
        result2.setUserId(keycloakId);
        result2.setFatigueScore(50);
        result2.setRiskLabel("Moyen");
        result2.setCreatedAt(Instant.now());

        List<FatigueResult> results = Arrays.asList(result2, result1); // Plus récent en premier
        when(fatigueResultRepository.findByUserIdOrderByCreatedAtDesc(keycloakId))
                .thenReturn(results);

        // ACT
        List<FatigueResultResponse> responses = fatigueResultService.getResultsForUser(keycloakId);

        // ASSERT
        assertThat(responses)
                .isNotNull()
                .hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo(2L);
        assertThat(responses.get(0).getFatigueScore()).isEqualTo(50);
        assertThat(responses.get(1).getId()).isEqualTo(1L);
        assertThat(responses.get(1).getFatigueScore()).isEqualTo(75);
        
        verify(fatigueResultRepository, times(1))
                .findByUserIdOrderByCreatedAtDesc(keycloakId);
    }

    @Test
    void shouldReturnEmptyListWhenNoResults() {
        // ARRANGE
        when(fatigueResultRepository.findByUserIdOrderByCreatedAtDesc(keycloakId))
                .thenReturn(List.of());

        // ACT
        List<FatigueResultResponse> responses = fatigueResultService.getResultsForUser(keycloakId);

        // ASSERT
        assertThat(responses)
                .isNotNull()
                .isEmpty();
        verify(fatigueResultRepository, times(1))
                .findByUserIdOrderByCreatedAtDesc(keycloakId);
    }

    @Test
    void shouldMapAllFieldsWhenGettingResults() {
        // ARRANGE
        FatigueResult result = new FatigueResult();
        result.setId(10L);
        result.setUserId(keycloakId);
        result.setFatigueScore(85);
        result.setRiskLabel("Très Élevé");
        result.setRiskTitle("Risque Très Élevé");
        result.setMessage("Attention maximale");
        result.setConfidence(0.99);
        result.setRecommendationsJson("[{\"action\":\"Urgent\"}]");
        result.setRecommendationText("Consultez un médecin");
        result.setCreatedAt(Instant.parse("2024-01-15T10:00:00Z"));

        when(fatigueResultRepository.findByUserIdOrderByCreatedAtDesc(keycloakId))
                .thenReturn(List.of(result));

        // ACT
        List<FatigueResultResponse> responses = fatigueResultService.getResultsForUser(keycloakId);

        // ASSERT
        assertThat(responses).hasSize(1);
        FatigueResultResponse response = responses.get(0);
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getFatigueScore()).isEqualTo(85);
        assertThat(response.getRiskLabel()).isEqualTo("Très Élevé");
        assertThat(response.getRiskTitle()).isEqualTo("Risque Très Élevé");
        assertThat(response.getMessage()).isEqualTo("Attention maximale");
        assertThat(response.getConfidence()).isEqualTo(0.99);
        assertThat(response.getRecommendationsJson()).isEqualTo("[{\"action\":\"Urgent\"}]");
        assertThat(response.getRecommendationText()).isEqualTo("Consultez un médecin");
        assertThat(response.getCreatedAt()).isEqualTo(Instant.parse("2024-01-15T10:00:00Z"));
    }

    @Test
    void shouldHandleNullFieldsInRequest() throws JsonProcessingException {
        // ARRANGE
        request.setFatigueScore(null);
        request.setRiskLabel(null);
        request.setRiskTitle(null);
        request.setMessage(null);
        request.setConfidence(null);
        request.setRecommendationText(null);
        
        when(objectMapper.writeValueAsString(request.getRecommendations()))
                .thenReturn("[]");
        when(fatigueResultRepository.save(any(FatigueResult.class))).thenReturn(savedEntity);

        // ACT
        FatigueResultResponse response = fatigueResultService.saveForUser(keycloakId, request);

        // ASSERT
        assertThat(response).isNotNull();
        verify(fatigueResultRepository, times(1)).save(any(FatigueResult.class));
    }
}

