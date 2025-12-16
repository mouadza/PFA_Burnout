package com.burncare.burncare_app.integration;

import com.burncare.burncare_app.dto.BurnoutResultRequest;
import com.burncare.burncare_app.entities.Profession;
import com.burncare.burncare_app.entities.Role;
import com.burncare.burncare_app.entities.User;
import com.burncare.burncare_app.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest // 1. Charge tout le contexte Spring (comme l'application réelle)
@AutoConfigureMockMvc // 2. Configure MockMvc pour appeler les API
@Transactional // 3. Annule les modifications en BDD à la fin de chaque test (nettoyage auto)
@ActiveProfiles("test") // Optionnel: si vous avez un application-test.properties
class BurnoutFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository; // On a besoin du repo pour préparer la BDD

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSaveAndRetrieveBurnoutResult() throws Exception {
        // --- ETAPE 1 : PRÉPARATION (ARRANGE) ---
        // Il faut qu'un utilisateur existe en base pour lier le résultat
        String keycloakId = "integration-user-uuid";
        User user = new User();
        user.setEmail("integration@test.com");
        user.setFirstName("Test");
        user.setLastName("Integration");
        user.setRole(Role.USER);
        user.setProfession(Profession.INFIRMIER);
        user.setKeycloakId(keycloakId);
        user.setEnabled(true);
        userRepository.save(user);

        // --- ETAPE 2 : ACTION - SAUVEGARDE (POST) ---
        BurnoutResultRequest request = new BurnoutResultRequest(
                75,
                "Élevé",
                "Risque Élevé",
                "Faites attention",
                "Prenez des vacances",
                Arrays.asList(4, 4, 3, 5)
        );

        // On appelle l'API POST /api/burnout-results
        mockMvc.perform(post("/api/burnout-results")
                        .with(jwt().jwt(builder -> builder.subject(keycloakId))) // On simule le token JWT de cet utilisateur
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.burnoutScore").value(75));

        // --- ETAPE 3 : VÉRIFICATION - RECUPERATION (GET) ---
        // On appelle l'API GET /api/burnout-results/me pour voir si on retrouve le résultat
        mockMvc.perform(get("/api/burnout-results/me")
                        .with(jwt().jwt(builder -> builder.subject(keycloakId)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)) // Il doit y avoir 1 résultat
                .andExpect(jsonPath("$[0].riskLabel").value("Élevé"))
                .andExpect(jsonPath("$[0].burnoutScore").value(75));
    }
}