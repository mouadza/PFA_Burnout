package com.burncare.burncare_app.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

class FatigueResultRequestTest {

    @Test
    void shouldCreateFatigueResultRequestWithAllArgsConstructor() {
        // ARRANGE
        Integer fatigueScore = 75;
        String riskLabel = "Élevé";
        String riskTitle = "Risque Élevé";
        String message = "Faites attention";
        Double confidence = 0.95;
        List<String> recommendations = Arrays.asList("Repos", "Vacances");
        String recommendationText = "Prenez des vacances";

        // ACT
        FatigueResultRequest request = new FatigueResultRequest(
                fatigueScore, riskLabel, riskTitle, message, confidence,
                recommendations, recommendationText
        );

        // ASSERT
        assertThat(request.getFatigueScore()).isEqualTo(fatigueScore);
        assertThat(request.getRiskLabel()).isEqualTo(riskLabel);
        assertThat(request.getRiskTitle()).isEqualTo(riskTitle);
        assertThat(request.getMessage()).isEqualTo(message);
        assertThat(request.getConfidence()).isEqualTo(confidence);
        assertThat(request.getRecommendations()).isEqualTo(recommendations);
        assertThat(request.getRecommendationText()).isEqualTo(recommendationText);
    }

    @Test
    void shouldCreateFatigueResultRequestWithNoArgsConstructor() {
        // ARRANGE & ACT
        FatigueResultRequest request = new FatigueResultRequest();

        // ASSERT
        assertThat(request.getFatigueScore()).isNull();
        assertThat(request.getRiskLabel()).isNull();
        assertThat(request.getRiskTitle()).isNull();
        assertThat(request.getMessage()).isNull();
        assertThat(request.getConfidence()).isNull();
        assertThat(request.getRecommendations()).isNull();
        assertThat(request.getRecommendationText()).isNull();
    }

    @Test
    void shouldSetAndGetAllFields() {
        // ARRANGE
        FatigueResultRequest request = new FatigueResultRequest();
        Integer fatigueScore = 50;
        String riskLabel = "Moyen";
        String riskTitle = "Risque Moyen";
        String message = "Soyez prudent";
        Double confidence = 0.80;
        Map<String, Object> recommendations = Map.of("action", "Repos");
        String recommendationText = "Reposez-vous";

        // ACT
        request.setFatigueScore(fatigueScore);
        request.setRiskLabel(riskLabel);
        request.setRiskTitle(riskTitle);
        request.setMessage(message);
        request.setConfidence(confidence);
        request.setRecommendations(recommendations);
        request.setRecommendationText(recommendationText);

        // ASSERT
        assertThat(request.getFatigueScore()).isEqualTo(fatigueScore);
        assertThat(request.getRiskLabel()).isEqualTo(riskLabel);
        assertThat(request.getRiskTitle()).isEqualTo(riskTitle);
        assertThat(request.getMessage()).isEqualTo(message);
        assertThat(request.getConfidence()).isEqualTo(confidence);
        assertThat(request.getRecommendations()).isEqualTo(recommendations);
        assertThat(request.getRecommendationText()).isEqualTo(recommendationText);
    }

    @Test
    void shouldHandleNullValues() {
        // ARRANGE
        FatigueResultRequest request = new FatigueResultRequest();

        // ACT
        request.setFatigueScore(null);
        request.setRiskLabel(null);
        request.setRiskTitle(null);
        request.setMessage(null);
        request.setConfidence(null);
        request.setRecommendations(null);
        request.setRecommendationText(null);

        // ASSERT
        assertThat(request.getFatigueScore()).isNull();
        assertThat(request.getRiskLabel()).isNull();
        assertThat(request.getRiskTitle()).isNull();
        assertThat(request.getMessage()).isNull();
        assertThat(request.getConfidence()).isNull();
        assertThat(request.getRecommendations()).isNull();
        assertThat(request.getRecommendationText()).isNull();
    }

    @Test
    void shouldHandleEdgeCaseValues() {
        // ARRANGE
        FatigueResultRequest request = new FatigueResultRequest();
        
        // ACT
        request.setFatigueScore(0);
        request.setFatigueScore(100);
        request.setConfidence(0.0);
        request.setConfidence(1.0);

        // ASSERT
        assertThat(request.getFatigueScore()).isEqualTo(100);
        assertThat(request.getConfidence()).isEqualTo(1.0);
    }

    @Test
    void shouldHandleComplexRecommendationsObject() {
        // ARRANGE
        FatigueResultRequest request = new FatigueResultRequest();
        List<Map<String, Object>> complexRecommendations = Arrays.asList(
                Map.of("type", "rest", "duration", 7),
                Map.of("type", "exercise", "intensity", "light")
        );

        // ACT
        request.setRecommendations(complexRecommendations);

        // ASSERT
        assertThat(request.getRecommendations()).isEqualTo(complexRecommendations);
        assertThat(request.getRecommendations()).isInstanceOf(List.class);
    }
}

