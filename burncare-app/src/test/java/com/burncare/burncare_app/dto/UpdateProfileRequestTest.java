package com.burncare.burncare_app.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UpdateProfileRequestTest {

    @Test
    void shouldCreateUpdateProfileRequest() {
        // ARRANGE
        String email = "jean@mail.com";
        String firstName = "Jean";
        String lastName = "Dupont";

        // ACT
        UpdateProfileRequest request = new UpdateProfileRequest(email, firstName, lastName);

        // ASSERT
        assertThat(request.email()).isEqualTo(email);
        assertThat(request.firstName()).isEqualTo(firstName);
        assertThat(request.lastName()).isEqualTo(lastName);

        // Vérification toString() (généré automatiquement par record)
        assertThat(request.toString()).contains("Jean", "Dupont");
    }
}