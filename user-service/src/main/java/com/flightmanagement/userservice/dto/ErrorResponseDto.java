package com.flightmanagement.userservice.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponseDto {

    private String message;
    private int status;
    private LocalDateTime timestamp;
}