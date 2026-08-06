package com.literandltx.timer.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserChangeEmailRequestDto {
    @Email
    @NotBlank
    private String newEmail;

    @NotBlank
    private String password;
}
