package com.burncare.burncare_app.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FatigueResultRequest {
    private Integer fatigueScore;
    private String riskLabel;
    private String riskTitle;
    private String message;
    private Double confidence;

    private Object recommendations;     // JSON array (List/Map)
    private String recommendationText;  // optionnel
}
