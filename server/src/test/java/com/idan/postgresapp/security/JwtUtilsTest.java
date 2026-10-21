package com.idan.postgresapp.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long expirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", secret);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", expirationMs);
    }

    @Test
    void testGenerateAndValidateToken() {
        String token = jwtUtils.generateToken("test@example.com", "ADMIN", 101L, "Test Admin");
        assertNotNull(token);
        assertTrue(jwtUtils.validateJwtToken(token));

        assertEquals("test@example.com", jwtUtils.getEmailFromToken(token));
        assertEquals("ADMIN", jwtUtils.getRoleFromToken(token));
        assertEquals(101L, jwtUtils.getUserIdFromToken(token));
    }

    @Test
    void testInvalidToken() {
        assertFalse(jwtUtils.validateJwtToken("invalid.token.here"));
    }

    @Test
    void testDefaultRoleWhenNull() {
        String token = jwtUtils.generateToken("user@example.com", null, 202L, "User");
        assertTrue(jwtUtils.validateJwtToken(token));
        assertEquals("USER", jwtUtils.getRoleFromToken(token));
    }
}
