package com.burncare.burncare_app.controllers;

import com.burncare.burncare_app.config.SecurityConfig;
import com.burncare.burncare_app.dto.AdminStatsResponse;
import com.burncare.burncare_app.services.AdminStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminStatsController.class)
@Import(SecurityConfig.class)
class AdminStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminStatsService adminStatsService;

    @Test
    void shouldGetStatsForAdmin() throws Exception {
        // ARRANGE
        AdminStatsResponse stats = AdminStatsResponse.builder()
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

        when(adminStatsService.getStats()).thenReturn(stats);

        // ACT & ASSERT
        mockMvc.perform(get("/api/admin/stats")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(100L))
                .andExpect(jsonPath("$.burnoutTotal").value(50L))
                .andExpect(jsonPath("$.burnoutLow").value(20L))
                .andExpect(jsonPath("$.burnoutMedium").value(20L))
                .andExpect(jsonPath("$.burnoutHigh").value(10L))
                .andExpect(jsonPath("$.fatigueTotal").value(75L))
                .andExpect(jsonPath("$.fatigueAlert").value(25L))
                .andExpect(jsonPath("$.fatigueNonVigilant").value(30L))
                .andExpect(jsonPath("$.fatigueTired").value(20L))
                .andExpect(jsonPath("$.avgFatigueScore").value(65.5));
    }

    @Test
    void shouldReturnForbiddenForNonAdmin() throws Exception {
        // ARRANGE
        AdminStatsResponse stats = AdminStatsResponse.builder()
                .totalUsers(100L)
                .build();

        when(adminStatsService.getStats()).thenReturn(stats);

        // ACT & ASSERT
        mockMvc.perform(get("/api/admin/stats")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnUnauthorizedForAnonymous() throws Exception {
        // ACT & ASSERT
        mockMvc.perform(get("/api/admin/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldHandleZeroStats() throws Exception {
        // ARRANGE
        AdminStatsResponse stats = AdminStatsResponse.builder()
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

        when(adminStatsService.getStats()).thenReturn(stats);

        // ACT & ASSERT
        mockMvc.perform(get("/api/admin/stats")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(0L))
                .andExpect(jsonPath("$.burnoutTotal").value(0L))
                .andExpect(jsonPath("$.fatigueTotal").value(0L))
                .andExpect(jsonPath("$.avgFatigueScore").value(0.0));
    }

    @Test
    void shouldReturnAllStatsFields() throws Exception {
        // ARRANGE
        AdminStatsResponse stats = AdminStatsResponse.builder()
                .totalUsers(200L)
                .burnoutTotal(150L)
                .burnoutLow(60L)
                .burnoutMedium(50L)
                .burnoutHigh(40L)
                .fatigueTotal(180L)
                .fatigueAlert(60L)
                .fatigueNonVigilant(70L)
                .fatigueTired(50L)
                .avgFatigueScore(72.5)
                .build();

        when(adminStatsService.getStats()).thenReturn(stats);

        // ACT & ASSERT
        mockMvc.perform(get("/api/admin/stats")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").exists())
                .andExpect(jsonPath("$.burnoutTotal").exists())
                .andExpect(jsonPath("$.burnoutLow").exists())
                .andExpect(jsonPath("$.burnoutMedium").exists())
                .andExpect(jsonPath("$.burnoutHigh").exists())
                .andExpect(jsonPath("$.fatigueTotal").exists())
                .andExpect(jsonPath("$.fatigueAlert").exists())
                .andExpect(jsonPath("$.fatigueNonVigilant").exists())
                .andExpect(jsonPath("$.fatigueTired").exists())
                .andExpect(jsonPath("$.avgFatigueScore").exists());
    }
}

