package com.burncare.burncare_app.entities;// package com.burncare.burncare_app.entities;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Table(name = "burnout_results")
public class BurnoutResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 lien vers ton User local
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Integer burnoutScore;     // 0–100
    private String riskLabel;         // "Faible" / "Moyen" / "Élevé"
    private String riskTitle;         // "Risque Élevé"

    @Column(length = 2000)
    private String message;

    @Column(length = 2000)
    private String recommendation;

    // On stocke les réponses brutes en JSON pour l’analyse
    @Column(columnDefinition = "TEXT")
    private String answersJson;

    private Instant createdAt;
}
