package com.burncare.burncare_app.dto;

public record UpdateProfileRequest(
        String email,
        String firstName,
        String lastName
) {}