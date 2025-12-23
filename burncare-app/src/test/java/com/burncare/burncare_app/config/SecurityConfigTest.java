package com.burncare.burncare_app.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SecurityConfigTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Autowired
    private SecurityConfig securityConfig;

    @Test
    void shouldCreatePasswordEncoder() {
        // ASSERT
        assertThat(passwordEncoder).isNotNull();
    }

    @Test
    void shouldEncodePassword() {
        // ARRANGE
        String rawPassword = "testPassword123";

        // ACT
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // ASSERT
        assertThat(encodedPassword)
                .isNotNull()
                .isNotEqualTo(rawPassword)
                .hasSizeGreaterThan(0);
    }

    @Test
    void shouldMatchEncodedPassword() {
        // ARRANGE
        String rawPassword = "testPassword123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // ACT
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);

        // ASSERT
        assertThat(matches).isTrue();
    }

    @Test
    void shouldNotMatchWrongPassword() {
        // ARRANGE
        String rawPassword = "testPassword123";
        String wrongPassword = "wrongPassword";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // ACT
        boolean matches = passwordEncoder.matches(wrongPassword, encodedPassword);

        // ASSERT
        assertThat(matches).isFalse();
    }

    @Test
    void shouldCreateCorsConfigurationSource() {
        // ASSERT
        assertThat(corsConfigurationSource).isNotNull();
    }

    @Test
    void shouldConfigureCorsWithAllowedOrigins() {
        // ARRANGE
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");

        // ACT
        var corsConfig = corsConfigurationSource.getCorsConfiguration(request);

        // ASSERT
        assertThat(corsConfig).isNotNull();
        assertThat(corsConfig.getAllowedOrigins()).isNotNull();
        assertThat(corsConfig.getAllowedOrigins()).isNotEmpty();
        assertThat(corsConfig.getAllowedMethods()).contains("GET", "POST", "PUT", "DELETE", "OPTIONS");
        assertThat(corsConfig.getAllowCredentials()).isTrue();
    }

    @Test
    void shouldCreateJwtAuthenticationConverter() {
        // ACT
        var converter = securityConfig.jwtAuthenticationConverter();

        // ASSERT
        assertThat(converter)
                .isNotNull()
                .isInstanceOf(org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter.class);
    }
}

