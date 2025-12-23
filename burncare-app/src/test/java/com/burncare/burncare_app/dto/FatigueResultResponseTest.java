package com.burncare.burncare_app.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

class FatigueResultResponseTest {

    @Test
    void shouldCreateFatigueResultResponseWithBuilder() {
        // ARRANGE
        Long id = 1L;
        Integer fatigueScore = 75;
        String riskLabel = "Élevé";
        String riskTitle = "Risque Élevé";
        String message = "Faites attention";
        Double confidence = 0.95;
        String recommendationsJson = "[{\"action\":\"Repos\"}]";
        String recommendationText = "Prenez des vacances";
        Instant createdAt = Instant.now();

        // ACT
        FatigueResultResponse response = FatigueResultResponse.builder()
                .id(id)
                .fatigueScore(fatigueScore)
                .riskLabel(riskLabel)
                .riskTitle(riskTitle)
                .message(message)
                .confidence(confidence)
                .recommendationsJson(recommendationsJson)
                .recommendationText(recommendationText)
                .createdAt(createdAt)
                .build();

        // ASSERT
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getFatigueScore()).isEqualTo(fatigueScore);
        assertThat(response.getRiskLabel()).isEqualTo(riskLabel);
        assertThat(response.getRiskTitle()).isEqualTo(riskTitle);
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getConfidence()).isEqualTo(confidence);
        assertThat(response.getRecommendationsJson()).isEqualTo(recommendationsJson);
        assertThat(response.getRecommendationText()).isEqualTo(recommendationText);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void shouldCreateFatigueResultResponseWithAllArgsConstructor() {
        // ARRANGE
        Long id = 2L;
        Integer fatigueScore = 50;
        String riskLabel = "Moyen";
        String riskTitle = "Risque Moyen";
        String message = "Soyez prudent";
        Double confidence = 0.80;
        String recommendationsJson = "[]";
        String recommendationText = "Reposez-vous";
        Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");

        // ACT
        FatigueResultResponse response = new FatigueResultResponse(
                id, fatigueScore, riskLabel, riskTitle, message, confidence,
                recommendationsJson, recommendationText, createdAt
        );

        // ASSERT
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getFatigueScore()).isEqualTo(fatigueScore);
        assertThat(response.getRiskLabel()).isEqualTo(riskLabel);
        assertThat(response.getRiskTitle()).isEqualTo(riskTitle);
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getConfidence()).isEqualTo(confidence);
        assertThat(response.getRecommendationsJson()).isEqualTo(recommendationsJson);
        assertThat(response.getRecommendationText()).isEqualTo(recommendationText);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void shouldCreateFatigueResultResponseWithNoArgsConstructor() {
        // ARRANGE & ACT
        FatigueResultResponse response = new FatigueResultResponse();

        // ASSERT
        assertThat(response.getId()).isNull();
        assertThat(response.getFatigueScore()).isNull();
        assertThat(response.getRiskLabel()).isNull();
        assertThat(response.getRiskTitle()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getConfidence()).isNull();
        assertThat(response.getRecommendationsJson()).isNull();
        assertThat(response.getRecommendationText()).isNull();
        assertThat(response.getCreatedAt()).isNull();
    }

    @Test
    void shouldSetAndGetAllFields() {
        // ARRANGE
        FatigueResultResponse response = new FatigueResultResponse();
        Long id = 3L;
        Integer fatigueScore = 25;
        String riskLabel = "Faible";
        String riskTitle = "Risque Faible";
        String message = "Tout va bien";
        Double confidence = 0.60;
        String recommendationsJson = "{\"recommendations\":[]}";
        String recommendationText = "Continuez ainsi";
        Instant createdAt = Instant.now();

        // ACT
        response.setId(id);
        response.setFatigueScore(fatigueScore);
        response.setRiskLabel(riskLabel);
        response.setRiskTitle(riskTitle);
        response.setMessage(message);
        response.setConfidence(confidence);
        response.setRecommendationsJson(recommendationsJson);
        response.setRecommendationText(recommendationText);
        response.setCreatedAt(createdAt);

        // ASSERT
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getFatigueScore()).isEqualTo(fatigueScore);
        assertThat(response.getRiskLabel()).isEqualTo(riskLabel);
        assertThat(response.getRiskTitle()).isEqualTo(riskTitle);
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getConfidence()).isEqualTo(confidence);
        assertThat(response.getRecommendationsJson()).isEqualTo(recommendationsJson);
        assertThat(response.getRecommendationText()).isEqualTo(recommendationText);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void shouldHandleNullValues() {
        // ARRANGE
        FatigueResultResponse response = FatigueResultResponse.builder()
                .id(null)
                .fatigueScore(null)
                .riskLabel(null)
                .riskTitle(null)
                .message(null)
                .confidence(null)
                .recommendationsJson(null)
                .recommendationText(null)
                .createdAt(null)
                .build();

        // ASSERT
        assertThat(response.getId()).isNull();
        assertThat(response.getFatigueScore()).isNull();
        assertThat(response.getRiskLabel()).isNull();
        assertThat(response.getRiskTitle()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getConfidence()).isNull();
        assertThat(response.getRecommendationsJson()).isNull();
        assertThat(response.getRecommendationText()).isNull();
        assertThat(response.getCreatedAt()).isNull();
    }

    @Test
    void shouldHaveSameFieldValues() {
        // ARRANGE - Since FatigueResultResponse doesn't have @EqualsAndHashCode,
        // we test that objects with the same field values can be created
        Instant now = Instant.now();
        FatigueResultResponse response1 = FatigueResultResponse.builder()
                .id(1L)
                .fatigueScore(75)
                .riskLabel("Élevé")
                .riskTitle("Risque Élevé")
                .message("Test")
                .confidence(0.95)
                .recommendationsJson("[]")
                .recommendationText("Test")
                .createdAt(now)
                .build();

        FatigueResultResponse response2 = FatigueResultResponse.builder()
                .id(1L)
                .fatigueScore(75)
                .riskLabel("Élevé")
                .riskTitle("Risque Élevé")
                .message("Test")
                .confidence(0.95)
                .recommendationsJson("[]")
                .recommendationText("Test")
                .createdAt(now)
                .build();

        FatigueResultResponse response3 = FatigueResultResponse.builder()
                .id(2L)
                .fatigueScore(75)
                .riskLabel("Élevé")
                .riskTitle("Risque Élevé")
                .message("Test")
                .confidence(0.95)
                .recommendationsJson("[]")
                .recommendationText("Test")
                .createdAt(now)
                .build();

        // ASSERT - Compare field values since equals/hashCode are not implemented
        assertThat(response1.getId()).isEqualTo(response2.getId());
        assertThat(response1.getFatigueScore()).isEqualTo(response2.getFatigueScore());
        assertThat(response1.getRiskLabel()).isEqualTo(response2.getRiskLabel());
        assertThat(response1.getRiskTitle()).isEqualTo(response2.getRiskTitle());
        assertThat(response1.getMessage()).isEqualTo(response2.getMessage());
        assertThat(response1.getConfidence()).isEqualTo(response2.getConfidence());
        assertThat(response1.getRecommendationsJson()).isEqualTo(response2.getRecommendationsJson());
        assertThat(response1.getRecommendationText()).isEqualTo(response2.getRecommendationText());
        assertThat(response1.getCreatedAt()).isEqualTo(response2.getCreatedAt());
        
        // Verify different IDs result in different objects
        assertThat(response1.getId()).isNotEqualTo(response3.getId());
    }

    @Test
    void shouldHandleToString() {
        // ARRANGE
        FatigueResultResponse response = FatigueResultResponse.builder()
                .id(1L)
                .fatigueScore(75)
                .build();

        // ACT
        String toString = response.toString();

        // ASSERT
        assertThat(toString)
                .isNotNull()
                .contains("FatigueResultResponse");
    }
}

