package com.burncare.burncare_app.repositories;

import com.burncare.burncare_app.entities.Profession;
import com.burncare.burncare_app.entities.Role;
import com.burncare.burncare_app.entities.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail_ShouldReturnUser() {
        // ARRANGE : On insère un utilisateur de test
        User user = new User();
        user.setEmail("test@repo.com");
        user.setFirstName("Tester");
        user.setLastName("Repo");
        user.setRole(Role.USER);
        user.setProfession(Profession.MEDECIN);
        user.setKeycloakId("kc-123");
        user.setEnabled(true);

        userRepository.save(user);

        // ACT : On appelle la méthode à tester
        Optional<User> found = userRepository.findByEmail("test@repo.com");

        // ASSERT : On vérifie le résultat
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Tester");
    }

    @Test
    void existsByEmail_ShouldReturnTrue() {
        // ARRANGE
        User user = new User();
        user.setEmail("exists@repo.com");
        user.setKeycloakId("kc-456");
        userRepository.save(user);

        // ACT
        boolean exists = userRepository.existsByEmail("exists@repo.com");

        // ASSERT
        assertThat(exists).isTrue();
    }

    @Test
    void findByKeycloakId_ShouldReturnUser() {
        // ARRANGE
        User user = new User();
        user.setEmail("kc@repo.com");
        user.setKeycloakId("uuid-unique");
        userRepository.save(user);

        // ACT
        Optional<User> found = userRepository.findByKeycloakId("uuid-unique");

        // ASSERT
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("kc@repo.com");
    }
}