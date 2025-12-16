package com.burncare.burncare_app.services;

import com.burncare.burncare_app.dto.BurnoutResultRequest;
import com.burncare.burncare_app.dto.BurnoutResultResponse;
import com.burncare.burncare_app.entities.BurnoutResult;
import com.burncare.burncare_app.entities.User;
import com.burncare.burncare_app.repositories.BurnoutResultRepository;
import com.burncare.burncare_app.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BurnoutResultServiceTest {

    @Mock
    private BurnoutResultRepository burnoutResultRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BurnoutResultService burnoutResultService;

    @Test
    void saveForUser_ShouldSaveAndReturnResponse() {
        // ARRANGE
        String keycloakId = "user-123";
        User user = new User();
        user.setKeycloakId(keycloakId);

        BurnoutResultRequest request = new BurnoutResultRequest(
                65, "Élevé", "Risque", "Msg", "Reco", Arrays.asList(1, 2, 3)
        );

        BurnoutResult savedEntity = new BurnoutResult();
        savedEntity.setId(10L);
        savedEntity.setBurnoutScore(65);
        savedEntity.setCreatedAt(Instant.now());
        savedEntity.setRiskLabel("Élevé");
        savedEntity.setRiskTitle("Risque"); // Assurez-vous que ce champ n'est pas null

        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(user));
        when(burnoutResultRepository.save(any(BurnoutResult.class))).thenReturn(savedEntity);

        // ACT
        BurnoutResultResponse response = burnoutResultService.saveForUser(keycloakId, request);

        // ASSERT
        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals(65, response.burnoutScore());
        verify(burnoutResultRepository).save(any(BurnoutResult.class));
    }

    @Test
    void getResultsForUser_ShouldReturnList() {
        // ARRANGE
        String keycloakId = "user-123";
        User user = new User();

        BurnoutResult r1 = new BurnoutResult();
        r1.setId(1L); r1.setBurnoutScore(10); r1.setCreatedAt(Instant.now());

        BurnoutResult r2 = new BurnoutResult();
        r2.setId(2L); r2.setBurnoutScore(20); r2.setCreatedAt(Instant.now());

        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(user));
        when(burnoutResultRepository.findByUserOrderByCreatedAtDesc(user))
                .thenReturn(Arrays.asList(r1, r2));

        // ACT
        List<BurnoutResultResponse> results = burnoutResultService.getResultsForUser(keycloakId);

        // ASSERT
        assertEquals(2, results.size());
        assertEquals(10, results.get(0).burnoutScore());
    }
}