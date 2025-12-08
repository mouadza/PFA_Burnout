package com.burncare.burncare_app.services;

import com.burncare.burncare_app.dto.*;
import com.burncare.burncare_app.entities.*;
import com.burncare.burncare_app.repositories.UserRepository;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final Keycloak keycloak;

    @Value("${keycloak.urls.auth}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    public AuthService(UserRepository userRepository, Keycloak keycloak) {
        this.userRepository = userRepository;
        this.keycloak = keycloak;
    }

    // --- INSCRIPTION (REGISTER) ---
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet email est déjà utilisé.");
        }

        // ✅ SÉCURITÉ MAXIMALE :
        // Tout nouveau compte est DÉSACTIVÉ par défaut (false).
        // Même si l'admin coche "Activé" lors de la création, on l'ignore ici pour forcer la validation ultérieure.
        // L'admin devra aller dans "Détails" -> "Approuver" pour l'activer.
        boolean isEnabled = false;

        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setFirstName(req.firstName());
        kcUser.setLastName(req.lastName());
        kcUser.setEmail(req.email());
        kcUser.setUsername(req.email());
        kcUser.setEnabled(isEnabled); // Toujours false à la création
        kcUser.setEmailVerified(true);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(req.password());
        credential.setTemporary(false);
        kcUser.setCredentials(List.of(credential));

        UsersResource usersResource = keycloak.realm(realm).users();

        Response response;
        try {
            response = usersResource.create(kcUser);
        } catch (Exception e) {
            throw new RuntimeException("Erreur connexion Keycloak: " + e.getMessage());
        }

        if (response.getStatus() != 201) {
            String errorBody = response.readEntity(String.class);
            throw new RuntimeException("Erreur Keycloak: " + errorBody);
        }

        String userId = CreatedResponseUtil.getCreatedId(response);

        // Ajout des rôles Keycloak
        try {
            if (req.profession() != null) {
                String professionRole = req.profession().name();
                try {
                    RoleRepresentation roleRep = keycloak.realm(realm).roles().get(professionRole).toRepresentation();
                    usersResource.get(userId).roles().realmLevel().add(Collections.singletonList(roleRep));
                } catch (Exception ex) {
                    System.err.println("⚠️ Rôle Keycloak introuvable pour profession: " + professionRole);
                }
            }

            if (req.role() == Role.ADMIN) {
                try {
                    RoleRepresentation adminRoleRep = keycloak.realm(realm).roles().get("ADMIN").toRepresentation();
                    usersResource.get(userId).roles().realmLevel().add(Collections.singletonList(adminRoleRep));
                } catch (Exception ex) {
                    System.err.println("⚠️ Rôle ADMIN introuvable dans Keycloak");
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur assignation rôles Keycloak : " + e.getMessage());
        }

        User user = new User();
        user.setKeycloakId(userId);
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        user.setEmail(req.email());
        user.setProfession(req.profession());

        user.setRole(req.role() != null ? req.role() : Role.USER);

        user.setEnabled(isEnabled);

        userRepository.save(user);

        return new AuthResponse(
                "",
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getProfession()
        );
    }

    // --- CONNEXION (LOGIN) ---
    public AuthResponse login(AuthRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new BadCredentialsException("Email ou mot de passe incorrect"));

        if (!user.isEnabled()) {
            // Message explicite pour l'utilisateur bloqué
            throw new DisabledException("Votre compte est créé mais doit être activé par un administrateur.");
        }

        String tokenEndpoint = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", "springboot-app");
        map.add("username", req.email());
        map.add("password", req.password());
        map.add("grant_type", "password");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

        String accessToken = "";
        try {
            ResponseEntity<Map> response = restTemplate.exchange(tokenEndpoint, HttpMethod.POST, entity, Map.class);
            accessToken = (String) response.getBody().get("access_token");
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new BadCredentialsException("Email ou mot de passe incorrect");
            }
            throw new RuntimeException("Erreur Keycloak: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Erreur interne: " + e.getMessage());
        }

        return new AuthResponse(
                accessToken,
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getProfession()
        );
    }
}