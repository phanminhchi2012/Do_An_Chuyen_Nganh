package com.example.scalys.repositories;

import com.example.scalys.models.EmailOutbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Long> {
    boolean existsByOrderIdAndKind(Long orderId, String kind);
    List<EmailOutbox> findTop20BySentAtIsNullOrderByIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EmailOutbox e where e.id = :id")
    Optional<EmailOutbox> findByIdForUpdate(@Param("id") Long id);
}
