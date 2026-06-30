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
import com.literandltx.timer.dto.sync.SyncQueueBulkResponse;
import com.literandltx.timer.mapper.SyncPayloadMapper;
import com.literandltx.timer.model.User;
import com.literandltx.timer.service.LabelService;
import com.literandltx.timer.service.SyncService;
import com.literandltx.timer.service.TimerEntryService;
import com.literandltx.timer.service.TimerOptionService;
import com.literandltx.timer.service.TimerSettingService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    public SyncQueueBulkResponse processQueue(SyncQueueBulkRequest request, User user) {
        SyncQueueBulkResponse response = new SyncQueueBulkResponse();
        List<Long> successfulIds = new ArrayList<>();
        List<SyncQueueBulkResponse.FailedSyncAction> failedActions = new ArrayList<>();

        if (request == null || request.getActions() == null || request.getActions().isEmpty()) {
            log.debug("Received empty or null sync queue bulk request.");
            return response;
        }

        int totalActions = request.getActions().size();
        log.info("Starting sync queue processing. Total actions: {}", totalActions);

        for (SyncActionDto actionDto : request.getActions()) {
            try {
                log.debug("Processing sync action: [{}] for entityType: [{}], entityId: [{}]",
                        actionDto.getAction(), actionDto.getEntityType(), actionDto.getEntityId());

                Object specificDto = null;

                if (!"DELETE".equals(actionDto.getAction())) {
                    specificDto = payloadMapper.extractPayload(actionDto)
                            .orElseThrow(() -> new RuntimeException("Failed to map sync payload."));
                }

                processAction(actionDto, specificDto, user);
                successfulIds.add(actionDto.getId());

                log.debug("Successfully processed sync action for entityId: [{}]", actionDto.getEntityId());
            } catch (Exception e) {
                log.error("Failed action for entityId [{}] of type [{}]. Error: {}",
                        actionDto.getEntityId(), actionDto.getEntityType(), e.getMessage(), e);

                failedActions.add(new SyncQueueBulkResponse.FailedSyncAction(
                        actionDto.getId(),
                        e.getMessage()
                ));
            }
        }

        log.info("Finished sync queue processing. Successes: {}/{}, Failures: {}/{}",
                successfulIds.size(), totalActions, failedActions.size(), totalActions);

        response.setSuccessfulIds(successfulIds);
        response.setFailedActions(failedActions);
        return response;
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
