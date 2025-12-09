package com.burncare.burncare_app.dto;

public record BurnoutResultResponse(
        Long id,
        Integer burnoutScore,
        String riskLabel,
        String riskTitle,
        String createdAt
) {}

