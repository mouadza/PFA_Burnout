package com.burncare.burncare_app.repositories;// package com.burncare.burncare_app.repositories;

import com.burncare.burncare_app.entities.BurnoutResult;
import com.burncare.burncare_app.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BurnoutResultRepository extends JpaRepository<BurnoutResult, Long> {

    List<BurnoutResult> findByUserOrderByCreatedAtDesc(User user);
}
