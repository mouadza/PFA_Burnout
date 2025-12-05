package com.burncare.burncare_app.dto;

import com.burncare.burncare_app.entities.Profession;

public record RegisterRequest(
        String firstName,
        String lastName,
        String email,
        String password,
        Profession profession
) {}

