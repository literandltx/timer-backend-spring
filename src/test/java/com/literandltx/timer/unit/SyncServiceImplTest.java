package com.literandltx.timer.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.literandltx.timer.dto.entry.TimerEntryCreateRequestDto;
import com.literandltx.timer.dto.entry.TimerEntryUpdateRequestDto;
import com.literandltx.timer.dto.label.LabelCreateRequestDto;
import com.literandltx.timer.dto.label.LabelUpdateRequestDto;
import com.literandltx.timer.dto.option.TimerOptionCreateRequestDto;
import com.literandltx.timer.dto.option.TimerOptionUpdateRequestDto;
import com.literandltx.timer.dto.preset.TimerPresetRequestDto;
import com.literandltx.timer.dto.sync.SyncActionDto;
import com.literandltx.timer.dto.sync.SyncQueueBulkRequest;
import com.literandltx.timer.dto.sync.SyncQueueBulkResponse;
import com.literandltx.timer.mapper.SyncPayloadMapper;
import com.literandltx.timer.model.User;
import com.literandltx.timer.service.LabelService;
import com.literandltx.timer.service.TimerEntryService;
import com.literandltx.timer.service.TimerOptionService;
import com.literandltx.timer.service.TimerPresetService;
import com.literandltx.timer.service.impl.SyncServiceImpl;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SyncServiceImplTest {

    @Mock
    private SyncPayloadMapper payloadMapper;

    @Mock
    private LabelService labelService;

    @Mock
    private TimerEntryService timerEntryService;

    @Mock
    private TimerOptionService timerOptionService;

    @Mock
    private TimerPresetService timerPresetService;

    @InjectMocks
    private SyncServiceImpl syncService;

    @Test
    void processQueue_WhenRequestIsNull_ShouldReturnEmptyResponse() {
        // 1. Arrange
        User user = mock(User.class);

        // 2. Act
        SyncQueueBulkResponse response = syncService.processQueue(null, user);

        // 3. Assert
        assertTrue(response.getSuccessfulIds().isEmpty());
        assertTrue(response.getFailedActions().isEmpty());
    }

    @Test
    void processQueue_WhenActionsAreEmpty_ShouldReturnEmptyResponse() {
        // 1. Arrange
        User user = mock(User.class);
        SyncQueueBulkRequest request = new SyncQueueBulkRequest();
        request.setActions(Collections.emptyList());

        // 2. Act
        SyncQueueBulkResponse response = syncService.processQueue(request, user);

        // 3. Assert
        assertTrue(response.getSuccessfulIds().isEmpty());
        assertTrue(response.getFailedActions().isEmpty());
    }

    @Test
    void processQueue_WhenActionIsSuccessfulCreate_ShouldAddToSuccessfulIds() {
        // 1. Arrange
        User user = mock(User.class);
        UUID entityId = UUID.randomUUID();

        SyncActionDto actionDto = new SyncActionDto();
        actionDto.setId(1L);
        actionDto.setEntityId(entityId);
        actionDto.setEntityType("LABEL");
        actionDto.setAction("CREATE");

        SyncQueueBulkRequest request = new SyncQueueBulkRequest();
        request.setActions(List.of(actionDto));

        LabelCreateRequestDto payload = mock(LabelCreateRequestDto.class);
        when(payloadMapper.extractPayload(actionDto)).thenReturn(Optional.of(payload));

        // 2. Act
        SyncQueueBulkResponse response = syncService.processQueue(request, user);

        // 3. Assert
        assertEquals(1, response.getSuccessfulIds().size());
        assertEquals(1L, response.getSuccessfulIds().get(0));
        assertTrue(response.getFailedActions().isEmpty());
        verify(labelService).save(payload, user);
    }

    @Test
    void processQueue_WhenActionIsDelete_ShouldSkipPayloadExtractionAndSucceed() {
        // 1. Arrange
        User user = mock(User.class);
        UUID entityId = UUID.randomUUID();

        SyncActionDto actionDto = new SyncActionDto();
        actionDto.setId(2L);
        actionDto.setEntityId(entityId);
        actionDto.setEntityType("TIMER_ENTRY");
        actionDto.setAction("DELETE");

        SyncQueueBulkRequest request = new SyncQueueBulkRequest();
        request.setActions(List.of(actionDto));

        // 2. Act
        SyncQueueBulkResponse response = syncService.processQueue(request, user);

        // 3. Assert
        assertEquals(1, response.getSuccessfulIds().size());
        assertEquals(2L, response.getSuccessfulIds().get(0));
        assertTrue(response.getFailedActions().isEmpty());

        // Verify payloadMapper was never called since action is DELETE
        verify(payloadMapper, never()).extractPayload(any());
        verify(timerEntryService).delete(entityId, user);
    }

    @Test
    void processQueue_WhenPayloadExtractionFails_ShouldAddToFailedActions() {
        // 1. Arrange
        User user = mock(User.class);

        SyncActionDto actionDto = new SyncActionDto();
        actionDto.setId(3L);
        actionDto.setEntityId(UUID.randomUUID());
        actionDto.setEntityType("TIMER_OPTION");
        actionDto.setAction("UPDATE");

        SyncQueueBulkRequest request = new SyncQueueBulkRequest();
        request.setActions(List.of(actionDto));

        when(payloadMapper.extractPayload(actionDto)).thenReturn(Optional.empty());

        // 2. Act
        SyncQueueBulkResponse response = syncService.processQueue(request, user);

        // 3. Assert
        assertTrue(response.getSuccessfulIds().isEmpty());
        assertEquals(1, response.getFailedActions().size());
        assertEquals(3L, response.getFailedActions().get(0).getId());
        assertEquals("Failed to map sync payload.", response.getFailedActions().get(0).getError());
    }

    @Test
    void processQueue_WhenServiceThrowsException_ShouldAddToFailedActions() {
        // 1. Arrange
        User user = mock(User.class);
        UUID entityId = UUID.randomUUID();

        SyncActionDto actionDto = new SyncActionDto();
        actionDto.setId(4L);
        actionDto.setEntityId(entityId);
        actionDto.setEntityType("LABEL");
        actionDto.setAction("CREATE");

        SyncQueueBulkRequest request = new SyncQueueBulkRequest();
        request.setActions(List.of(actionDto));

        LabelCreateRequestDto payload = mock(LabelCreateRequestDto.class);
        when(payloadMapper.extractPayload(actionDto)).thenReturn(Optional.of(payload));

        doThrow(new RuntimeException("Database error")).when(labelService).save(payload, user);

        // 2. Act
        SyncQueueBulkResponse response = syncService.processQueue(request, user);

        // 3. Assert
        assertTrue(response.getSuccessfulIds().isEmpty());
        assertEquals(1, response.getFailedActions().size());
        assertEquals(4L, response.getFailedActions().get(0).getId());
        assertEquals("Database error", response.getFailedActions().get(0).getError());
    }

    @Test
    void processQueue_WhenEntityTypeIsUnknown_ShouldLogWarningAndSucceedWithoutCallingServices() {
        // 1. Arrange
        User user = mock(User.class);

        SyncActionDto actionDto = new SyncActionDto();
        actionDto.setId(5L);
        actionDto.setEntityId(UUID.randomUUID());
        actionDto.setEntityType("UNKNOWN_TYPE");
        actionDto.setAction("CREATE");

        SyncQueueBulkRequest request = new SyncQueueBulkRequest();
        request.setActions(List.of(actionDto));

        Object payload = new Object();
        when(payloadMapper.extractPayload(actionDto)).thenReturn(Optional.of(payload));

        // 2. Act
        SyncQueueBulkResponse response = syncService.processQueue(request, user);

        // 3. Assert
        assertEquals(1, response.getSuccessfulIds().size());
        assertEquals(5L, response.getSuccessfulIds().get(0));
        assertTrue(response.getFailedActions().isEmpty());

        verify(labelService, never()).save(any(), eq(user));
        verify(timerEntryService, never()).save(any(), eq(user));
    }

    @Test
    void processQueue_LabelActions_ShouldCallCorrectLabelServiceMethods() {
        // 1. Arrange
        User user = mock(User.class);
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        UUID id4 = UUID.randomUUID();

        SyncActionDto createAction = new SyncActionDto();
        createAction.setId(1L);
        createAction.setEntityId(id1);
        createAction.setEntityType("LABEL");
        createAction.setAction("CREATE");
        LabelCreateRequestDto createPayload = mock(LabelCreateRequestDto.class);
        when(payloadMapper.extractPayload(createAction)).thenReturn(Optional.of(createPayload));

        SyncActionDto updateAction = new SyncActionDto();
        updateAction.setId(2L);
        updateAction.setEntityId(id2);
        updateAction.setEntityType("LABEL");
        updateAction.setAction("UPDATE");
        LabelUpdateRequestDto updatePayload = mock(LabelUpdateRequestDto.class);
        when(payloadMapper.extractPayload(updateAction)).thenReturn(Optional.of(updatePayload));

        SyncActionDto deleteAction = new SyncActionDto();
        deleteAction.setId(3L);
        deleteAction.setEntityId(id3);
        deleteAction.setEntityType("LABEL");
        deleteAction.setAction("DELETE");

        SyncActionDto unsupportedAction = new SyncActionDto();
        unsupportedAction.setId(4L);
        unsupportedAction.setEntityId(id4);
        unsupportedAction.setEntityType("LABEL");
        unsupportedAction.setAction("UNKNOWN_ACTION");
        when(payloadMapper.extractPayload(unsupportedAction)).thenReturn(Optional.of(new Object()));

        SyncQueueBulkRequest request = new SyncQueueBulkRequest();
        request.setActions(List.of(createAction, updateAction, deleteAction, unsupportedAction));

        // 2. Act
        SyncQueueBulkResponse response = syncService.processQueue(request, user);

        // 3. Assert
        assertEquals(4, response.getSuccessfulIds().size());
        assertTrue(response.getFailedActions().isEmpty());
        verify(labelService).save(createPayload, user);
        verify(labelService).update(id2, updatePayload, user);
        verify(labelService).delete(id3, user);
    }

    @Test
    void processQueue_TimerEntryActions_ShouldCallCorrectTimerEntryServiceMethods() {
        // 1. Arrange
        User user = mock(User.class);
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        UUID id4 = UUID.randomUUID();

        SyncActionDto createAction = new SyncActionDto();
        createAction.setId(1L);
        createAction.setEntityId(id1);
        createAction.setEntityType("TIMER_ENTRY");
        createAction.setAction("CREATE");
        TimerEntryCreateRequestDto createPayload = mock(TimerEntryCreateRequestDto.class);
        when(payloadMapper.extractPayload(createAction)).thenReturn(Optional.of(createPayload));

        SyncActionDto updateAction = new SyncActionDto();
        updateAction.setId(2L);
        updateAction.setEntityId(id2);
        updateAction.setEntityType("TIMER_ENTRY");
        updateAction.setAction("UPDATE");
        TimerEntryUpdateRequestDto updatePayload = mock(TimerEntryUpdateRequestDto.class);
        when(payloadMapper.extractPayload(updateAction)).thenReturn(Optional.of(updatePayload));

        SyncActionDto deleteAction = new SyncActionDto();
        deleteAction.setId(3L);
        deleteAction.setEntityId(id3);
        deleteAction.setEntityType("TIMER_ENTRY");
        deleteAction.setAction("DELETE");

        SyncActionDto unsupportedAction = new SyncActionDto();
        unsupportedAction.setId(4L);
        unsupportedAction.setEntityId(id4);
        unsupportedAction.setEntityType("TIMER_ENTRY");
        unsupportedAction.setAction("UNKNOWN_ACTION");
        when(payloadMapper.extractPayload(unsupportedAction)).thenReturn(Optional.of(new Object()));

        SyncQueueBulkRequest request = new SyncQueueBulkRequest();
        request.setActions(List.of(createAction, updateAction, deleteAction, unsupportedAction));

        // 2. Act
        SyncQueueBulkResponse response = syncService.processQueue(request, user);

        // 3. Assert
        assertEquals(4, response.getSuccessfulIds().size());
        assertTrue(response.getFailedActions().isEmpty());
        verify(timerEntryService).save(createPayload, user);
        verify(timerEntryService).update(id2, updatePayload, user);
        verify(timerEntryService).delete(id3, user);
    }

    @Test
    void processQueue_TimerOptionActions_ShouldCallCorrectTimerOptionServiceMethods() {
        // 1. Arrange
        User user = mock(User.class);
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        UUID id4 = UUID.randomUUID();

        SyncActionDto createAction = new SyncActionDto();
        createAction.setId(1L);
        createAction.setEntityId(id1);
        createAction.setEntityType("TIMER_OPTION");
        createAction.setAction("CREATE");
        TimerOptionCreateRequestDto createPayload = mock(TimerOptionCreateRequestDto.class);
        when(payloadMapper.extractPayload(createAction)).thenReturn(Optional.of(createPayload));

        SyncActionDto updateAction = new SyncActionDto();
        updateAction.setId(2L);
        updateAction.setEntityId(id2);
        updateAction.setEntityType("TIMER_OPTION");
        updateAction.setAction("UPDATE");
        TimerOptionUpdateRequestDto updatePayload = mock(TimerOptionUpdateRequestDto.class);
        when(payloadMapper.extractPayload(updateAction)).thenReturn(Optional.of(updatePayload));

        SyncActionDto deleteAction = new SyncActionDto();
        deleteAction.setId(3L);
        deleteAction.setEntityId(id3);
        deleteAction.setEntityType("TIMER_OPTION");
        deleteAction.setAction("DELETE");

        SyncActionDto unsupportedAction = new SyncActionDto();
        unsupportedAction.setId(4L);
        unsupportedAction.setEntityId(id4);
        unsupportedAction.setEntityType("TIMER_OPTION");
        unsupportedAction.setAction("UNKNOWN_ACTION");
        when(payloadMapper.extractPayload(unsupportedAction)).thenReturn(Optional.of(new Object()));

        SyncQueueBulkRequest request = new SyncQueueBulkRequest();
        request.setActions(List.of(createAction, updateAction, deleteAction, unsupportedAction));

        // 2. Act
        SyncQueueBulkResponse response = syncService.processQueue(request, user);

        // 3. Assert
        assertEquals(4, response.getSuccessfulIds().size());
        assertTrue(response.getFailedActions().isEmpty());
        verify(timerOptionService).save(createPayload, user);
        verify(timerOptionService).update(id2, updatePayload, user);
        verify(timerOptionService).delete(id3, user);
    }

    @Test
    void processQueue_TimerSettingActions_ShouldCallTimerPresetServiceUpsert() {
        // 1. Arrange
        User user = mock(User.class);
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        SyncActionDto createAction = new SyncActionDto();
        createAction.setId(1L);
        createAction.setEntityId(id1);
        createAction.setEntityType("TIMER_SETTING");
        createAction.setAction("CREATE");
        TimerPresetRequestDto createPayload = mock(TimerPresetRequestDto.class);
        when(payloadMapper.extractPayload(createAction)).thenReturn(Optional.of(createPayload));

        SyncActionDto updateAction = new SyncActionDto();
        updateAction.setId(2L);
        updateAction.setEntityId(id2);
        updateAction.setEntityType("TIMER_SETTING");
        updateAction.setAction("UPDATE");
        TimerPresetRequestDto updatePayload = mock(TimerPresetRequestDto.class);
        when(payloadMapper.extractPayload(updateAction)).thenReturn(Optional.of(updatePayload));

        SyncActionDto unsupportedAction = new SyncActionDto();
        unsupportedAction.setId(3L);
        unsupportedAction.setEntityId(id3);
        unsupportedAction.setEntityType("TIMER_SETTING");
        unsupportedAction.setAction("UNKNOWN_ACTION");
        when(payloadMapper.extractPayload(unsupportedAction)).thenReturn(Optional.of(new Object()));

        SyncQueueBulkRequest request = new SyncQueueBulkRequest();
        request.setActions(List.of(createAction, updateAction, unsupportedAction));

        // 2. Act
        SyncQueueBulkResponse response = syncService.processQueue(request, user);

        // 3. Assert
        assertEquals(3, response.getSuccessfulIds().size());
        assertTrue(response.getFailedActions().isEmpty());

        verify(timerPresetService).upsert(createPayload, user);
        verify(timerPresetService).upsert(updatePayload, user);
    }
}
