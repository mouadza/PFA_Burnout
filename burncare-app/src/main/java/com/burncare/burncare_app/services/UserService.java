package com.burncare.burncare_app.services;

import com.burncare.burncare_app.dto.AuthResponse;
import com.burncare.burncare_app.dto.ChangePasswordRequest;
import com.burncare.burncare_app.dto.UpdateProfileRequest;
import com.burncare.burncare_app.entities.User;
import com.burncare.burncare_app.repositories.UserRepository;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    public UserService(UserRepository userRepository, Keycloak keycloak) {
        this.userRepository = userRepository;
        this.keycloak = keycloak;
    }

    // ✅ Modifier Nom et Prénom (Local + Keycloak)
    public AuthResponse updateProfile(UpdateProfileRequest req) {
        // 1. Récupération utilisateur local
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé : " + req.email()));

        // 2. Mise à jour Keycloak
        if (user.getKeycloakId() != null) {
            try {
                UserResource userResource = keycloak.realm(realm).users().get(user.getKeycloakId());
                UserRepresentation kcUser = userResource.toRepresentation();
                kcUser.setFirstName(req.firstName());
                kcUser.setLastName(req.lastName());
                userResource.update(kcUser);
                System.out.println("✅ Keycloak mis à jour pour : " + req.email());
            } catch (Exception e) {
                System.err.println("⚠️ Erreur Keycloak (Update Profile) : " + e.getMessage());
            }
        }

        // 3. Mise à jour Base de données locale
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        userRepository.save(user);

        // 4. Retourner les nouvelles infos pour mettre à jour l'app mobile
        return new AuthResponse(
                "", // On ne renvoie pas le token ici (inutile pour une update profil)
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getProfession()
        );
    }

    // ✅ Changer le Mot de Passe (Keycloak Uniquement)
    public void changePassword(ChangePasswordRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé : " + req.email()));

        if (user.getKeycloakId() != null) {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(req.newPassword());
            credential.setTemporary(false); // Mot de passe définitif

            keycloak.realm(realm).users().get(user.getKeycloakId()).resetPassword(credential);
            System.out.println("✅ Mot de passe changé dans Keycloak pour : " + req.email());
        } else {
            throw new RuntimeException("Impossible de changer le mot de passe : ID Keycloak manquant.");
        }
    }
}