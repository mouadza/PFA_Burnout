package com.burncare.burncare_app.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FatigueResultResponse {
    private Long id;
    private Integer fatigueScore;
    private String riskLabel;
    private String riskTitle;
    private String message;
    private Double confidence;

    private String recommendationsJson;
    private String recommendationText;

    private Instant createdAt;
}
