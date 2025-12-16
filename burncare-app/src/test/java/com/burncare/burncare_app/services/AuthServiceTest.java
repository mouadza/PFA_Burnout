package com.burncare.burncare_app.services;

import com.burncare.burncare_app.dto.AuthRequest;
import com.burncare.burncare_app.dto.AuthResponse;
import com.burncare.burncare_app.dto.RegisterRequest;
import com.burncare.burncare_app.entities.Profession;
import com.burncare.burncare_app.entities.Role;
import com.burncare.burncare_app.entities.User;
import com.burncare.burncare_app.repositories.UserRepository;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Keycloak keycloak;

    @InjectMocks
    private AuthService authService;

    // Keycloak mocks chain
    @Mock private RealmResource realmResource;
    @Mock private UsersResource usersResource;
    @Mock private UserResource userResource;
    @Mock private RolesResource rolesResource;
    @Mock private RoleMappingResource roleMappingResource;
    @Mock private RoleScopeResource roleScopeResource;
    @Mock private org.keycloak.admin.client.resource.RoleResource roleResource;

    private final String realmName = "test-realm";
    private final String keycloakUrl = "http://localhost:8080";

    @BeforeEach
    void setUp() {
        // Set @Value fields manually since we are in a unit test without Spring context
        ReflectionTestUtils.setField(authService, "realm", realmName);
        ReflectionTestUtils.setField(authService, "keycloakServerUrl", keycloakUrl);
    }

    // --- TESTS REGISTER ---

    @Test
    void register_ShouldThrowConflict_WhenEmailExists() {
        // Arrange
        RegisterRequest req = new RegisterRequest(
                "John", "Doe", "exist@test.com", "pass",
                Profession.MEDECIN, Role.USER, true
        );
        when(userRepository.existsByEmail(req.email())).thenReturn(true);

        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void register_ShouldSucceed_WhenValidRequest() {
        // Arrange
        RegisterRequest req = new RegisterRequest(
                "John", "Doe", "new@test.com", "pass",
                Profession.MEDECIN, Role.USER, true
        );

        when(userRepository.existsByEmail(req.email())).thenReturn(false);

        // Mock Keycloak chain
        when(keycloak.realm(realmName)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        // Mock success response from Keycloak (201 Created)
        Response mockResponse = Response.status(201)
                .location(URI.create("user/uuid-123"))
                .build();
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(mockResponse);

        // Mock Role assignment
        when(realmResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get(anyString())).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(new RoleRepresentation());
        when(usersResource.get(anyString())).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);

        // Act
        AuthResponse response = authService.register(req);

        // Assert
        assertNotNull(response);
        // ✅ Correction : Utilisation de .email() au lieu de .getEmail() pour le record
        assertEquals("new@test.com", response.email());

        // Verify local DB save was called with correct values
        verify(userRepository).save(argThat(user ->
                user.getEmail().equals("new@test.com") &&
                        user.getKeycloakId().equals("uuid-123") &&
                        !user.isEnabled() // Logic dictates false for USER role
        ));
    }

    // --- TESTS LOGIN ---

    @Test
    void login_ShouldThrowBadCredentials_WhenUserNotFoundLocally() {
        // Arrange
        AuthRequest req = new AuthRequest("unknown@test.com", "pass");
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> authService.login(req));
    }

    @Test
    void login_ShouldThrowDisabled_WhenUserNotEnabled() {
        // Arrange
        User disabledUser = new User();
        disabledUser.setEmail("disabled@test.com");
        disabledUser.setEnabled(false);

        AuthRequest req = new AuthRequest("disabled@test.com", "pass");
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(disabledUser));

        // Act & Assert
        assertThrows(DisabledException.class, () -> authService.login(req));
    }

    @Test
    void login_ShouldThrowBadCredentials_WhenKeycloakReturns401() {
        // Arrange
        User activeUser = new User();
        activeUser.setEmail("active@test.com");
        activeUser.setEnabled(true);

        AuthRequest req = new AuthRequest("active@test.com", "wrongpass");
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(activeUser));

        // Mock RestTemplate to throw 401
        try (MockedConstruction<RestTemplate> mockedRestTemplate = Mockito.mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.exchange(
                            anyString(),
                            eq(HttpMethod.POST),
                            any(HttpEntity.class),
                            eq(Map.class)
                    )).thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));
                })) {

            // Act & Assert
            assertThrows(BadCredentialsException.class, () -> authService.login(req));
        }
    }

    @Test
    void login_ShouldSucceed_WhenCredentialsAreValid() {
        // Arrange
        User activeUser = new User();
        activeUser.setFirstName("John");
        activeUser.setLastName("Doe");
        activeUser.setEmail("valid@test.com");
        activeUser.setRole(Role.USER);
        activeUser.setProfession(Profession.MEDECIN);
        activeUser.setEnabled(true);

        AuthRequest req = new AuthRequest("valid@test.com", "validpass");
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(activeUser));

        // Mock RestTemplate to return token
        Map<String, String> tokenBody = new HashMap<>();
        tokenBody.put("access_token", "fake-jwt-token");
        ResponseEntity<Map> responseEntity = ResponseEntity.ok(tokenBody);

        try (MockedConstruction<RestTemplate> mockedRestTemplate = Mockito.mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.exchange(
                            anyString(),
                            eq(HttpMethod.POST),
                            any(HttpEntity.class),
                            eq(Map.class)
                    )).thenReturn(responseEntity);
                })) {

            // Act
            AuthResponse response = authService.login(req);

            // Assert
            assertNotNull(response);
            // ✅ Correction : Utilisation des accesseurs de record (.token(), .email())
            assertEquals("fake-jwt-token", response.token());
            assertEquals("valid@test.com", response.email());
        }
    }
}