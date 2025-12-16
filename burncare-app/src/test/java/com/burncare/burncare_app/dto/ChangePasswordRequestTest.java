package com.burncare.burncare_app.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ChangePasswordRequestTest {

    @Test
    void shouldCreateChangePasswordRequest() {
        // ARRANGE
        String email = "jean@mail.com";
        String newPass = "newSecret123";

        // ACT
        ChangePasswordRequest request = new ChangePasswordRequest(email, newPass);

        // ASSERT
        assertThat(request.email()).isEqualTo(email);
        assertThat(request.newPassword()).isEqualTo(newPass);
    }
}