package com.flightmanagement.apigateway.filter;

import com.flightmanagement.apigateway.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/swagger-ui",
            "/v3/api-docs",
            "/user-service/v3/api-docs",
            "/flight-service/v3/api-docs",
            "/booking-service/v3/api-docs"
    );

    private static final List<String> ADMIN_METHODS = List.of("POST", "PUT", "DELETE");

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = sanitize(request.getRequestURI());
        String method = request.getMethod().toUpperCase(Locale.ROOT);

        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (path.startsWith("/api/flights/internal") || path.startsWith("/api/users/internal")) {
            log.warn("Blocked external access to internal API: path={}", path);
            writeError(response, HttpStatus.FORBIDDEN, "Internal API access denied");
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeError(response, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.isTokenValid(token)) {
            log.warn("Invalid JWT for path={}", path);
            writeError(response, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
            return;
        }

        String email = jwtUtil.extractEmail(token);
        String role  = jwtUtil.extractRole(token);

        if (path.startsWith("/api/flights") && ADMIN_METHODS.contains(method) && !"ADMIN".equals(role)) {
            log.warn("Access denied: email={} role={} path={}", sanitize(email), role, path);
            writeError(response, HttpStatus.FORBIDDEN, "Access denied: ADMIN role required");
            return;
        }

        MutableHttpServletRequest mutable = new MutableHttpServletRequest(request);
        mutable.putHeader("X-User-Email", email);
        mutable.putHeader("X-User-Role", role);
        mutable.setAttribute("X-User-Email", email);
        mutable.setAttribute("X-User-Role", role);

        filterChain.doFilter(mutable, response);
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[\r\n\t]", "_");
    }
}
