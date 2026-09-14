package com.flightmanagement.userservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.flightmanagement.userservice.dto.ApiResponseDto;
import com.flightmanagement.userservice.dto.AuthResponseDto;
import com.flightmanagement.userservice.dto.LoginRequestDto;
import com.flightmanagement.userservice.dto.RegisterRequestDto;
import com.flightmanagement.userservice.entity.User;
import com.flightmanagement.userservice.enums.Role;
import com.flightmanagement.userservice.exception.InvalidCredentialsException;
import com.flightmanagement.userservice.exception.UserAlreadyExistsException;
import com.flightmanagement.userservice.repository.UserRepository;
import com.flightmanagement.userservice.security.JwtService;
import com.flightmanagement.userservice.service.impl.AuthServiceImpl;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new User();
        existingUser.setUserId(1L);
        existingUser.setName("John Doe");
        existingUser.setEmail("john@example.com");
        existingUser.setPasswordHash("$2a$hashed");
        existingUser.setRole(Role.CUSTOMER);
    }

    @Test
    void register_success() {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$hashed");
        when(userRepository.save(any())).thenReturn(existingUser);

        ApiResponseDto result = authService.register(request);

        assertEquals("User registered successfully", result.getMessage());
    }

    @Test
    void register_emailAlreadyExists_throwsConflict() {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setEmail("john@example.com");
        request.setPassword("password123");
        request.setName("John");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
    }

    @Test
    void login_success() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("password123", "$2a$hashed")).thenReturn(true);
        when(jwtService.generateToken("john@example.com", "CUSTOMER")).thenReturn("mock.jwt.token");

        AuthResponseDto result = authService.login(request);

        assertNotNull(result.getToken());
        assertEquals("CUSTOMER", result.getRole());
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("john@example.com");
        request.setPassword("wrongpassword");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrongpassword", "$2a$hashed")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_userNotFound_throwsUnauthorized() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("unknown@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }
}
