package com.example.scalys.repositories;

import com.example.scalys.models.StaffAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffAccountRepository extends JpaRepository<StaffAccount, Long> {
    Optional<StaffAccount> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<StaffAccount> findAllByOrderByCreatedAtDescIdDesc();
}
