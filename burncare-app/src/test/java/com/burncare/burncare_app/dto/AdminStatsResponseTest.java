package com.burncare.burncare_app.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AdminStatsResponseTest {

    @Test
    void shouldCreateAdminStatsResponseWithBuilder() {
        // ARRANGE & ACT
        AdminStatsResponse response = AdminStatsResponse.builder()
                .totalUsers(100L)
                .burnoutTotal(50L)
                .burnoutLow(20L)
                .burnoutMedium(20L)
                .burnoutHigh(10L)
                .fatigueTotal(75L)
                .fatigueAlert(25L)
                .fatigueNonVigilant(30L)
                .fatigueTired(20L)
                .avgFatigueScore(65.5)
                .build();

        // ASSERT
        assertThat(response.getTotalUsers()).isEqualTo(100L);
        assertThat(response.getBurnoutTotal()).isEqualTo(50L);
        assertThat(response.getBurnoutLow()).isEqualTo(20L);
        assertThat(response.getBurnoutMedium()).isEqualTo(20L);
        assertThat(response.getBurnoutHigh()).isEqualTo(10L);
        assertThat(response.getFatigueTotal()).isEqualTo(75L);
        assertThat(response.getFatigueAlert()).isEqualTo(25L);
        assertThat(response.getFatigueNonVigilant()).isEqualTo(30L);
        assertThat(response.getFatigueTired()).isEqualTo(20L);
        assertThat(response.getAvgFatigueScore()).isEqualTo(65.5);
    }

    @Test
    void shouldCreateAdminStatsResponseWithSetters() {
        // ARRANGE - Using builder since @Data with @Builder doesn't provide no-args constructor
        AdminStatsResponse response = AdminStatsResponse.builder()
                .totalUsers(0L)
                .burnoutTotal(0L)
                .burnoutLow(0L)
                .burnoutMedium(0L)
                .burnoutHigh(0L)
                .fatigueTotal(0L)
                .fatigueAlert(0L)
                .fatigueNonVigilant(0L)
                .fatigueTired(0L)
                .avgFatigueScore(0.0)
                .build();

        // ACT
        response.setTotalUsers(200L);
        response.setBurnoutTotal(100L);
        response.setBurnoutLow(40L);
        response.setBurnoutMedium(35L);
        response.setBurnoutHigh(25L);
        response.setFatigueTotal(150L);
        response.setFatigueAlert(50L);
        response.setFatigueNonVigilant(60L);
        response.setFatigueTired(40L);
        response.setAvgFatigueScore(70.0);

        // ASSERT
        assertThat(response.getTotalUsers()).isEqualTo(200L);
        assertThat(response.getBurnoutTotal()).isEqualTo(100L);
        assertThat(response.getBurnoutLow()).isEqualTo(40L);
        assertThat(response.getBurnoutMedium()).isEqualTo(35L);
        assertThat(response.getBurnoutHigh()).isEqualTo(25L);
        assertThat(response.getFatigueTotal()).isEqualTo(150L);
        assertThat(response.getFatigueAlert()).isEqualTo(50L);
        assertThat(response.getFatigueNonVigilant()).isEqualTo(60L);
        assertThat(response.getFatigueTired()).isEqualTo(40L);
        assertThat(response.getAvgFatigueScore()).isEqualTo(70.0);
    }

    @Test
    void shouldHandleZeroValues() {
        // ARRANGE & ACT
        AdminStatsResponse response = AdminStatsResponse.builder()
                .totalUsers(0L)
                .burnoutTotal(0L)
                .burnoutLow(0L)
                .burnoutMedium(0L)
                .burnoutHigh(0L)
                .fatigueTotal(0L)
                .fatigueAlert(0L)
                .fatigueNonVigilant(0L)
                .fatigueTired(0L)
                .avgFatigueScore(0.0)
                .build();

        // ASSERT
        assertThat(response.getTotalUsers()).isZero();
        assertThat(response.getBurnoutTotal()).isZero();
        assertThat(response.getAvgFatigueScore()).isZero();
    }

    @Test
    void shouldHandleEqualsAndHashCode() {
        // ARRANGE
        AdminStatsResponse response1 = AdminStatsResponse.builder()
                .totalUsers(100L)
                .burnoutTotal(50L)
                .avgFatigueScore(65.5)
                .build();

        AdminStatsResponse response2 = AdminStatsResponse.builder()
                .totalUsers(100L)
                .burnoutTotal(50L)
                .avgFatigueScore(65.5)
                .build();

        AdminStatsResponse response3 = AdminStatsResponse.builder()
                .totalUsers(200L)
                .burnoutTotal(50L)
                .avgFatigueScore(65.5)
                .build();

        // ASSERT
        assertThat(response1)
                .isEqualTo(response2)
                .hasSameHashCodeAs(response2)
                .isNotEqualTo(response3);
    }

    @Test
    void shouldHandleToString() {
        // ARRANGE
        AdminStatsResponse response = AdminStatsResponse.builder()
                .totalUsers(100L)
                .burnoutTotal(50L)
                .build();

        // ACT
        String toString = response.toString();

        // ASSERT
        assertThat(toString)
                .isNotNull()
                .contains("AdminStatsResponse");
    }
}

