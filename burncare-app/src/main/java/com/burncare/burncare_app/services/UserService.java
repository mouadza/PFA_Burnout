package com.burncare.burncare_app.services;

import com.burncare.burncare_app.dto.AuthResponse;
import com.burncare.burncare_app.dto.ChangePasswordRequest;
import com.burncare.burncare_app.dto.UpdateProfileRequest;
import com.burncare.burncare_app.dto.UserDTO;
import com.burncare.burncare_app.entities.Profession;
import com.burncare.burncare_app.entities.Role;
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

    // ==========================================
    // 1. GESTION PROFIL (Utilisateur lui-même)
    // ==========================================
    public AuthResponse updateProfile(UpdateProfileRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé : " + req.email()));

        if (user.getKeycloakId() != null) {
            try {
                UserResource userResource = keycloak.realm(realm).users().get(user.getKeycloakId());
                UserRepresentation kcUser = userResource.toRepresentation();
                kcUser.setFirstName(req.firstName());
                kcUser.setLastName(req.lastName());
                userResource.update(kcUser);
            } catch (Exception e) {
                System.err.println("⚠️ Erreur Keycloak : " + e.getMessage());
            }
        }

        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
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

    public void changePassword(ChangePasswordRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé : " + req.email()));

        if (user.getKeycloakId() != null) {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(req.newPassword());
            credential.setTemporary(false);
            keycloak.realm(realm).users().get(user.getKeycloakId()).resetPassword(credential);
        }
    }

    // ==========================================
    // 2. ADMINISTRATION (Correction du Crash)
    // ==========================================
    public User updateUser(Long id, UserDTO userDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID : " + id));

        // ✅ CORRECTION : Vérifier si les valeurs sont NULL avant de les appliquer
        // Cela permet les mises à jour partielles (ex: juste activer le compte sans renvoyer le rôle)

        if (userDTO.getFirstName() != null) user.setFirstName(userDTO.getFirstName());
        if (userDTO.getLastName() != null) user.setLastName(userDTO.getLastName());
        if (userDTO.getEmail() != null) user.setEmail(userDTO.getEmail());

        // Protection contre le NullPointerException sur valueOf()
        if (userDTO.getRole() != null) {
            user.setRole(Role.valueOf(userDTO.getRole()));
        }
        if (userDTO.getProfession() != null) {
            user.setProfession(Profession.valueOf(userDTO.getProfession()));
        }

        // Pour enabled, comme c'est un boolean primitif, attention :
        // Si le JSON ne contient pas "enabled", il sera false par défaut.
        // Assurez-vous que le Front envoie toujours la valeur désirée pour 'enabled'.
        user.setEnabled(userDTO.isEnabled());

        userRepository.save(user);

        // Mise à jour Keycloak
        if (user.getKeycloakId() != null) {
            try {
                UserResource userResource = keycloak.realm(realm).users().get(user.getKeycloakId());
                UserRepresentation kcUser = userResource.toRepresentation();

                if (userDTO.getFirstName() != null) kcUser.setFirstName(userDTO.getFirstName());
                if (userDTO.getLastName() != null) kcUser.setLastName(userDTO.getLastName());
                if (userDTO.getEmail() != null) kcUser.setEmail(userDTO.getEmail());

                kcUser.setEnabled(userDTO.isEnabled());

                userResource.update(kcUser);
            } catch (Exception e) {
                System.err.println("⚠️ Admin : Erreur Keycloak (non bloquant) : " + e.getMessage());
            }
        }

        return user;
    }
}