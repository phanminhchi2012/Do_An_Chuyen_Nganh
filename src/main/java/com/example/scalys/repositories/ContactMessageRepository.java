package com.example.scalys.repositories;

import com.example.scalys.models.ContactMessage;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    long countByStatus(ContactMessage.Status status);
    Page<ContactMessage> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
    Page<ContactMessage> findByStatusOrderByCreatedAtDescIdDesc(ContactMessage.Status status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ContactMessage c where c.id = :id")
    Optional<ContactMessage> findByIdForUpdate(@Param("id") Long id);
}
