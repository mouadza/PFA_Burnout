package com.burncare.burncare_app.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminStatsResponse {

    // Users
    private long totalUsers;

    // Burnout
    private long burnoutTotal;
    private long burnoutLow;
    private long burnoutMedium;
    private long burnoutHigh;

    // Fatigue
    private long fatigueTotal;
    private long fatigueAlert;
    private long fatigueNonVigilant;
    private long fatigueTired;

    private double avgFatigueScore;
}
