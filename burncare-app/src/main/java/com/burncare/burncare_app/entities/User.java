package com.burncare.burncare_app.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ AJOUT IMPORTANT : Le lien avec Keycloak
    @Column(unique = true)
    private String keycloakId;

    private String firstName;
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    // ⚠️ MODIFICATION : J'ai retiré '@Column(nullable = false)'
    // Car avec Keycloak, ce champ sera null dans notre base locale.
    private String password;

    @Enumerated(EnumType.STRING)
    private Profession profession;

    @Enumerated(EnumType.STRING)
    private Role role;

}