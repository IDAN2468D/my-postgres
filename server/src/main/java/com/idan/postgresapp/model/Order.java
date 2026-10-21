package com.idan.postgresapp.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", length = 150)
    private String productName;

    @Column(name = "quantity")
    private Integer quantity = 1;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 50)
    private String status = "pending";

    @Column(name = "order_date", insertable = false, updatable = false)
    private LocalDateTime orderDate;

    public Order() {}

    public Order(Long userId, BigDecimal totalAmount, String status) {
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.quantity = 1;
    }

    public Order(Long userId, Long productId, String productName, Integer quantity, BigDecimal totalAmount, String status) {
        this.userId = userId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity != null ? quantity : 1;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
}
