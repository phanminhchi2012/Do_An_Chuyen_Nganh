package com.example.scalys.repositories;

import com.example.scalys.models.Promotion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    List<Promotion> findAllByOrderByCreatedAtDescIdDesc();
    List<Promotion> findByEnabledTrueOrderByIdAsc();
    boolean existsByCodePrefix(String codePrefix);
}
