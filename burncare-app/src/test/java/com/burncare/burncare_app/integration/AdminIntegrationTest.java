package com.burncare.burncare_app.integration;

import com.burncare.burncare_app.entities.Profession;
import com.burncare.burncare_app.entities.Role;
import com.burncare.burncare_app.entities.User;
import com.burncare.burncare_app.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private Keycloak keycloak;

    @Test
    void adminShouldSeeAllUsers() throws Exception {
        // ARRANGE : Créer 2 users en BDD
        userRepository.save(createUser("u1@test.com", "User1"));
        userRepository.save(createUser("u2@test.com", "User2"));

        // ACT : Appel API avec Rôle ADMIN
        mockMvc.perform(get("/api/admin/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON))
                // ASSERT
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)); // Doit trouver les 2 users
    }

    @Test
    void simpleUserShouldNotSeeAllUsers() throws Exception {
        // ACT : Appel API avec Rôle USER
        mockMvc.perform(get("/api/admin/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON))
                // ASSERT : Sécurité
                .andExpect(status().isForbidden()); // 403
    }

    @Test
    void adminShouldDeleteUser() throws Exception {
        // ARRANGE
        User userToDelete = userRepository.save(createUser("delete@test.com", "To Delete"));

        // Mock Keycloak delete (pour que ça ne plante pas)
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        UserResource userResource = mock(UserResource.class);

        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(anyString())).thenReturn(userResource);

        // ACT
        mockMvc.perform(delete("/api/admin/users/" + userToDelete.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        // ASSERT
        assertThat(userRepository.existsByEmail("delete@test.com")).isFalse();
    }

    // Helper pour créer rapidement un user
    private User createUser(String email, String name) {
        User u = new User();
        u.setEmail(email);
        u.setFirstName(name);
        u.setLastName("Test");
        u.setRole(Role.USER);
        u.setProfession(Profession.INFIRMIER);
        u.setKeycloakId("uuid-" + email);
        u.setEnabled(true);
        return u;
    }
}