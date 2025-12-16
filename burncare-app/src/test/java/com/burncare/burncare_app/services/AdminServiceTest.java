package com.burncare.burncare_app.services;

import com.burncare.burncare_app.entities.User;
import com.burncare.burncare_app.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Keycloak keycloak;

    @InjectMocks
    private AdminService adminService;

    // Keycloak Mocks
    @Mock private RealmResource realmResource;
    @Mock private UsersResource usersResource;
    @Mock private UserResource userResource;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminService, "realm", "test-realm");
    }

    @Test
    void getAllUsers_ShouldReturnList() {
        // ARRANGE
        when(userRepository.findAll()).thenReturn(Arrays.asList(new User(), new User()));

        // ACT
        List<User> result = adminService.getAllUsers();

        // ASSERT
        assertEquals(2, result.size());
    }

    @Test
    void deleteUser_ShouldRemoveFromKeycloakAndDb() {
        // ARRANGE
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setKeycloakId("uuid-123");
        user.setEmail("test@delete.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Mock Keycloak
        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("uuid-123")).thenReturn(userResource);

        // ACT
        adminService.deleteUser(userId);

        // ASSERT
        verify(userResource).remove(); // Vérifie appel Keycloak
        verify(userRepository).delete(user); // Vérifie appel DB
    }

    @Test
    void deleteUser_ShouldThrowIfNotFound() {
        // ARRANGE
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(RuntimeException.class, () -> adminService.deleteUser(99L));
        // S'assurer qu'on ne touche pas à Keycloak si l'user n'existe pas en local
        verifyNoInteractions(keycloak);
    }
}