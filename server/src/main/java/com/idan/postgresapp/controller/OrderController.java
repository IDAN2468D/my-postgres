package com.idan.postgresapp.controller;

import com.idan.postgresapp.model.Order;
import com.idan.postgresapp.model.Product;
import com.idan.postgresapp.model.User;
import com.idan.postgresapp.repository.OrderRepository;
import com.idan.postgresapp.repository.ProductRepository;
import com.idan.postgresapp.repository.UserRepository;
import com.idan.postgresapp.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        Map<String, Object> response = new HashMap<>();
        List<Order> orders = orderRepository.findAllByOrderByIdDesc();

        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            String st = status.trim();
            orders = orders.stream()
                    .filter(o -> o.getStatus() != null && o.getStatus().equalsIgnoreCase(st))
                    .collect(Collectors.toList());
        }

        Map<Long, User> userCache = new HashMap<>();
        List<Map<String, Object>> enrichedOrders = new ArrayList<>();

        for (Order o : orders) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", o.getId());
            item.put("userId", o.getUserId());
            item.put("productId", o.getProductId());
            item.put("product_name", o.getProductName() != null ? o.getProductName() : "הזמנה כללית");
            item.put("quantity", o.getQuantity() != null ? o.getQuantity() : 1);
            item.put("total_amount", o.getTotalAmount());
            item.put("status", o.getStatus());
            item.put("order_date", o.getOrderDate());

            String userName = "אורח";
            String userEmail = "";
            if (o.getUserId() != null) {
                User u = userCache.computeIfAbsent(o.getUserId(), id -> userRepository.findById(id).orElse(null));
                if (u != null) {
                    userName = u.getName();
                    userEmail = u.getEmail();
                } else {
                    userName = "משתמש #" + o.getUserId();
                }
            }
            item.put("user_name", userName);
            item.put("user_email", userEmail);

            if (search != null && !search.isBlank()) {
                String q = search.trim().toLowerCase();
                String pName = String.valueOf(item.get("product_name")).toLowerCase();
                if (!userName.toLowerCase().contains(q) &&
                    !pName.contains(q) &&
                    !String.valueOf(o.getId()).contains(q)) {
                    continue;
                }
            }

            enrichedOrders.add(item);
        }

        response.put("status", "success");
        response.put("data", enrichedOrders);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();

        Long userId = payload.get("userId") != null ? Long.valueOf(payload.get("userId").toString()) : null;
        Long productId = payload.get("productId") != null && !payload.get("productId").toString().isBlank()
                ? Long.valueOf(payload.get("productId").toString()) : null;
        Integer quantity = payload.get("quantity") != null
                ? Integer.valueOf(payload.get("quantity").toString()) : 1;
        if (quantity <= 0) quantity = 1;

        String status = payload.get("status") != null && !payload.get("status").toString().isBlank()
                ? payload.get("status").toString().trim().toLowerCase() : "pending";

        BigDecimal totalAmount = null;
        if (payload.get("totalAmount") != null && !payload.get("totalAmount").toString().isBlank()) {
            totalAmount = new BigDecimal(payload.get("totalAmount").toString());
        }

        String productName = payload.get("productName") != null ? payload.get("productName").toString() : null;

        // If a productId is provided, validate & deduct stock automatically
        if (productId != null) {
            Product product = productRepository.findById(productId).orElse(null);
            if (product == null) {
                response.put("status", "error");
                response.put("message", "המוצר שנבחר לא נמצא במסד הנתונים");
                return ResponseEntity.badRequest().body(response);
            }
            int currentStock = product.getStock() != null ? product.getStock() : 0;
            if (currentStock < quantity) {
                response.put("status", "error");
                response.put("message", "אין מספיק מלאי למוצר '" + product.getName() + "' (זמין במלאי: " + currentStock + ")");
                return ResponseEntity.badRequest().body(response);
            }

            // Deduct stock
            product.setStock(currentStock - quantity);
            productRepository.save(product);

            productName = product.getName();
            if (totalAmount == null) {
                totalAmount = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            }
        }

        if (userId == null || totalAmount == null) {
            response.put("status", "error");
            response.put("message", "חובה לבחור לקוח ומוצר (או להזין סכום להזמנה)");
            return ResponseEntity.badRequest().body(response);
        }

        Order order = new Order(userId, productId, productName, quantity, totalAmount, status);
        Order saved = orderRepository.save(order);

        auditLogService.log(
                "CREATE_ORDER",
                "ORDER",
                saved.getId(),
                "נוצרה הזמנה #" + saved.getId() +
                (productName != null ? " (" + productName + " x" + quantity + ")" : "") +
                " בסך ₪" + saved.getTotalAmount()
        );

        response.put("status", "success");
        response.put("message", "ההזמנה נוצרה בהצלחה והמלאי עודכן!");
        response.put("data", saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            response.put("status", "error");
            response.put("message", "הזמנה לא נמצאה");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        String newStatus = payload.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            response.put("status", "error");
            response.put("message", "חובה לציין סטטוס חדש");
            return ResponseEntity.badRequest().body(response);
        }
        newStatus = newStatus.trim().toLowerCase();
        String oldStatus = order.getStatus() != null ? order.getStatus().toLowerCase() : "pending";

        // Restore or re-deduct stock when cancelling/uncancelling an order linked to a product
        if (order.getProductId() != null && order.getQuantity() != null && order.getQuantity() > 0) {
            Product product = productRepository.findById(order.getProductId()).orElse(null);
            if (product != null) {
                int stock = product.getStock() != null ? product.getStock() : 0;
                if (!"cancelled".equals(oldStatus) && "cancelled".equals(newStatus)) {
                    product.setStock(stock + order.getQuantity());
                    productRepository.save(product);
                } else if ("cancelled".equals(oldStatus) && !"cancelled".equals(newStatus)) {
                    if (stock < order.getQuantity()) {
                        response.put("status", "error");
                        response.put("message", "אין מספיק מלאי לשחזור ההזמנה המבוטלת");
                        return ResponseEntity.badRequest().body(response);
                    }
                    product.setStock(stock - order.getQuantity());
                    productRepository.save(product);
                }
            }
        }

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);

        auditLogService.log(
                "UPDATE_ORDER_STATUS",
                "ORDER",
                saved.getId(),
                "סטטוס הזמנה #" + saved.getId() + " שונה מ-" + oldStatus + " ל-" + newStatus
        );

        response.put("status", "success");
        response.put("message", "סטטוס ההזמנה עודכן ל-" + newStatus);
        response.put("data", saved);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteOrder(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        Order existing = orderRepository.findById(id).orElse(null);
        if (existing == null) {
            response.put("status", "error");
            response.put("message", "הזמנה לא נמצאה");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // Restore stock if order wasn't cancelled
        if (existing.getProductId() != null && existing.getQuantity() != null &&
            !"cancelled".equalsIgnoreCase(existing.getStatus())) {
            productRepository.findById(existing.getProductId()).ifPresent(p -> {
                p.setStock((p.getStock() != null ? p.getStock() : 0) + existing.getQuantity());
                productRepository.save(p);
            });
        }

        orderRepository.deleteById(id);
        auditLogService.log(
                "DELETE_ORDER",
                "ORDER",
                id,
                "נמחקה הזמנה #" + id + " בסך ₪" + existing.getTotalAmount()
        );

        response.put("status", "success");
        response.put("message", "ההזמנה נמחקה בהצלחה");
        return ResponseEntity.ok(response);
    }
}
