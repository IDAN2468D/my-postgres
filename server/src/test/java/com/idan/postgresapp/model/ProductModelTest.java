package com.idan.postgresapp.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ProductModelTest {

    @Test
    void testProductCreationAndGetters() {
        Product product = new Product("Laptop", "Electronics", new BigDecimal("4500.00"), 10);
        product.setId(1L);
        LocalDateTime now = LocalDateTime.now();
        product.setCreatedAt(now);

        assertEquals(1L, product.getId());
        assertEquals("Laptop", product.getName());
        assertEquals("Electronics", product.getCategory());
        assertEquals(new BigDecimal("4500.00"), product.getPrice());
        assertEquals(10, product.getStock());
        assertEquals(now, product.getCreatedAt());
    }

    @Test
    void testProductSetters() {
        Product product = new Product();
        product.setName("Keyboard");
        product.setCategory("Peripherals");
        product.setPrice(new BigDecimal("299.90"));
        product.setStock(25);

        assertEquals("Keyboard", product.getName());
        assertEquals("Peripherals", product.getCategory());
        assertEquals(new BigDecimal("299.90"), product.getPrice());
        assertEquals(25, product.getStock());
    }
}
