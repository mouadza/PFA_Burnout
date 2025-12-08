package com.burncare.burncare_app.services;

import com.burncare.burncare_app.entities.User;
import com.burncare.burncare_app.repositories.UserRepository;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    public AdminService(UserRepository userRepository, Keycloak keycloak) {
        this.userRepository = userRepository;
        this.keycloak = keycloak;
    }

    // 📋 Lister tous les utilisateurs
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 🗑️ Supprimer un utilisateur (Local + Keycloak)
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // 1. Suppression Keycloak
        if (user.getKeycloakId() != null) {
            try {
                keycloak.realm(realm).users().get(user.getKeycloakId()).remove();
                System.out.println("✅ User supprimé de Keycloak: " + user.getEmail());
            } catch (Exception e) {
                System.err.println("⚠️ Erreur suppression Keycloak (déjà supprimé ?): " + e.getMessage());
            }
        }

        // 2. Suppression Locale
        userRepository.delete(user);
        System.out.println("✅ User supprimé de la BDD locale: " + user.getEmail());
    }
}