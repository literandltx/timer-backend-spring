package com.literandltx.timer.service.impl;

import com.literandltx.timer.dto.entry.TimerEntryCreateRequestDto;
import com.literandltx.timer.dto.entry.TimerEntryUpdateRequestDto;
import com.literandltx.timer.dto.label.LabelCreateRequestDto;
import com.literandltx.timer.dto.label.LabelUpdateRequestDto;
import com.literandltx.timer.dto.option.TimerOptionCreateRequestDto;
import com.literandltx.timer.dto.option.TimerOptionUpdateRequestDto;
import com.literandltx.timer.dto.settings.TimerSettingRequestDto;
import com.literandltx.timer.dto.sync.SyncActionDto;
import com.literandltx.timer.dto.sync.SyncQueueBulkRequest;
import com.literandltx.timer.mapper.SyncPayloadMapper;
import com.literandltx.timer.model.User;
import com.literandltx.timer.service.LabelService;
import com.literandltx.timer.service.SyncService;
import com.literandltx.timer.service.TimerEntryService;
import com.literandltx.timer.service.TimerOptionService;
import com.literandltx.timer.service.TimerSettingService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncServiceImpl implements SyncService {

    private final SyncPayloadMapper payloadMapper;
    private final LabelService labelService;
    private final TimerEntryService timerEntryService;
    private final TimerOptionService timerOptionService;
    private final TimerSettingService timerSettingService;

    @Override
    @Transactional
    public boolean processQueue(SyncQueueBulkRequest request, User user) {
        if (request == null || request.getActions() == null) {
            return false;
        }

        for (SyncActionDto actionDto : request.getActions()) {
            try {
                Optional<Object> specificDtoOpt = payloadMapper.extractPayload(actionDto);
                Object specificDto = specificDtoOpt.orElseThrow(() -> {
                    log.error("Failed to parse sync dto. Payload mapped to empty for entityId {} of type {}",
                            actionDto.getEntityId(), actionDto.getEntityType());
                    return new RuntimeException("Failed mapped sync payload.");
                });

                processAction(actionDto, specificDto, user);
            } catch (Exception e) {
                log.error("Failed to process sync action for entityId {} of type {}",
                        actionDto.getEntityId(), actionDto.getEntityType(), e);
                throw new RuntimeException("Sync processing failed", e);
            }
        }

        return true;
    }

    private void processAction(SyncActionDto actionDto, Object payload, User user) {
        String entityType = actionDto.getEntityType();
        String action = actionDto.getAction();
        UUID entityId = actionDto.getEntityId();

        switch (entityType) {
            case "LABEL" -> {
                switch (action) {
                    case "CREATE" -> labelService.save((LabelCreateRequestDto) payload, user);
                    case "UPDATE" -> labelService.update(entityId, (LabelUpdateRequestDto) payload, user);
                    case "DELETE" -> labelService.delete(entityId, user);
                    default -> log.warn("Unsupported action {} for entity type LABEL", action);
                }
            }
            case "TIMER_ENTRY" -> {
                switch (action) {
                    case "CREATE" -> timerEntryService.save((TimerEntryCreateRequestDto) payload, user);
                    case "UPDATE" -> timerEntryService.update(entityId, (TimerEntryUpdateRequestDto) payload, user);
                    case "DELETE" -> timerEntryService.delete(entityId, user);
                    default -> log.warn("Unsupported action {} for entity type TIMER_ENTRY", action);
                }
            }
            case "TIMER_OPTION" -> {
                switch (action) {
                    case "CREATE" -> timerOptionService.save((TimerOptionCreateRequestDto) payload, user);
                    case "UPDATE" -> timerOptionService.update(entityId, (TimerOptionUpdateRequestDto) payload, user);
                    case "DELETE" -> timerOptionService.delete(entityId, user);
                    default -> log.warn("Unsupported action {} for entity type TIMER_OPTION", action);
                }
            }
            case "TIMER_SETTING" -> {
                switch (action) {
                    case "CREATE", "UPDATE" -> timerSettingService.upsert((TimerSettingRequestDto) payload, user);
                    default -> log.warn("Unsupported action {} for entity type TIMER_SETTING", action);
                }
            }
            default -> log.warn("Unknown entity type: {}", entityType);
        }
    }
}
