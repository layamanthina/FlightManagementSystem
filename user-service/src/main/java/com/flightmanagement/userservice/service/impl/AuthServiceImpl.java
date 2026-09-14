package com.flightmanagement.userservice.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
import com.flightmanagement.userservice.service.AuthService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    @Override
    public ApiResponseDto register(RegisterRequestDto request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration rejected: email already exists email={}", request.getEmail());
            throw new UserAlreadyExistsException("Email already exists");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CUSTOMER);

        userRepository.save(user);

        log.info("User registered: email={}", request.getEmail());
        return new ApiResponseDto("User registered successfully");
    }

    @Override
    public AuthResponseDto login(LoginRequestDto request) {

        User user = userRepository.findByEmail(
                request.getEmail())
                .orElseThrow(() ->
                        new InvalidCredentialsException(
                                "Invalid email or password"));

        boolean validPassword =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPasswordHash());

        if (!validPassword) {
            log.warn("Failed login attempt for email={}", request.getEmail());
            throw new InvalidCredentialsException(
                    "Invalid email or password");
        }

        log.info("Successful login: email={}", request.getEmail());

        String token =
                jwtService.generateToken(user.getEmail(), user.getRole().name());

        return new AuthResponseDto(
                token,
                user.getRole().name(),
                "Login successful");
    }
}