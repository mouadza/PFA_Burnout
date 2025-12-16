package com.burncare.burncare_app.dto;

import com.burncare.burncare_app.entities.Profession;
import com.burncare.burncare_app.entities.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Le prénom est obligatoire")
        String firstName,

        @NotBlank(message = "Le nom est obligatoire")
        String lastName,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 6, message = "Le mot de passe doit faire au moins 6 caractères")
        String password,

        @NotNull(message = "La profession est obligatoire")
        Profession profession,

        Role role,

        Boolean enabled
) {}