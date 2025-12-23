package com.burncare.burncare_app.services;

import com.burncare.burncare_app.dto.AdminStatsResponse;
import com.burncare.burncare_app.repositories.BurnoutResultRepository;
import com.burncare.burncare_app.repositories.FatigueResultRepository;
import com.burncare.burncare_app.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStatsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BurnoutResultRepository burnoutRepo;

    @Mock
    private FatigueResultRepository fatigueRepo;

    @InjectMocks
    private AdminStatsService adminStatsService;

    @BeforeEach
    void setUp() {
        // Setup default mock values
        when(userRepository.count()).thenReturn(100L);
        when(burnoutRepo.countAll()).thenReturn(50L);
        when(burnoutRepo.countByRiskLabel("Faible")).thenReturn(20L);
        when(burnoutRepo.countByRiskLabel("Moyen")).thenReturn(20L);
        when(burnoutRepo.countByRiskLabel("Élevé")).thenReturn(10L);
        when(fatigueRepo.countAll()).thenReturn(75L);
        when(fatigueRepo.countByRiskLabel("Faible")).thenReturn(25L);
        when(fatigueRepo.countByRiskLabel("Moyen")).thenReturn(30L);
        when(fatigueRepo.countByRiskLabel("Élevé")).thenReturn(20L);
        when(fatigueRepo.averageFatigueScore()).thenReturn(65.5);
    }

    @Test
    void shouldGetStatsWithAllData() {
        // ACT
        AdminStatsResponse stats = adminStatsService.getStats();

        // ASSERT
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalUsers()).isEqualTo(100L);
        assertThat(stats.getBurnoutTotal()).isEqualTo(50L);
        assertThat(stats.getBurnoutLow()).isEqualTo(20L);
        assertThat(stats.getBurnoutMedium()).isEqualTo(20L);
        assertThat(stats.getBurnoutHigh()).isEqualTo(10L);
        assertThat(stats.getFatigueTotal()).isEqualTo(75L);
        assertThat(stats.getFatigueAlert()).isEqualTo(25L);
        assertThat(stats.getFatigueNonVigilant()).isEqualTo(30L);
        assertThat(stats.getFatigueTired()).isEqualTo(20L);
        assertThat(stats.getAvgFatigueScore()).isEqualTo(65.5);
    }

    @Test
    void shouldHandleZeroValues() {
        // ARRANGE
        when(userRepository.count()).thenReturn(0L);
        when(burnoutRepo.countAll()).thenReturn(0L);
        when(burnoutRepo.countByRiskLabel("Faible")).thenReturn(0L);
        when(burnoutRepo.countByRiskLabel("Moyen")).thenReturn(0L);
        when(burnoutRepo.countByRiskLabel("Élevé")).thenReturn(0L);
        when(fatigueRepo.countAll()).thenReturn(0L);
        when(fatigueRepo.countByRiskLabel("Faible")).thenReturn(0L);
        when(fatigueRepo.countByRiskLabel("Moyen")).thenReturn(0L);
        when(fatigueRepo.countByRiskLabel("Élevé")).thenReturn(0L);
        when(fatigueRepo.averageFatigueScore()).thenReturn(null);

        // ACT
        AdminStatsResponse stats = adminStatsService.getStats();

        // ASSERT
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalUsers()).isZero();
        assertThat(stats.getBurnoutTotal()).isZero();
        assertThat(stats.getFatigueTotal()).isZero();
        assertThat(stats.getAvgFatigueScore()).isZero();
    }

    @Test
    void shouldHandleNullAverageFatigueScore() {
        // ARRANGE
        when(fatigueRepo.averageFatigueScore()).thenReturn(null);

        // ACT
        AdminStatsResponse stats = adminStatsService.getStats();

        // ASSERT
        assertThat(stats.getAvgFatigueScore()).isZero();
    }

    @Test
    void shouldHandleLargeValues() {
        // ARRANGE
        when(userRepository.count()).thenReturn(10000L);
        when(burnoutRepo.countAll()).thenReturn(5000L);
        when(burnoutRepo.countByRiskLabel("Faible")).thenReturn(2000L);
        when(burnoutRepo.countByRiskLabel("Moyen")).thenReturn(2000L);
        when(burnoutRepo.countByRiskLabel("Élevé")).thenReturn(1000L);
        when(fatigueRepo.countAll()).thenReturn(7500L);
        when(fatigueRepo.countByRiskLabel("Faible")).thenReturn(2500L);
        when(fatigueRepo.countByRiskLabel("Moyen")).thenReturn(3000L);
        when(fatigueRepo.countByRiskLabel("Élevé")).thenReturn(2000L);
        when(fatigueRepo.averageFatigueScore()).thenReturn(75.75);

        // ACT
        AdminStatsResponse stats = adminStatsService.getStats();

        // ASSERT
        assertThat(stats.getTotalUsers()).isEqualTo(10000L);
        assertThat(stats.getBurnoutTotal()).isEqualTo(5000L);
        assertThat(stats.getFatigueTotal()).isEqualTo(7500L);
        assertThat(stats.getAvgFatigueScore()).isEqualTo(75.75);
    }

    @Test
    void shouldCalculateCorrectBurnoutDistribution() {
        // ARRANGE
        when(burnoutRepo.countAll()).thenReturn(100L);
        when(burnoutRepo.countByRiskLabel("Faible")).thenReturn(40L);
        when(burnoutRepo.countByRiskLabel("Moyen")).thenReturn(35L);
        when(burnoutRepo.countByRiskLabel("Élevé")).thenReturn(25L);

        // ACT
        AdminStatsResponse stats = adminStatsService.getStats();

        // ASSERT
        assertThat(stats.getBurnoutTotal()).isEqualTo(100L);
        assertThat(stats.getBurnoutLow()).isEqualTo(40L);
        assertThat(stats.getBurnoutMedium()).isEqualTo(35L);
        assertThat(stats.getBurnoutHigh()).isEqualTo(25L);
        // Verify sum equals total
        assertThat(stats.getBurnoutLow() + stats.getBurnoutMedium() + stats.getBurnoutHigh())
                .isEqualTo(stats.getBurnoutTotal());
    }

    @Test
    void shouldCalculateCorrectFatigueDistribution() {
        // ARRANGE
        when(fatigueRepo.countAll()).thenReturn(100L);
        when(fatigueRepo.countByRiskLabel("Faible")).thenReturn(30L);
        when(fatigueRepo.countByRiskLabel("Moyen")).thenReturn(40L);
        when(fatigueRepo.countByRiskLabel("Élevé")).thenReturn(30L);

        // ACT
        AdminStatsResponse stats = adminStatsService.getStats();

        // ASSERT
        assertThat(stats.getFatigueTotal()).isEqualTo(100L);
        assertThat(stats.getFatigueAlert()).isEqualTo(30L);
        assertThat(stats.getFatigueNonVigilant()).isEqualTo(40L);
        assertThat(stats.getFatigueTired()).isEqualTo(30L);
        // Verify sum equals total
        assertThat(stats.getFatigueAlert() + stats.getFatigueNonVigilant() + stats.getFatigueTired())
                .isEqualTo(stats.getFatigueTotal());
    }

    @Test
    void shouldHandleDecimalAverageScore() {
        // ARRANGE
        when(fatigueRepo.averageFatigueScore()).thenReturn(67.123456);

        // ACT
        AdminStatsResponse stats = adminStatsService.getStats();

        // ASSERT
        assertThat(stats.getAvgFatigueScore()).isEqualTo(67.123456);
    }
}

