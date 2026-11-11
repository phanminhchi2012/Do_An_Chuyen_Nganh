package com.example.scalys.repositories;

import com.example.scalys.models.CustomerAccount;
import java.util.Optional;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, Long> {
    Optional<CustomerAccount> findByEmail(String email);
    Optional<CustomerAccount> findByGoogleSubject(String googleSubject);
    boolean existsByEmail(String email);
    List<CustomerAccount> findAllByOrderByCreatedAtDescIdDesc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CustomerAccount c where c.id = :id")
    Optional<CustomerAccount> findByIdForUpdate(@Param("id") Long id);
}
