package com.example.timer_backend.service;

import com.example.timer_backend.dto.timer.setting.CreateTimerSettingRequestDto;
import com.example.timer_backend.dto.timer.setting.CreateTimerSettingResponseDto;
import com.example.timer_backend.dto.timer.setting.TimerSettingRequestDto;
import com.example.timer_backend.dto.timer.setting.TimerSettingResponseDto;
import com.example.timer_backend.model.User;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public interface TimerSettingService {
    CreateTimerSettingResponseDto save(CreateTimerSettingRequestDto request, User authUser);

    TimerSettingResponseDto findById(Long id, User authUser);

    TimerSettingResponseDto updateById(Long id, TimerSettingRequestDto request, User authUser);

    void deleteById(Long id, User authUser);

    List<TimerSettingResponseDto> findAll(User user, Pageable pageable);
}
