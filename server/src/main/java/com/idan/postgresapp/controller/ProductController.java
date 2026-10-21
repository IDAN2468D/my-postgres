package com.idan.postgresapp.controller;

import com.idan.postgresapp.model.Product;
import com.idan.postgresapp.repository.ProductRepository;
import com.idan.postgresapp.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean lowStock) {
        Map<String, Object> response = new HashMap<>();
        List<Product> products = productRepository.findAllByOrderByIdDesc();

        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase();
            products = products.stream()
                    .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(q)) ||
                                 (p.getCategory() != null && p.getCategory().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        }

        if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
            String cat = category.trim();
            products = products.stream()
                    .filter(p -> p.getCategory() != null && p.getCategory().equalsIgnoreCase(cat))
                    .collect(Collectors.toList());
        }

        if (Boolean.TRUE.equals(lowStock)) {
            products = products.stream()
                    .filter(p -> p.getStock() != null && p.getStock() <= 5)
                    .collect(Collectors.toList());
        }

        response.put("status", "success");
        response.put("data", products);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createProduct(@RequestBody Product product) {
        Map<String, Object> response = new HashMap<>();
        if (product.getName() == null || product.getName().isBlank() || product.getPrice() == null) {
            response.put("status", "error");
            response.put("message", "שם מוצר ומחיר הם שדות חובה");
            return ResponseEntity.badRequest().body(response);
        }
        if (product.getCategory() == null || product.getCategory().isBlank()) {
            product.setCategory("כללי");
        }
        if (product.getStock() == null || product.getStock() < 0) {
            product.setStock(0);
        }
        Product saved = productRepository.save(product);
        auditLogService.log(
                "CREATE_PRODUCT",
                "PRODUCT",
                saved.getId(),
                "נוסף מוצר חדש: " + saved.getName() + " (₪" + saved.getPrice() + ", מלאי: " + saved.getStock() + ")"
        );
        response.put("status", "success");
        response.put("message", "המוצר נוסף בהצלחה!");
        response.put("data", saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateProduct(@PathVariable Long id, @RequestBody Product updated) {
        Map<String, Object> response = new HashMap<>();
        Product existing = productRepository.findById(id).orElse(null);
        if (existing == null) {
            response.put("status", "error");
            response.put("message", "מוצר לא נמצא");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        if (updated.getName() != null && !updated.getName().isBlank()) {
            existing.setName(updated.getName().trim());
        }
        if (updated.getCategory() != null && !updated.getCategory().isBlank()) {
            existing.setCategory(updated.getCategory().trim());
        }
        if (updated.getPrice() != null && updated.getPrice().compareTo(BigDecimal.ZERO) >= 0) {
            existing.setPrice(updated.getPrice());
        }
        if (updated.getStock() != null && updated.getStock() >= 0) {
            existing.setStock(updated.getStock());
        }

        Product saved = productRepository.save(existing);
        auditLogService.log(
                "UPDATE_PRODUCT",
                "PRODUCT",
                saved.getId(),
                "עודכן מוצר: " + saved.getName() + " (מחיר: ₪" + saved.getPrice() + ", מלאי: " + saved.getStock() + ")"
        );

        response.put("status", "success");
        response.put("message", "המוצר עודכן בהצלחה!");
        response.put("data", saved);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<Map<String, Object>> adjustStock(@PathVariable Long id, @RequestBody Map<String, Integer> payload) {
        Map<String, Object> response = new HashMap<>();
        Product existing = productRepository.findById(id).orElse(null);
        if (existing == null) {
            response.put("status", "error");
            response.put("message", "מוצר לא נמצא");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        int oldStock = existing.getStock() != null ? existing.getStock() : 0;
        int newStock = oldStock;
        if (payload.containsKey("delta")) {
            newStock = Math.max(0, oldStock + payload.get("delta"));
        } else if (payload.containsKey("stock")) {
            newStock = Math.max(0, payload.get("stock"));
        }

        existing.setStock(newStock);
        Product saved = productRepository.save(existing);

        auditLogService.log(
                "ADJUST_STOCK",
                "PRODUCT",
                saved.getId(),
                "עודכן מלאי למוצר '" + saved.getName() + "': " + oldStock + " ← " + newStock
        );

        response.put("status", "success");
        response.put("message", "המלאי עודכן בהצלחה");
        response.put("data", saved);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProduct(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        Product existing = productRepository.findById(id).orElse(null);
        if (existing == null) {
            response.put("status", "error");
            response.put("message", "מוצר לא נמצא");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        String prodName = existing.getName();
        productRepository.deleteById(id);
        auditLogService.log(
                "DELETE_PRODUCT",
                "PRODUCT",
                id,
                "נמחק מוצר מהמערכת: " + prodName + " (#" + id + ")"
        );
        response.put("status", "success");
        response.put("message", "המוצר נמחק בהצלחה");
        return ResponseEntity.ok(response);
    }
}
