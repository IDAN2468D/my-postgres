package com.idan.postgresapp.repository;

import com.idan.postgresapp.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByOrderByIdDesc();

    List<Order> findByStatusIgnoreCaseOrderByIdDesc(String status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE LOWER(o.status) <> 'cancelled'")
    BigDecimal sumTotalRevenue();
}
