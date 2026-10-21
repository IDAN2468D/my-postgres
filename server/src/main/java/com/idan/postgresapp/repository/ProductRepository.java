package com.idan.postgresapp.repository;

import com.idan.postgresapp.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByOrderByIdDesc();

    List<Product> findByCategoryIgnoreCaseOrderByIdDesc(String category);

    List<Product> findByNameContainingIgnoreCaseOrderByIdDesc(String name);

    List<Product> findByStockLessThanEqualOrderByStockAsc(Integer threshold);

    long countByStockLessThanEqual(Integer threshold);

    @Query("SELECT COALESCE(SUM(p.price * p.stock), 0) FROM Product p")
    BigDecimal sumTotalInventoryValue();
}
