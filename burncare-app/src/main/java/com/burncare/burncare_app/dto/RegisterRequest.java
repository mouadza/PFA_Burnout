package com.burncare.burncare_app.dto;

import com.burncare.burncare_app.entities.Profession;

public record RegisterRequest(
        String fullName,
        String email,
        String password,
        Profession profession
) {}

