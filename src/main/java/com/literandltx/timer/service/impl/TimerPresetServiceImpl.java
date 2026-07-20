package com.literandltx.timer.service.impl;

import static com.literandltx.timer.validation.OwnershipValidator.validateOwnership;

import com.literandltx.timer.dto.preset.TimerPresetRequestDto;
import com.literandltx.timer.dto.preset.TimerPresetResponseDto;
import com.literandltx.timer.dto.sync.SyncAction;
import com.literandltx.timer.mapper.TimerPresetMapper;
import com.literandltx.timer.model.Label;
import com.literandltx.timer.model.TimerOption;
import com.literandltx.timer.model.TimerPreset;
import com.literandltx.timer.model.User;
import com.literandltx.timer.repository.LabelRepository;
import com.literandltx.timer.repository.TimerOptionRepository;
import com.literandltx.timer.repository.TimerPresetRepository;
import com.literandltx.timer.service.TimerPresetService;
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
public class TimerPresetServiceImpl implements TimerPresetService {

    private static final String WS_DESTINATION = "/queue/timer-presets";

    private final WebSocketBroadcastService broadcastService;
    private final TimerPresetRepository timerPresetRepository;
    private final TimerOptionRepository timerOptionRepository;
    private final LabelRepository labelRepository;
    private final TimerPresetMapper timerPresetMapper;

    @Override
    @Transactional
    public TimerPresetResponseDto upsert(TimerPresetRequestDto request, User authUser) {
        log.info("Upserting timer preset for user: {}", authUser.getId());

        TimerOption option = timerOptionRepository.findById(request.timerOptionUuid())
                .orElseThrow(() -> new EntityNotFoundException("Timer option with id " + request.timerOptionUuid() + " not found"));
        validateOwnership(option, authUser);

        Label label = labelRepository.findById(request.labelUuid())
                .orElseThrow(() -> new EntityNotFoundException("Label with id " + request.labelUuid() + " not found"));
        validateOwnership(label, authUser);

        Optional<TimerPreset> existingPreset = timerPresetRepository.findByUserId(authUser.getId());
        TimerPreset timerPreset;

        if (existingPreset.isPresent()) {
            timerPreset = existingPreset.get();
            timerPresetMapper.updateEntityFromDto(request, timerPreset);
            log.debug("Updated existing user's {} preset with UUID: {}", authUser.getId(), timerPreset.getUuid());
        } else {
            timerPreset = timerPresetMapper.toEntity(request, authUser);
            log.debug("Created new user's {} preset with UUID: {}", authUser.getId(), timerPreset.getUuid());
        }

        timerPreset.setTimerOption(option);
        timerPreset.setLabel(label);
        timerPreset.setLastUpdated(System.currentTimeMillis());
        timerPreset.setUpdatedAt(LocalDateTime.now());

        TimerPreset savedPreset = timerPresetRepository.save(timerPreset);
        TimerPresetResponseDto response = timerPresetMapper.toResponseDto(savedPreset);

        broadcastService.broadcast(authUser.getEmail(), WS_DESTINATION, SyncAction.UPDATE, response);

        return response;
    }

    @Override
    public TimerPresetResponseDto find(LocalDateTime updatedAfter, User authUser) {
        TimerPreset preset;

        if (updatedAfter != null) {
            log.debug("Fetching preset updated after: {}", updatedAfter);
            Optional<TimerPreset> deltaPreset = timerPresetRepository.findByUserIdAndUpdatedAtAfter(authUser.getId(), updatedAfter);

            if (deltaPreset.isEmpty()) {
                log.debug("No preset updates found after {} for user {}", updatedAfter, authUser.getId());
                return null;
            }

            preset = deltaPreset.get();
        } else {
            log.debug("Fetching current preset");
            Optional<TimerPreset> currentPreset = timerPresetRepository.findByUserId(authUser.getId());

            if (currentPreset.isEmpty()) {
                log.debug("No preset found for user {}", authUser.getId());
                return null;
            }

            preset = currentPreset.get();
        }

        return timerPresetMapper.toResponseDto(preset);
    }

}
