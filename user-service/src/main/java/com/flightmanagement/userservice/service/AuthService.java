package com.flightmanagement.userservice.service;

import com.flightmanagement.userservice.dto.ApiResponseDto;
import com.flightmanagement.userservice.dto.AuthResponseDto;
import com.flightmanagement.userservice.dto.LoginRequestDto;
import com.flightmanagement.userservice.dto.RegisterRequestDto;

public interface AuthService {

    ApiResponseDto register(RegisterRequestDto request);

    AuthResponseDto login(LoginRequestDto request);

}