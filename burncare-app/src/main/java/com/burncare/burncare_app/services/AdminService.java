package com.burncare.burncare_app.services;

import com.burncare.burncare_app.entities.User;
import com.burncare.burncare_app.repositories.UserRepository;
import com.burncare.burncare_app.repositories.BurnoutResultRepository;
import com.burncare.burncare_app.repositories.FatigueResultRepository;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final Keycloak keycloak;
    private final BurnoutResultRepository burnoutResultRepository;
    private final FatigueResultRepository fatigueResultRepository;

    @Value("${keycloak.realm}")
    private String realm;

    public AdminService(UserRepository userRepository, Keycloak keycloak, 
                       BurnoutResultRepository burnoutResultRepository,
                       FatigueResultRepository fatigueResultRepository) {
        this.userRepository = userRepository;
        this.keycloak = keycloak;
        this.burnoutResultRepository = burnoutResultRepository;
        this.fatigueResultRepository = fatigueResultRepository;
    }

    // 📋 Lister tous les utilisateurs
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 🗑️ Supprimer un utilisateur (Local + Keycloak)
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // 1. Supprimer tous les résultats de burnout associés
        try {
            List<com.burncare.burncare_app.entities.BurnoutResult> burnoutResults = 
                burnoutResultRepository.findByUserOrderByCreatedAtDesc(user);
            if (!burnoutResults.isEmpty()) {
                burnoutResultRepository.deleteAll(burnoutResults);
                System.out.println("✅ " + burnoutResults.size() + " résultat(s) de burnout supprimé(s) pour l'utilisateur: " + user.getEmail());
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur lors de la suppression des résultats de burnout: " + e.getMessage());
        }

        // 2. Supprimer tous les résultats de fatigue associés (utilise keycloakId)
        try {
            if (user.getKeycloakId() != null) {
                List<com.burncare.burncare_app.entities.FatigueResult> fatigueResults = 
                    fatigueResultRepository.findByUserIdOrderByCreatedAtDesc(user.getKeycloakId());
                if (!fatigueResults.isEmpty()) {
                    fatigueResultRepository.deleteAll(fatigueResults);
                    System.out.println("✅ " + fatigueResults.size() + " résultat(s) de fatigue supprimé(s) pour l'utilisateur: " + user.getEmail());
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur lors de la suppression des résultats de fatigue: " + e.getMessage());
        }

        // 3. Suppression Keycloak
        if (user.getKeycloakId() != null) {
            try {
                keycloak.realm(realm).users().get(user.getKeycloakId()).remove();
                System.out.println("✅ User supprimé de Keycloak: " + user.getEmail());
            } catch (Exception e) {
                System.err.println("⚠️ Erreur suppression Keycloak (déjà supprimé ?): " + e.getMessage());
            }
        }

        // 4. Suppression Locale
        userRepository.delete(user);
        System.out.println("✅ User supprimé de la BDD locale: " + user.getEmail());
    }
}