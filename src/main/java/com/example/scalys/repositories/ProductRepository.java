package com.example.scalys.repositories;

import com.example.scalys.models.Product;
import com.example.scalys.models.ProductStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByOrderByIdAsc();
    List<Product> findByStatusInOrderByIdAsc(Collection<ProductStatus> statuses);
    boolean existsByIndividualCodeAndIdNot(String individualCode, Long id);
    boolean existsByIndividualCode(String individualCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select product from Product product where product.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
