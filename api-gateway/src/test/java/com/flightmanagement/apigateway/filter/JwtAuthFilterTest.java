package com.flightmanagement.apigateway.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.flightmanagement.apigateway.security.JwtUtil;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    // --- Public paths pass through without any token ---

    @Test
    void publicPath_register_passesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");
        jwtAuthFilter.doFilterInternal(request, response, filterChain);
        assertNotNull(filterChain.getRequest(), "Filter chain should have been invoked");
        assertEquals(200, response.getStatus());
    }

    @Test
    void publicPath_login_passesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        jwtAuthFilter.doFilterInternal(request, response, filterChain);
        assertNotNull(filterChain.getRequest());
        assertEquals(200, response.getStatus());
    }

    @Test
    void publicPath_swaggerDocs_passesThrough() throws Exception {
        for (String docsPath : List.of("/user-service/v3/api-docs", "/flight-service/v3/api-docs", "/booking-service/v3/api-docs")) {
            response = new MockHttpServletResponse();
            filterChain = new MockFilterChain();
            MockHttpServletRequest request = new MockHttpServletRequest("GET", docsPath);
            jwtAuthFilter.doFilterInternal(request, response, filterChain);
            assertNotNull(filterChain.getRequest(), docsPath + " should pass through");
            assertEquals(200, response.getStatus());
        }
    }

    // --- Internal API blocked regardless of auth ---

    @Test
    void internalApi_withValidToken_returns403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/flights/internal/1/seats/decrease");
        request.addHeader("Authorization", "Bearer valid.token");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
        assertTrue(response.getContentAsString().contains("Internal API access denied"));
    }

    @Test
    void internalApi_withoutToken_returns403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/flights/internal/1/seats/increase");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
        assertTrue(response.getContentAsString().contains("Internal API access denied"));
    }

    // --- Missing / malformed Authorization header ---

    @Test
    void missingAuthHeader_returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/flights");
        jwtAuthFilter.doFilterInternal(request, response, filterChain);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertTrue(response.getContentAsString().contains("Missing or invalid Authorization header"));
    }

    @Test
    void authHeaderWithoutBearer_returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/flights");
        request.addHeader("Authorization", "Basic sometoken");
        jwtAuthFilter.doFilterInternal(request, response, filterChain);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
    }

    // --- Invalid / expired token ---

    @Test
    void invalidToken_returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/flights");
        request.addHeader("Authorization", "Bearer invalid.token.here");
        when(jwtUtil.isTokenValid("invalid.token.here")).thenReturn(false);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertTrue(response.getContentAsString().contains("Invalid or expired token"));
    }

    // --- Role enforcement: ADMIN-only paths ---

    @Test
    void postFlight_customerRole_returns403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/flights");
        request.addHeader("Authorization", "Bearer valid.token");
        when(jwtUtil.isTokenValid("valid.token")).thenReturn(true);
        when(jwtUtil.extractEmail("valid.token")).thenReturn("user@example.com");
        when(jwtUtil.extractRole("valid.token")).thenReturn("CUSTOMER");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
        assertTrue(response.getContentAsString().contains("ADMIN role required"));
    }

    @Test
    void putFlight_customerRole_returns403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/flights/1");
        request.addHeader("Authorization", "Bearer valid.token");
        when(jwtUtil.isTokenValid("valid.token")).thenReturn(true);
        when(jwtUtil.extractEmail("valid.token")).thenReturn("user@example.com");
        when(jwtUtil.extractRole("valid.token")).thenReturn("CUSTOMER");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
    }

    @Test
    void deleteFlight_customerRole_returns403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/flights/1");
        request.addHeader("Authorization", "Bearer valid.token");
        when(jwtUtil.isTokenValid("valid.token")).thenReturn(true);
        when(jwtUtil.extractEmail("valid.token")).thenReturn("user@example.com");
        when(jwtUtil.extractRole("valid.token")).thenReturn("CUSTOMER");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
    }

    @Test
    void postFlight_adminRole_passesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/flights");
        request.addHeader("Authorization", "Bearer admin.token");
        when(jwtUtil.isTokenValid("admin.token")).thenReturn(true);
        when(jwtUtil.extractEmail("admin.token")).thenReturn("admin@example.com");
        when(jwtUtil.extractRole("admin.token")).thenReturn("ADMIN");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(filterChain.getRequest());
        assertEquals(200, response.getStatus());
    }

    // --- GET flights allowed for any authenticated role ---

    @Test
    void getFlight_customerRole_passesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/flights");
        request.addHeader("Authorization", "Bearer customer.token");
        when(jwtUtil.isTokenValid("customer.token")).thenReturn(true);
        when(jwtUtil.extractEmail("customer.token")).thenReturn("user@example.com");
        when(jwtUtil.extractRole("customer.token")).thenReturn("CUSTOMER");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(filterChain.getRequest());
        assertEquals(200, response.getStatus());
    }

    // --- Header injection into downstream request ---

    @Test
    void validToken_injectsXUserEmailAndRoleHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/bookings/my");
        request.addHeader("Authorization", "Bearer valid.token");
        when(jwtUtil.isTokenValid("valid.token")).thenReturn(true);
        when(jwtUtil.extractEmail("valid.token")).thenReturn("john@example.com");
        when(jwtUtil.extractRole("valid.token")).thenReturn("CUSTOMER");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(filterChain.getRequest());
        assertEquals("john@example.com",
                ((jakarta.servlet.http.HttpServletRequest) filterChain.getRequest()).getHeader("X-User-Email"));
        assertEquals("CUSTOMER",
                ((jakarta.servlet.http.HttpServletRequest) filterChain.getRequest()).getHeader("X-User-Role"));
    }
}
