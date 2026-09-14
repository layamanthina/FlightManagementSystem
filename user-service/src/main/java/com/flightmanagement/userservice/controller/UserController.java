package com.flightmanagement.userservice.controller;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.flightmanagement.userservice.dto.UserResponseDto;
import com.flightmanagement.userservice.entity.User;
import com.flightmanagement.userservice.exception.ResourceNotFoundException;
import com.flightmanagement.userservice.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMe(
            @Parameter(hidden = true) @RequestHeader("X-User-Email") String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        return ResponseEntity.ok(new UserResponseDto(
                user.getUserId(), user.getName(), user.getEmail(), user.getRole().name()));
    }

    @GetMapping("/internal/by-email")
    public ResponseEntity<UserResponseDto> getUserByEmail(@RequestParam String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        return ResponseEntity.ok(new UserResponseDto(
                user.getUserId(), user.getName(), user.getEmail(), user.getRole().name()));
    }
}
