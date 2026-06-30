package com.literandltx.timer.mapper;

import com.literandltx.timer.dto.entry.TimerEntryCreateRequestDto;
import com.literandltx.timer.dto.entry.TimerEntryUpdateRequestDto;
import com.literandltx.timer.dto.label.LabelCreateRequestDto;
import com.literandltx.timer.dto.label.LabelUpdateRequestDto;
import com.literandltx.timer.dto.option.TimerOptionCreateRequestDto;
import com.literandltx.timer.dto.option.TimerOptionUpdateRequestDto;
import com.literandltx.timer.dto.settings.TimerSettingRequestDto;
import com.literandltx.timer.dto.sync.SyncActionDto;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class SyncPayloadMapper {

    private final JsonMapper jsonMapper;

    public SyncPayloadMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * Converts the {@code Map<String, Object>} payload into the corresponding specific DTO.
     * Returns Optional.empty() if the payload is missing or the action is unsupported.
     */
    public Optional<Object> extractPayload(@NonNull SyncActionDto syncActionDto) {
        if (syncActionDto.getPayload() == null) {
            return Optional.empty();
        }

        String entityType = syncActionDto.getEntityType();
        String action = syncActionDto.getAction();

        Object parsedDto = switch (entityType) {
            case "LABEL" -> switch (action) {
                case "CREATE" -> jsonMapper.convertValue(syncActionDto.getPayload(), LabelCreateRequestDto.class);
                case "UPDATE" -> jsonMapper.convertValue(syncActionDto.getPayload(), LabelUpdateRequestDto.class);
                default -> null;
            };
            case "TIMER_ENTRY" -> switch (action) {
                case "CREATE" -> jsonMapper.convertValue(syncActionDto.getPayload(), TimerEntryCreateRequestDto.class);
                case "UPDATE" -> jsonMapper.convertValue(syncActionDto.getPayload(), TimerEntryUpdateRequestDto.class);
                default -> null;
            };
            case "TIMER_OPTION" -> switch (action) {
                case "CREATE" -> jsonMapper.convertValue(syncActionDto.getPayload(), TimerOptionCreateRequestDto.class);
                case "UPDATE" -> jsonMapper.convertValue(syncActionDto.getPayload(), TimerOptionUpdateRequestDto.class);
                default -> null;
            };
            case "TIMER_SETTING" -> switch (action) {
                case "CREATE", "UPDATE" -> jsonMapper.convertValue(syncActionDto.getPayload(), TimerSettingRequestDto.class);
                default -> null;
            };
            default -> throw new IllegalArgumentException("Unknown entity type: " + entityType);
        };

        return Optional.ofNullable(parsedDto);
    }
}
