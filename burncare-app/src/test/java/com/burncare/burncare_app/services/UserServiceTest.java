package com.burncare.burncare_app.services;

import com.burncare.burncare_app.dto.ChangePasswordRequest;
import com.burncare.burncare_app.dto.UpdateProfileRequest;
import com.burncare.burncare_app.dto.UserDTO;
import com.burncare.burncare_app.entities.Profession;
import com.burncare.burncare_app.entities.Role;
import com.burncare.burncare_app.entities.User;
import com.burncare.burncare_app.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Keycloak keycloak;

    @InjectMocks
    private UserService userService;

    // Mock Keycloak Chaining
    @Mock private RealmResource realmResource;
    @Mock private UsersResource usersResource;
    @Mock private UserResource userResource;

    @BeforeEach
    void setUp() {
        // Injection de la valeur @Value("${keycloak.realm}")
        ReflectionTestUtils.setField(userService, "realm", "test-realm");
    }

    @Test
    void updateProfile_ShouldUpdateLocalAndKeycloak() {
        // ARRANGE
        UpdateProfileRequest req = new UpdateProfileRequest("jean@mail.com", "Jean", "NewName");
        User user = new User();
        user.setEmail("jean@mail.com");
        user.setKeycloakId("uuid-123");
        user.setRole(Role.USER);
        user.setProfession(Profession.MEDECIN);

        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));

        // Mock Keycloak calls
        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("uuid-123")).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(new UserRepresentation());

        // ACT
        userService.updateProfile(req);

        // ASSERT
        assertEquals("NewName", user.getLastName()); // Vérifie l'objet local modifié
        verify(userRepository).save(user); // Vérifie la sauvegarde locale
        verify(userResource).update(any(UserRepresentation.class)); // Vérifie Keycloak
    }

    @Test
    void changePassword_ShouldCallKeycloakReset() {
        // ARRANGE
        ChangePasswordRequest req = new ChangePasswordRequest("jean@mail.com", "newpass");
        User user = new User();
        user.setKeycloakId("uuid-123");

        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));

        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("uuid-123")).thenReturn(userResource);

        // ACT
        userService.changePassword(req);

        // ASSERT
        verify(userResource).resetPassword(any(CredentialRepresentation.class));
    }

    @Test
    void updateUser_ShouldUpdateAdminFields() {
        // ARRANGE
        Long userId = 1L;
        UserDTO dto = new UserDTO("Jean", "Dupont", "jean@mail.com", "ADMIN", "ADMIN", true);
        User user = new User();
        user.setId(userId);
        user.setKeycloakId("uuid-123");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Mock Keycloak calls for admin update
        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("uuid-123")).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(new UserRepresentation());

        // ACT
        User result = userService.updateUser(userId, dto);

        // ASSERT
        assertEquals(Role.ADMIN, result.getRole());
        assertTrue(result.isEnabled());
        verify(userRepository).save(user);
    }
}