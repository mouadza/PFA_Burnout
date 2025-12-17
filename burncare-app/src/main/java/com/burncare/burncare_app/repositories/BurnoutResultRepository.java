package com.burncare.burncare_app.repositories;// package com.burncare.burncare_app.repositories;

import com.burncare.burncare_app.entities.BurnoutResult;
import com.burncare.burncare_app.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BurnoutResultRepository extends JpaRepository<BurnoutResult, Long> {

    List<BurnoutResult> findByUserOrderByCreatedAtDesc(User user);
    @Query("SELECT COUNT(b) FROM BurnoutResult b")
    long countAll();

    long countByRiskLabel(String riskLabel);
}
