package com.burncare.burncare_app.dto;

import java.util.List;

public record BurnoutResultRequest(
        Integer burnoutScore,
        String riskLabel,
        String riskTitle,
        String message,
        String recommendation,
        List<Integer> answers
) {}
