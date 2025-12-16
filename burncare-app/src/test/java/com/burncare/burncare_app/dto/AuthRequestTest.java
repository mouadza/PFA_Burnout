package com.burncare.burncare_app.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AuthRequestTest {

    @Test
    void shouldCreateAuthRequest() {
        // ARRANGE
        String email = "test@mail.com";
        String password = "password123";

        // ACT
        AuthRequest request = new AuthRequest(email, password);

        // ASSERT
        assertThat(request.email()).isEqualTo(email);
        assertThat(request.password()).isEqualTo(password);

        // Vérification de l'égalité (feature des records)
        AuthRequest request2 = new AuthRequest(email, password);
        assertThat(request).isEqualTo(request2);
    }
}