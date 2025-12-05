package com.burncare.burncare_app.dto;

public record ChangePasswordRequest(
        String email,
        String newPassword
) {}
