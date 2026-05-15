package com.example.timer_backend.service.impl;

import com.example.timer_backend.dto.timer.setting.CreateTimerSettingRequestDto;
import com.example.timer_backend.dto.timer.setting.CreateTimerSettingResponseDto;
import com.example.timer_backend.dto.timer.setting.TimerSettingRequestDto;
import com.example.timer_backend.dto.timer.setting.TimerSettingResponseDto;
import com.example.timer_backend.mapper.TimerSettingMapper;
import com.example.timer_backend.model.TimerOption;
import com.example.timer_backend.model.TimerSetting;
import com.example.timer_backend.model.User;
import com.example.timer_backend.repository.TimerOptionRepository;
import com.example.timer_backend.repository.TimerSettingRepository;
import com.example.timer_backend.service.TimerSettingService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimerSettingServiceImpl implements TimerSettingService {
    private final TimerSettingRepository timerSettingRepository;
    private final TimerOptionRepository timerOptionRepository;
    private final TimerSettingMapper timerSettingMapper;

    @Override
    @Transactional
    public CreateTimerSettingResponseDto save(CreateTimerSettingRequestDto request, User authUser) {
        log.info("Creating timer settings for user id: {}", authUser.getId());

        TimerOption timerOption = timerOptionRepository.findById(request.getTimerOptionId())
                .orElseThrow(() -> new EntityNotFoundException("TimerOption with id " + request.getTimerOptionId() + " not found"));

        TimerSetting timerSetting = TimerSetting.builder()
                .user(authUser)
                .preference(timerOption)
                .build();

        TimerSetting saved = timerSettingRepository.save(timerSetting);
        log.info("Timer settings created successfully with id: {}", saved.getId());

        return timerSettingMapper.toCreateTimerSettingResponse(saved);
    }

    @Override
    public TimerSettingResponseDto findById(Long id, User authUser) {
        log.info("Fetching timer setting with id: {}", id);

        TimerSetting timerSetting = timerSettingRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("TimerSetting with id " + id + " not found")
        );

        checkOwnership(timerSetting, authUser);

        return timerSettingMapper.toTimerSettingResponse(timerSetting);
    }

    @Override
    @Transactional
    public TimerSettingResponseDto updateById(Long id, TimerSettingRequestDto request, User authUser) {
        log.info("Updating timer setting id: {} for user id: {}", id, authUser.getId());

        TimerSetting existingSetting = timerSettingRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("TimerSetting with id " + id + " not found")
        );

        checkOwnership(existingSetting, authUser);

        TimerOption newOption = timerOptionRepository.findById(request.getTimerOptionId())
                .orElseThrow(() -> new EntityNotFoundException("TimerOption with id " + request.getTimerOptionId() + " not found"));

        existingSetting.setPreference(newOption);

        TimerSetting saved = timerSettingRepository.save(existingSetting);
        return timerSettingMapper.toTimerSettingResponse(saved);
    }

    @Override
    @Transactional
    public void deleteById(Long id, User authUser) {
        log.info("Deleting timer setting id: {} for user id: {}", id, authUser.getId());

        TimerSetting timerSetting = timerSettingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TimerSetting with id " + id + " not found"));

        checkOwnership(timerSetting, authUser);

        timerSettingRepository.delete(timerSetting);
        log.info("Timer setting with id: {} deleted successfully", id);
    }

    @Override
    public List<TimerSettingResponseDto> findAll(User user, Pageable pageable) {
        return timerSettingRepository.findAllByUserId(user.getId(), pageable)
                .stream()
                .map(timerSettingMapper::toTimerSettingResponse)
                .toList();
    }

    private void checkOwnership(TimerSetting timerSetting, User authUser) {
        if (!timerSetting.getUser().getId().equals(authUser.getId())) {
            log.warn("Access denied: User {} tried to access settings {} owned by user {}",
                    authUser.getId(), timerSetting.getId(), timerSetting.getUser().getId());
            throw new AccessDeniedException("User does not own these settings");
        }
    }
}
