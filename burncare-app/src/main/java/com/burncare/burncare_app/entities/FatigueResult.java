package com.burncare.burncare_app.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Table(name = "fatigue_results")
public class FatigueResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ Keycloak "sub"
    @Column(nullable = false)
    private String userId;

    private Integer fatigueScore;   // 0–100
    private String riskLabel;
    private String riskTitle;

    @Column(columnDefinition = "TEXT")
    private String message;

    private Double confidence;

    @Column(columnDefinition = "TEXT")
    private String recommendationsJson;

    @Column(columnDefinition = "TEXT")
    private String recommendationText;

    @Column(nullable = false)
    private Instant createdAt;
}
