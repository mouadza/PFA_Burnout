package com.burncare.burncare_app.repositories;

import com.burncare.burncare_app.entities.FatigueResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FatigueResultRepository extends JpaRepository<FatigueResult, Long> {
    List<FatigueResult> findByUserIdOrderByCreatedAtDesc(String userId);

    @Query("SELECT COUNT(f) FROM FatigueResult f")
    long countAll();

    long countByRiskLabel(String riskLabel);

    @Query("SELECT AVG(f.fatigueScore) FROM FatigueResult f")
    Double averageFatigueScore();

}
