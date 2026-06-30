package com.literandltx.timer.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.literandltx.timer.dto.entry.TimerEntryUpdateRequestDto;
import com.literandltx.timer.dto.label.LabelCreateRequestDto;
import com.literandltx.timer.dto.sync.SyncActionDto;
import com.literandltx.timer.mapper.SyncPayloadMapper;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class SyncPayloadMapperTest {

    @Mock
    private JsonMapper jsonMapper;

    @InjectMocks
    private SyncPayloadMapper syncPayloadMapper;

    @Test
    void extractPayload_WhenPayloadIsNull_ShouldReturnEmptyOptional() {
        // 1. Arrange
        SyncActionDto syncActionDto = mock(SyncActionDto.class);
        when(syncActionDto.getPayload()).thenReturn(null);

        // 2. Act
        Optional<Object> result = syncPayloadMapper.extractPayload(syncActionDto);

        // 3. Assert
        assertTrue(result.isEmpty());
        verifyNoInteractions(jsonMapper);
    }

    @Test
    void extractPayload_WhenLabelCreate_ShouldReturnMappedLabelCreateDto() {
        // 1. Arrange
        Map<String, Object> payloadMap = Map.of("name", "Work");

        LabelCreateRequestDto expectedDto = new LabelCreateRequestDto(
                UUID.randomUUID(),
                "Work",
                "#FFFFFF",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        SyncActionDto syncActionDto = mock(SyncActionDto.class);
        when(syncActionDto.getPayload()).thenReturn(payloadMap);
        when(syncActionDto.getEntityType()).thenReturn("LABEL");
        when(syncActionDto.getAction()).thenReturn("CREATE");

        when(jsonMapper.convertValue(payloadMap, LabelCreateRequestDto.class))
                .thenReturn(expectedDto);

        // 2. Act
        Optional<Object> result = syncPayloadMapper.extractPayload(syncActionDto);

        // 3. Assert
        assertTrue(result.isPresent());
        assertEquals(expectedDto, result.get());
    }

    @Test
    void extractPayload_WhenTimerEntryUpdate_ShouldReturnMappedTimerEntryUpdateDto() {
        // 1. Arrange
        Map<String, Object> payloadMap = Map.of("description", "Updated Task");
        TimerEntryUpdateRequestDto expectedDto = new TimerEntryUpdateRequestDto(
                UUID.randomUUID(),
                1000L,
                2000L,
                LocalDateTime.now()
        );

        SyncActionDto syncActionDto = mock(SyncActionDto.class);
        when(syncActionDto.getPayload()).thenReturn(payloadMap);
        when(syncActionDto.getEntityType()).thenReturn("TIMER_ENTRY");
        when(syncActionDto.getAction()).thenReturn("UPDATE");

        when(jsonMapper.convertValue(payloadMap, TimerEntryUpdateRequestDto.class))
                .thenReturn(expectedDto);

        // 2. Act
        Optional<Object> result = syncPayloadMapper.extractPayload(syncActionDto);

        // 3. Assert
        assertTrue(result.isPresent());
        assertEquals(expectedDto, result.get());
    }

    @Test
    void extractPayload_WhenActionIsUnsupported_ShouldReturnEmptyOptional() {
        // 1. Arrange
        Map<String, Object> payloadMap = Map.of("someKey", "someValue");

        SyncActionDto syncActionDto = mock(SyncActionDto.class);
        when(syncActionDto.getPayload()).thenReturn(payloadMap);
        when(syncActionDto.getEntityType()).thenReturn("TIMER_OPTION");
        when(syncActionDto.getAction()).thenReturn("DELETE");

        // 2. Act
        Optional<Object> result = syncPayloadMapper.extractPayload(syncActionDto);

        // 3. Assert
        assertTrue(result.isEmpty());
        verifyNoInteractions(jsonMapper);
    }

    @Test
    void extractPayload_WhenEntityTypeIsUnknown_ShouldThrowIllegalArgumentException() {
        // 1. Arrange
        Map<String, Object> payloadMap = Map.of("someKey", "someValue");

        SyncActionDto syncActionDto = mock(SyncActionDto.class);
        when(syncActionDto.getPayload()).thenReturn(payloadMap);
        when(syncActionDto.getEntityType()).thenReturn("UNKNOWN_ENTITY");
        when(syncActionDto.getAction()).thenReturn("CREATE");

        // 2. Act & 3. Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> syncPayloadMapper.extractPayload(syncActionDto)
        );

        assertEquals("Unknown entity type: UNKNOWN_ENTITY", exception.getMessage());
        verifyNoInteractions(jsonMapper);
    }
}
