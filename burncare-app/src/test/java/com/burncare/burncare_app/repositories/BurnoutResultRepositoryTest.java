package com.burncare.burncare_app.repositories;

import com.burncare.burncare_app.entities.BurnoutResult;
import com.burncare.burncare_app.entities.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BurnoutResultRepositoryTest {

    @Autowired
    private BurnoutResultRepository burnoutResultRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByUserOrderByCreatedAtDesc_ShouldReturnOrderedResults() {
        // ARRANGE : Créer un utilisateur
        User user = new User();
        user.setEmail("result@test.com");
        user.setKeycloakId("kc-result");
        user = userRepository.save(user);

        // Créer un résultat ANCIEN (il y a 1 jour)
        BurnoutResult oldResult = new BurnoutResult();
        oldResult.setUser(user);
        oldResult.setBurnoutScore(10);
        oldResult.setCreatedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        burnoutResultRepository.save(oldResult);

        // Créer un résultat RÉCENT (maintenant)
        BurnoutResult newResult = new BurnoutResult();
        newResult.setUser(user);
        newResult.setBurnoutScore(90);
        newResult.setCreatedAt(Instant.now());
        burnoutResultRepository.save(newResult);

        // ACT
        List<BurnoutResult> results = burnoutResultRepository.findByUserOrderByCreatedAtDesc(user);

        // ASSERT
        assertThat(results).hasSize(2);
        // Le premier doit être le plus récent (Desc)
        assertThat(results.get(0).getBurnoutScore()).isEqualTo(90);
        // Le deuxième doit être le plus ancien
        assertThat(results.get(1).getBurnoutScore()).isEqualTo(10);
    }
}