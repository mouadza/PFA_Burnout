package com.burncare.burncare_app.services;// import important
import com.burncare.burncare_app.dto.BurnoutResultRequest;
import com.burncare.burncare_app.dto.BurnoutResultResponse;
import com.burncare.burncare_app.entities.BurnoutResult;
import com.burncare.burncare_app.entities.User;
import com.burncare.burncare_app.repositories.BurnoutResultRepository;
import com.burncare.burncare_app.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BurnoutResultService {

    private final BurnoutResultRepository burnoutResultRepository;
    private final UserRepository userRepository;

    // ❌ NE PLUS LE METTRE EN final, NI DANS LE CONSTRUCTEUR
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public BurnoutResultResponse saveForUser(String keycloakId, BurnoutResultRequest req) {
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found for keycloakId " + keycloakId));

        BurnoutResult entity = new BurnoutResult();
        entity.setUser(user);
        entity.setBurnoutScore(req.burnoutScore());
        entity.setRiskLabel(req.riskLabel());
        entity.setRiskTitle(req.riskTitle());
        entity.setMessage(req.message());
        entity.setRecommendation(req.recommendation());

        try {
            // ✅ sérialisation des réponses en JSON
            entity.setAnswersJson(objectMapper.writeValueAsString(req.answers()));
        } catch (Exception e) {
            throw new RuntimeException("Error serializing answers", e);
        }

        entity.setCreatedAt(Instant.now());

        BurnoutResult saved = burnoutResultRepository.save(entity);

        return new BurnoutResultResponse(
                saved.getId(),
                saved.getBurnoutScore(),
                saved.getRiskLabel(),
                saved.getRiskTitle(),
                saved.getCreatedAt().toString()
        );
    }

    public List<BurnoutResultResponse> getResultsForUser(String keycloakId) {
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found for keycloakId " + keycloakId));

        List<BurnoutResult> results = burnoutResultRepository.findByUserOrderByCreatedAtDesc(user);

        return results.stream()
                .map(r -> new BurnoutResultResponse(
                        r.getId(),
                        r.getBurnoutScore(),
                        r.getRiskLabel(),
                        r.getRiskTitle(),
                        r.getCreatedAt().toString()
                ))
                .toList();
    }
}
