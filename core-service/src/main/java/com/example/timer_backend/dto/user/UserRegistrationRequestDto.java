package com.example.timer_backend.dto.user;

import com.example.timer_backend.validation.FieldMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldMatch(
        first = "password",
        second = "repeatPassword",
        message = "Password and repeat password shouldn't be empty and should be equal"
)
public class UserRegistrationRequestDto {
    @Email
    private String email;

    @Size(min = 8)
    private String password;

    @Size(min = 8)
    private String repeatPassword;
}
