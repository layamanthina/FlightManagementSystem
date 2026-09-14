package com.flightmanagement.bookingservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.flightmanagement.bookingservice.dto.UserResponseDto;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/api/users/internal/by-email")
    UserResponseDto getUserByEmail(@RequestParam String email);
}
