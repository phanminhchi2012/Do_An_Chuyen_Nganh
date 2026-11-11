package com.example.scalys.repositories;

import com.example.scalys.models.WarrantyClaim;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WarrantyClaimRepository extends JpaRepository<WarrantyClaim, Long> {
    long countByStatus(WarrantyClaim.Status status);
    Page<WarrantyClaim> findAllByOrderByCreatedAtDescIdDesc(Pageable page);
    Page<WarrantyClaim> findByStatusOrderByCreatedAtDescIdDesc(WarrantyClaim.Status status, Pageable page);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WarrantyClaim w where w.id = :id")
    Optional<WarrantyClaim> findByIdForUpdate(@Param("id") Long id);
}
