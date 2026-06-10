package com.literandltx.timer.service;

import com.literandltx.timer.dto.user.UserRegistrationRequestDto;
import com.literandltx.timer.dto.user.UserRegistrationResponseDto;
import org.springframework.stereotype.Service;

@Service
public interface UserService {
    UserRegistrationResponseDto register(UserRegistrationRequestDto request);
}
