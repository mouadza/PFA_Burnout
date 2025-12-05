package com.burncare.burncare_app.services;

import com.burncare.burncare_app.dto.*;
import com.burncare.burncare_app.entities.*;
import com.burncare.burncare_app.repositories.UserRepository;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

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

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new RuntimeException("Email existe déjà");
        }

        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setFirstName(req.firstName());
        kcUser.setLastName(req.lastName());
        kcUser.setEmail(req.email());
        kcUser.setUsername(req.email());
        kcUser.setEnabled(true);
        kcUser.setEmailVerified(true);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(req.password());
        credential.setTemporary(false);
        kcUser.setCredentials(List.of(credential));

        UsersResource usersResource = keycloak.realm(realm).users();
        Response response = usersResource.create(kcUser);

        if (response.getStatus() != 201) {
            String errorBody = response.readEntity(String.class);
            System.err.println("❌ KEYCLOAK REGISTER ERROR: " + errorBody);
            throw new RuntimeException("Keycloak Refus: " + errorBody);
        }

        String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

        try {
            String roleName = req.profession().name();
            RoleRepresentation roleRep = keycloak.realm(realm).roles().get(roleName).toRepresentation();
            usersResource.get(userId).roles().realmLevel().add(Collections.singletonList(roleRep));
        } catch (Exception e) {
            System.err.println("⚠️ ERREUR RÔLE : " + e.getMessage());
        }

        User user = new User();
        user.setKeycloakId(userId);
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        user.setEmail(req.email());
        user.setProfession(req.profession());
        user.setRole(Role.USER);

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

    public AuthResponse login(AuthRequest req) {
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
            throw new RuntimeException("Erreur Keycloak: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Erreur interne: " + e.getMessage());
        }

        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable en local"));

        return new AuthResponse(
                accessToken,
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getProfession()
        );
    }

    // ✅ MISE À JOUR DU PROFIL (Keycloak + DB)
    public AuthResponse updateProfile(UpdateProfileRequest req) {
        // 1. Récupérer l'utilisateur local
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // 2. Mettre à jour Keycloak
        if (user.getKeycloakId() != null) {
            UserResource userResource = keycloak.realm(realm).users().get(user.getKeycloakId());
            UserRepresentation kcUser = userResource.toRepresentation();
            kcUser.setFirstName(req.firstName());
            kcUser.setLastName(req.lastName());
            userResource.update(kcUser);
        }

        // 3. Mettre à jour la base locale
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        userRepository.save(user);

        // 4. Retourner les nouvelles infos (sans changer le token pour l'instant)
        return new AuthResponse(
                "", // On ne renvoie pas de nouveau token ici pour simplifier
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getProfession()
        );
    }

    // ✅ CHANGEMENT DE MOT DE PASSE (Keycloak Uniquement)
    public void changePassword(ChangePasswordRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (user.getKeycloakId() != null) {
            CredentialRepresentation cred = new CredentialRepresentation();
            cred.setType(CredentialRepresentation.PASSWORD);
            cred.setValue(req.newPassword());
            cred.setTemporary(false);

            keycloak.realm(realm).users().get(user.getKeycloakId()).resetPassword(cred);
            System.out.println("✅ Mot de passe mis à jour dans Keycloak pour " + req.email());
        }
    }
}