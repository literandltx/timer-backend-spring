package com.literandltx.timer.service.impl;

import static com.literandltx.timer.validation.OwnershipValidator.validateOwnership;

import com.literandltx.timer.dto.settings.TimerSettingRequestDto;
import com.literandltx.timer.dto.settings.TimerSettingResponseDto;
import com.literandltx.timer.dto.sync.SyncAction;
import com.literandltx.timer.mapper.TimerSettingMapper;
import com.literandltx.timer.model.TimerOption;
import com.literandltx.timer.model.TimerSetting;
import com.literandltx.timer.model.User;
import com.literandltx.timer.repository.TimerOptionRepository;
import com.literandltx.timer.repository.TimerSettingRepository;
import com.literandltx.timer.service.TimerSettingService;
import com.literandltx.timer.service.WebSocketBroadcastService;
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

    private static final String WS_DESTINATION = "/queue/timer-settings";

    private final WebSocketBroadcastService broadcastService;
    private final TimerSettingRepository timerSettingRepository;
    private final TimerOptionRepository timerOptionRepository;
    private final TimerSettingMapper timerSettingMapper;

    @Override
    @Transactional
    public TimerSettingResponseDto upsert(TimerSettingRequestDto request, User authUser) {
        log.info("Upserting timer settings for user: {}", authUser.getId());

        TimerOption option = timerOptionRepository.findById(request.timerOptionId())
                .orElseThrow(() -> new EntityNotFoundException("Timer option with id " + request.timerOptionId() + " not found"));

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
        timerSetting.setUpdatedAt(LocalDateTime.now());

        TimerSetting savedSetting = timerSettingRepository.save(timerSetting);
        TimerSettingResponseDto response = timerSettingMapper.toResponseDto(savedSetting);

        broadcastService.broadcast(authUser.getEmail(), WS_DESTINATION, SyncAction.UPDATE, response);

        return response;
    }

    @Override
    public TimerSettingResponseDto find(LocalDateTime updatedAfter, User authUser) {
        TimerSetting setting;

        if (updatedAfter != null) {
            log.debug("Fetching setting updated after: {}", updatedAfter);
            Optional<TimerSetting> deltaSetting = timerSettingRepository.findByUserIdAndUpdatedAtAfter(authUser.getId(), updatedAfter);

            if (deltaSetting.isEmpty()) {
                log.debug("No settings updates found after {} for user {}", updatedAfter, authUser.getId());
                return null;
            }

            setting = deltaSetting.get();
        } else {
            log.debug("Fetching current setting");
            setting = timerSettingRepository.findByUserId(authUser.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Setting not found for user " + authUser.getId()));
        }

        return timerSettingMapper.toResponseDto(setting);
    }

}
