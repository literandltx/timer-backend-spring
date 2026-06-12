package com.literandltx.timer.service;

import com.literandltx.timer.dto.settings.TimerSettingRequestDto;
import com.literandltx.timer.dto.settings.TimerSettingResponseDto;
import com.literandltx.timer.model.User;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public interface TimerSettingService {
    TimerSettingResponseDto upsert(TimerSettingRequestDto request, User authUser);

    TimerSettingResponseDto find(LocalDateTime updatedAfter, User authUser);
}
