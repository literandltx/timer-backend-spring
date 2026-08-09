package com.literandltx.timer.service;

import com.literandltx.timer.dto.user.UserChangeEmailRequestDto;
import com.literandltx.timer.dto.user.UserChangePasswordRequestDto;
import com.literandltx.timer.dto.user.register.UserRegistrationRequestDto;
import com.literandltx.timer.dto.user.register.UserRegistrationResponseDto;
import com.literandltx.timer.model.User;
import org.springframework.stereotype.Service;

@Service
public interface UserService {
    UserRegistrationResponseDto register(UserRegistrationRequestDto request);

    void changeEmail(UserChangeEmailRequestDto request, User user);

    void changePassword(UserChangePasswordRequestDto request, User user);

    void deleteUser(User user);
}
