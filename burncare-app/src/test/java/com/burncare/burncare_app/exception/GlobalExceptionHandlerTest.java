package com.burncare.burncare_app.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MockController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void handleLockedException_ShouldReturn401_AndCorrectMessage() throws Exception {
        mockMvc.perform(get("/test/locked")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Account Locked"))
                // ✅ CORRECTION : On attend le message lancé par le MockController ("Test Locked")
                .andExpect(jsonPath("$.message").value("Test Locked"));
    }

    @Test
    void handleDisabledException_ShouldReturn401_AndCorrectMessage() throws Exception {
        mockMvc.perform(get("/test/disabled")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Account Disabled"))
                // ✅ CORRECTION : On attend "Test Disabled"
                .andExpect(jsonPath("$.message").value("Test Disabled"));
    }

    @Test
    void handleBadCredentialsException_ShouldReturn401_AndCorrectMessage() throws Exception {
        mockMvc.perform(get("/test/bad-credentials")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Bad Credentials"))
                // ✅ CORRECTION : On attend "Test Bad Creds"
                .andExpect(jsonPath("$.message").value("Test Bad Creds"));
    }

    /**
     * Contrôleur interne factice.
     * C'est lui qui définit les messages que le Handler va relayer.
     */
    @RestController
    static class MockController {
        @GetMapping("/test/locked")
        public void throwLocked() {
            throw new LockedException("Test Locked");
        }

        @GetMapping("/test/disabled")
        public void throwDisabled() {
            throw new DisabledException("Test Disabled");
        }

        @GetMapping("/test/bad-credentials")
        public void throwBadCredentials() {
            throw new BadCredentialsException("Test Bad Creds");
        }
    }
}