package com.example.timer_backend.dto.user;

import com.example.timer_backend.validation.FieldMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@FieldMatch(
        first = "newPassword",
        second = "confirmationPassword",
        message = "Password and repeat password shouldn't be empty and should be equal"
)
public class UserUpdateRequestDto {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String currentPassword;

    @NotBlank
    @Size(min = 8)
    private String newPassword;

    @NotBlank
    private String confirmationPassword;
}
