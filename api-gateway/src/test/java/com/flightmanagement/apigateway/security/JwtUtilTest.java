package com.flightmanagement.apigateway.security;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

class JwtUtilTest {

    private static final String SECRET = "flightmanagementsystemsecurejwtsecretkey123456789";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", SECRET);
    }

    private String buildToken(String email, String role, long expiryMs) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void extractEmail_returnsCorrectSubject() {
        String token = buildToken("john@example.com", "CUSTOMER", 60_000);
        assertEquals("john@example.com", jwtUtil.extractEmail(token));
    }

    @Test
    void extractRole_returnsCorrectRole() {
        String token = buildToken("admin@example.com", "ADMIN", 60_000);
        assertEquals("ADMIN", jwtUtil.extractRole(token));
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        String token = buildToken("user@example.com", "CUSTOMER", 60_000);
        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        String token = buildToken("user@example.com", "CUSTOMER", -1000);
        assertFalse(jwtUtil.isTokenValid(token));
    }

    @Test
    void isTokenValid_tamperedToken_returnsFalse() {
        String token = buildToken("user@example.com", "CUSTOMER", 60_000);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertFalse(jwtUtil.isTokenValid(tampered));
    }

    @Test
    void isTokenValid_randomString_returnsFalse() {
        assertFalse(jwtUtil.isTokenValid("not.a.jwt"));
    }

    @Test
    void extractAllClaims_returnsNonNullClaims() {
        String token = buildToken("user@example.com", "CUSTOMER", 60_000);
        assertNotNull(jwtUtil.extractAllClaims(token));
    }
}
