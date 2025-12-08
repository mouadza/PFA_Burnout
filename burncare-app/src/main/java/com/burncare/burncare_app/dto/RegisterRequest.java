package com.burncare.burncare_app.dto;

import com.burncare.burncare_app.entities.Profession;
import com.burncare.burncare_app.entities.Role;

public record RegisterRequest(
        String firstName,
        String lastName,
        String email,
        String password,
        Profession profession,
        Role role,
        Boolean enabled
) {}

