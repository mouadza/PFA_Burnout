package com.burncare.burncare_app.integration;

import com.burncare.burncare_app.dto.RegisterRequest;
import com.burncare.burncare_app.entities.Profession;
import com.burncare.burncare_app.entities.Role;
import com.burncare.burncare_app.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.ws.rs.core.Response;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // On doit Mocker Keycloak car on n'a pas de serveur Keycloak lancé pour les tests
    @MockBean
    private Keycloak keycloak;

    @Test
    void shouldRegisterUserAndSaveToDatabase() throws Exception {
        // --- 1. MOCK KEYCLOAK (Simulation de la réussite) ---
        // C'est un peu verbeux car l'API Keycloak est chaînée, mais nécessaire
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        RolesResource rolesResource = mock(RolesResource.class);
        org.keycloak.admin.client.resource.RoleResource roleResource = mock(org.keycloak.admin.client.resource.RoleResource.class);
        UserResource userResource = mock(UserResource.class);
        RoleMappingResource roleMappingResource = mock(RoleMappingResource.class);
        RoleScopeResource roleScopeResource = mock(RoleScopeResource.class);

        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        // Simuler une création réussie (201 Created)
        when(usersResource.create(any(UserRepresentation.class)))
                .thenReturn(Response.status(201).location(URI.create("user/new-uuid-123")).build());

        // Simuler la gestion des rôles
        when(realmResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get(anyString())).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(new RoleRepresentation());
        when(usersResource.get(anyString())).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);


        // --- 2. ACTION : Inscription via l'API ---
        RegisterRequest request = new RegisterRequest(
                "Nouveau", "User", "integration@new.com", "password123",
                Profession.MEDECIN, Role.USER, true
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // --- 3. VÉRIFICATION BDD ---
        // On vérifie que le contrôleur a bien appelé le service qui a bien sauvegardé en base
        boolean exists = userRepository.existsByEmail("integration@new.com");
        assertThat(exists).isTrue();
    }
}