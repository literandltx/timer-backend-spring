package com.literandltx.timer.service.impl;

import static com.literandltx.timer.validation.OwnershipValidator.validateOwnership;

import com.literandltx.timer.dto.settings.TimerSettingRequestDto;
import com.literandltx.timer.dto.settings.TimerSettingResponseDto;
import com.literandltx.timer.mapper.TimerSettingMapper;
import com.literandltx.timer.model.TimerOption;
import com.literandltx.timer.model.TimerSetting;
import com.literandltx.timer.model.User;
import com.literandltx.timer.repository.TimerOptionRepository;
import com.literandltx.timer.repository.TimerSettingRepository;
import com.literandltx.timer.service.TimerSettingService;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public TimerSettingResponseDto upsert(TimerSettingRequestDto request, User authUser) {
        log.info("Upserting timer settings for user: {}", authUser.getId());

        TimerOption option = timerOptionRepository.findById(request.timerOptionId())
                .orElseThrow(() -> new IllegalArgumentException("Timer option not found"));

        validateOwnership(option, authUser);

        Optional<TimerSetting> existingSetting = timerSettingRepository.findByUserId(authUser.getId());
        TimerSetting timerSetting;

        if (existingSetting.isPresent()) {
            timerSetting = existingSetting.get();
            timerSettingMapper.updateEntityFromDto(request, timerSetting);
            log.debug("Updated existing user's {} setting with UUID: {}", authUser.getId(), timerSetting.getUuid());
        } else {
            timerSetting = timerSettingMapper.toEntity(request, authUser);
            log.debug("Created new user's {} setting with UUID: {}", authUser.getId(), timerSetting.getUuid());
        }

        timerSetting.setPreference(option);
        timerSetting.setLastUpdated(System.currentTimeMillis());

        TimerSetting savedSetting = timerSettingRepository.save(timerSetting);
        return timerSettingMapper.toResponseDto(savedSetting);
    }

    @Override
    public TimerSettingResponseDto find(LocalDateTime updatedAfter, User authUser) {
        TimerSetting setting;

        if (updatedAfter != null) {
            log.debug("Fetching setting updated after: {}", updatedAfter);
            setting = timerSettingRepository.findByUserIdAndUpdatedAtAfter(authUser.getId(), updatedAfter)
                    .orElseThrow(() -> new EntityNotFoundException("No recent settings found"));
        } else {
            log.debug("Fetching current setting");
            setting = timerSettingRepository.findByUserId(authUser.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Setting not found"));
        }

        return timerSettingMapper.toResponseDto(setting);
    }

}
