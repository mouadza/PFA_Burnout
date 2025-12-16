package com.burncare.burncare_app.dto;

import com.burncare.burncare_app.entities.Profession;
import com.burncare.burncare_app.entities.Role;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldFail_WhenEmailIsInvalid() {
        // ARRANGE : Email incorrect (pas de @)
        RegisterRequest req = new RegisterRequest(
                "Jean", "Dupont", "mail-invalide", "password123",
                Profession.MEDECIN, Role.USER, true
        );

        // ACT
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);

        // ASSERT
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Format d'email invalide");
    }

    @Test
    void shouldFail_WhenPasswordIsTooShort() {
        // ARRANGE : Mot de passe de 3 caractères (min 6 requis)
        RegisterRequest req = new RegisterRequest(
                "Jean", "Dupont", "jean@test.com", "123",
                Profession.MEDECIN, Role.USER, true
        );

        // ACT
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);

        // ASSERT
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Le mot de passe doit faire au moins 6 caractères");
    }

    @Test
    void shouldPass_WhenAllFieldsAreValid() {
        // ARRANGE
        RegisterRequest req = new RegisterRequest(
                "Jean", "Dupont", "jean@test.com", "password123",
                Profession.MEDECIN, Role.USER, true
        );

        // ACT
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);

        // ASSERT
        assertThat(violations).isEmpty();
    }
}