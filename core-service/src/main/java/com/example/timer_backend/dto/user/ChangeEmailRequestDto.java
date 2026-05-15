package com.example.timer_backend.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangeEmailRequestDto {
    @Email
    @NotBlank
    private String newEmail;
}
