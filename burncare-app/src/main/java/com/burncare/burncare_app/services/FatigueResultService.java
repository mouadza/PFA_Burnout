package com.burncare.burncare_app.services;

import com.burncare.burncare_app.dto.FatigueResultRequest;
import com.burncare.burncare_app.dto.FatigueResultResponse;
import com.burncare.burncare_app.entities.FatigueResult;
import com.burncare.burncare_app.repositories.FatigueResultRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FatigueResultService {

    private final FatigueResultRepository fatigueResultRepository;
    private final ObjectMapper objectMapper;

    public FatigueResultResponse saveForUser(String keycloakId, FatigueResultRequest request) {

        FatigueResult r = new FatigueResult();
        r.setUserId(keycloakId);

        r.setFatigueScore(request.getFatigueScore());
        r.setRiskLabel(request.getRiskLabel());
        r.setRiskTitle(request.getRiskTitle());
        r.setMessage(request.getMessage());
        r.setConfidence(request.getConfidence());
        r.setRecommendationText(request.getRecommendationText());

        r.setCreatedAt(Instant.now());

        try {
            r.setRecommendationsJson(objectMapper.writeValueAsString(request.getRecommendations()));
        } catch (Exception e) {
            r.setRecommendationsJson("[]");
        }

        FatigueResult saved = fatigueResultRepository.save(r);

        return FatigueResultResponse.builder()
                .id(saved.getId())
                .fatigueScore(saved.getFatigueScore())
                .riskLabel(saved.getRiskLabel())
                .riskTitle(saved.getRiskTitle())
                .message(saved.getMessage())
                .confidence(saved.getConfidence())
                .recommendationsJson(saved.getRecommendationsJson())
                .recommendationText(saved.getRecommendationText())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public List<FatigueResultResponse> getResultsForUser(String keycloakId) {
        return fatigueResultRepository.findByUserIdOrderByCreatedAtDesc(keycloakId)
                .stream()
                .map(r -> FatigueResultResponse.builder()
                        .id(r.getId())
                        .fatigueScore(r.getFatigueScore())
                        .riskLabel(r.getRiskLabel())
                        .riskTitle(r.getRiskTitle())
                        .message(r.getMessage())
                        .confidence(r.getConfidence())
                        .recommendationsJson(r.getRecommendationsJson())
                        .recommendationText(r.getRecommendationText())
                        .createdAt(r.getCreatedAt())
                        .build()
                )
                .toList();
    }
}
