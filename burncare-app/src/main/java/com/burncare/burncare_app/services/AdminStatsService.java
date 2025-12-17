package com.burncare.burncare_app.services;

import com.burncare.burncare_app.dto.AdminStatsResponse;
import com.burncare.burncare_app.repositories.BurnoutResultRepository;
import com.burncare.burncare_app.repositories.FatigueResultRepository;
import com.burncare.burncare_app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final UserRepository userRepository;
    private final BurnoutResultRepository burnoutRepo;
    private final FatigueResultRepository fatigueRepo;

    public AdminStatsResponse getStats() {

        return AdminStatsResponse.builder()
                // Users
                .totalUsers(userRepository.count())

                // Burnout
                .burnoutTotal(burnoutRepo.countAll())
                .burnoutLow(burnoutRepo.countByRiskLabel("Faible"))
                .burnoutMedium(burnoutRepo.countByRiskLabel("Moyen"))
                .burnoutHigh(burnoutRepo.countByRiskLabel("Élevé"))

                // Fatigue
                .fatigueTotal(fatigueRepo.countAll())
                .fatigueAlert(fatigueRepo.countByRiskLabel("Faible"))
                .fatigueNonVigilant(fatigueRepo.countByRiskLabel("Moyen"))
                .fatigueTired(fatigueRepo.countByRiskLabel("Élevé"))
                .avgFatigueScore(
                        fatigueRepo.averageFatigueScore() != null
                                ? fatigueRepo.averageFatigueScore()
                                : 0.0
                )
                .build();
    }
}

