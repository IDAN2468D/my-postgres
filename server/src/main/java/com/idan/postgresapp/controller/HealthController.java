package com.idan.postgresapp.controller;

import com.idan.postgresapp.model.Order;
import com.idan.postgresapp.model.Product;
import com.idan.postgresapp.repository.AuditLogRepository;
import com.idan.postgresapp.repository.OrderRepository;
import com.idan.postgresapp.repository.ProductRepository;
import com.idan.postgresapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api")
public class HealthController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Value("${spring.datasource.url:mydb}")
    private String dbUrl;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> response = new HashMap<>();
        try {
            String version = jdbcTemplate.queryForObject("SELECT version()", String.class);
            String serverTime = jdbcTemplate.queryForObject("SELECT NOW()::text", String.class);

            response.put("status", "success");
            response.put("message", "PostgreSQL (Java Spring Boot) מחובר ופועל בהצלחה!");
            response.put("database", "mydb");
            response.put("server_time", serverTime);
            response.put("version", version);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Product> allProducts = productRepository.findAll();
            List<Order> allOrders = orderRepository.findAll();
            long usersCount = userRepository.count();
            long auditCount = auditLogRepository.count();

            long productsCount = allProducts.size();
            long ordersCount = allOrders.size();
            long lowStockCount = allProducts.stream()
                    .filter(p -> p.getStock() != null && p.getStock() <= 5)
                    .count();

            BigDecimal totalRevenue = orderRepository.sumTotalRevenue();
            BigDecimal inventoryValue = productRepository.sumTotalInventoryValue();

            // Category distribution analytics
            Map<String, Map<String, Object>> categoryMap = new LinkedHashMap<>();
            for (Product p : allProducts) {
                String cat = (p.getCategory() != null && !p.getCategory().isBlank()) ? p.getCategory() : "כללי";
                Map<String, Object> entry = categoryMap.computeIfAbsent(cat, k -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("category", k);
                    m.put("count", 0);
                    m.put("stock", 0);
                    m.put("value", BigDecimal.ZERO);
                    return m;
                });
                int stock = p.getStock() != null ? p.getStock() : 0;
                BigDecimal val = (p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(stock));
                entry.put("count", (int) entry.get("count") + 1);
                entry.put("stock", (int) entry.get("stock") + stock);
                entry.put("value", ((BigDecimal) entry.get("value")).add(val));
            }

            // Orders by status analytics
            Map<String, Integer> ordersByStatus = new LinkedHashMap<>();
            ordersByStatus.put("completed", 0);
            ordersByStatus.put("shipped", 0);
            ordersByStatus.put("pending", 0);
            ordersByStatus.put("cancelled", 0);
            for (Order o : allOrders) {
                String st = o.getStatus() != null ? o.getStatus().toLowerCase() : "pending";
                ordersByStatus.put(st, ordersByStatus.getOrDefault(st, 0) + 1);
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("products", productsCount);
            stats.put("low_stock_count", lowStockCount);
            stats.put("users", usersCount);
            stats.put("orders", ordersCount);
            stats.put("audit_count", auditCount);
            stats.put("total_revenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
            stats.put("inventory_value", inventoryValue != null ? inventoryValue : BigDecimal.ZERO);
            stats.put("categories", new ArrayList<>(categoryMap.values()));
            stats.put("orders_by_status", ordersByStatus);

            response.put("status", "success");
            response.put("stats", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
