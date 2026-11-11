package com.example.scalys.repositories;

import com.example.scalys.models.CustomerPromotionCode;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerPromotionCodeRepository extends JpaRepository<CustomerPromotionCode, Long> {
    boolean existsByCode(String code);
    boolean existsByPromotionIdAndOwnerId(Long promotionId, Long ownerId);
    List<CustomerPromotionCode> findByOwnerIdOrderByIdDesc(Long ownerId);
    List<CustomerPromotionCode> findByReservedGroup(String reservedGroup);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CustomerPromotionCode c where c.code = :code")
    Optional<CustomerPromotionCode> findByCodeForUpdate(@Param("code") String code);
}
