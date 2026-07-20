package com.literandltx.timer.service;

import com.literandltx.timer.dto.preset.TimerPresetRequestDto;
import com.literandltx.timer.dto.preset.TimerPresetResponseDto;
import com.literandltx.timer.model.User;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public interface TimerPresetService {
    TimerPresetResponseDto upsert(TimerPresetRequestDto request, User authUser);

    TimerPresetResponseDto find(LocalDateTime updatedAfter, User authUser);
}
