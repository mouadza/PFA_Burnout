package com.burncare.burncare_app.integration;

import com.burncare.burncare_app.dto.UpdateProfileRequest;
import com.burncare.burncare_app.entities.Profession;
import com.burncare.burncare_app.entities.Role;
import com.burncare.burncare_app.entities.User;
import com.burncare.burncare_app.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class UserProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private Keycloak keycloak;

    @Test
    void shouldUpdateUserProfileInDatabase() throws Exception {
        // --- 1. PRÉPARATION (ARRANGE) ---
        // Création de l'utilisateur initial en BDD
        String userEmail = "profile-test@burncare.com";
        String keycloakId = "uuid-profile-test";

        User user = new User();
        user.setEmail(userEmail);
        user.setFirstName("AncienPrenom");
        user.setLastName("AncienNom");
        user.setRole(Role.USER);
        user.setProfession(Profession.INFIRMIER);
        user.setKeycloakId(keycloakId);
        user.setEnabled(true);
        userRepository.save(user);

        // Mock Keycloak (obligatoire car le service appelle Keycloak)
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        UserResource userResource = mock(UserResource.class);

        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(keycloakId)).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(new UserRepresentation());

        // --- 2. ACTION (ACT) ---
        // Requête de mise à jour
        UpdateProfileRequest request = new UpdateProfileRequest(
                userEmail,
                "NouveauPrenom",
                "NouveauNom"
        );

        mockMvc.perform(put("/api/user/profile")
                        // Simulation du token JWT (Rôle USER, ID Keycloak correct)
                        // ✅ CORRECTION : Utilisation de .jwt(...) pour définir le subject car .subject() n'existe pas directement
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(builder -> builder.subject(keycloakId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))

                // --- 3. VÉRIFICATION RÉPONSE (ASSERT HTTP) ---
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("NouveauPrenom"))
                .andExpect(jsonPath("$.lastName").value("NouveauNom"));

        // --- 4. VÉRIFICATION BDD (ASSERT DATA) ---
        // On récupère l'utilisateur en base pour être sûr qu'il a changé
        User updatedUser = userRepository.findByEmail(userEmail).orElseThrow();

        assertThat(updatedUser.getFirstName()).isEqualTo("NouveauPrenom");
        assertThat(updatedUser.getLastName()).isEqualTo("NouveauNom");
    }
}