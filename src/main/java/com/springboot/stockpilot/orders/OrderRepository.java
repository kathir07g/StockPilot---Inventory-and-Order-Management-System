package com.springboot.stockpilot.orders;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Optional<Order> findByIdAndCustomer_Id(Long id, Long customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
       SELECT o FROM Order o
       WHERE o.id = :orderId
       AND o.customer.id = :customerId
       """)
    Optional<Order> findByIdAndCustomerIdForUpdate(
            @Param("orderId") Long orderId,
            @Param("customerId") Long customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :orderId")
    Optional<Order> findByIdForUpdate(
            @Param("orderId") Long orderId
    );

    List<Order> findByCustomer_IdOrderByCreatedAtDesc(Long customerId);

}
