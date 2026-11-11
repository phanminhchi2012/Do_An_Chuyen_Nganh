package com.example.scalys.repositories;

import com.example.scalys.models.Order;
import com.example.scalys.models.OrderStatus;
import com.example.scalys.models.PaymentStatus;
import com.example.scalys.models.OrderType;
import com.example.scalys.models.PaymentMethod;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("select count(o) from Order o where o.orderType = com.example.scalys.models.OrderType.PURCHASE " +
            "and o.paymentMethod = com.example.scalys.models.PaymentMethod.PAY_LATER " +
            "and o.paymentStatus = com.example.scalys.models.PaymentStatus.NONE " +
            "and o.status = com.example.scalys.models.OrderStatus.NEW " +
            "and o.fulfillmentMethod <> com.example.scalys.models.FulfillmentMethod.SERVICE")
    long countPendingCodOrders();

    boolean existsByProductId(Long productId);
    boolean existsByProductIdAndStatus(Long productId, OrderStatus status);
    boolean existsByPublicCode(String publicCode);
    @Query("select count(o) > 0 from Order o where o.customerAccount.id = :accountId " +
            "and o.orderType = com.example.scalys.models.OrderType.PURCHASE " +
            "and (o.status = com.example.scalys.models.OrderStatus.CONFIRMED " +
            "or o.paymentStatus = com.example.scalys.models.PaymentStatus.PAID)")
    boolean existsSuccessfulPurchaseByAccountId(@Param("accountId") Long accountId);
    List<Order> findByPromotionCodeId(Long promotionCodeId);
    Optional<Order> findByPublicCodeAndPhone(String publicCode, String phone);
    @EntityGraph(attributePaths = "product")
    Page<Order> findByCustomerAccountIdOrderByCreatedAtDescIdDesc(Long customerAccountId, Pageable pageable);
    @EntityGraph(attributePaths = "product")
    List<Order> findAllByOrderByCreatedAtDescIdDesc();

    @EntityGraph(attributePaths = "product")
    List<Order> findByStatusOrderByCreatedAtDescIdDesc(OrderStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    Optional<Order> findByPaymentReference(String reference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.paymentReference = :reference")
    Optional<Order> findByPaymentReferenceForUpdate(@Param("reference") String reference);

    List<Order> findByPaymentStatusAndPaymentExpiresAtBefore(PaymentStatus status, Instant deadline);
    List<Order> findByPaymentMethodAndPaymentStatusIn(PaymentMethod method, List<PaymentStatus> statuses);
    List<Order> findByOrderTypeAndStatusAndHoldExpiresAtBefore(OrderType type, OrderStatus status, Instant deadline);
    List<Order> findByBalanceStatusAndBalanceExpiresAtBefore(PaymentStatus status, Instant deadline);
    List<Order> findByPaymentMethodAndStatusAndCreatedAtBefore(
            PaymentMethod method, OrderStatus status, Instant deadline);
    Optional<Order> findByBalanceReference(String reference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.balanceReference = :reference")
    Optional<Order> findByBalanceReferenceForUpdate(@Param("reference") String reference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.checkoutGroup = :group order by o.id")
    List<Order> findByCheckoutGroupForUpdate(@Param("group") String group);
}
