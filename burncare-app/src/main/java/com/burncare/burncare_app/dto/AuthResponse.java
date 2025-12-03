package com.burncare.burncare_app.dto;

import com.burncare.burncare_app.entities.Profession;
import com.burncare.burncare_app.entities.Role;

public record AuthResponse(
        String token,
        String fullName,
        String email,
        Role role,
        Profession profession
) {}

