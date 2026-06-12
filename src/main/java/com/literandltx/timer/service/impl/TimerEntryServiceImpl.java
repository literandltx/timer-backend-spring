package com.literandltx.timer.service.impl;

import com.literandltx.timer.dto.entry.TimerEntryCreateRequestDto;
import com.literandltx.timer.dto.entry.TimerEntryResponseDto;
import com.literandltx.timer.dto.entry.TimerEntryUpdateRequestDto;
import com.literandltx.timer.mapper.TimerEntryMapper;
import com.literandltx.timer.model.Label;
import com.literandltx.timer.model.TimerEntry;
import com.literandltx.timer.model.User;
import com.literandltx.timer.repository.LabelRepository;
import com.literandltx.timer.repository.TimerEntryRepository;
import com.literandltx.timer.service.TimerEntryService;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimerEntryServiceImpl implements TimerEntryService {

    private final TimerEntryRepository timerEntryRepository;
    private final LabelRepository labelRepository;
    private final TimerEntryMapper timerEntryMapper;

    @Override
    @Transactional
    public TimerEntryResponseDto save(TimerEntryCreateRequestDto request, User authUser) {
        log.info("Creating new timer entry with UUID: {} for user: {}", request.uuid(), authUser.getId());

        Optional<TimerEntry> existingTimerEntry = timerEntryRepository.findById(request.uuid());

        if (existingTimerEntry.isPresent()) {
            log.info("Timer entry already exists, skipping update.");

            TimerEntry entry = existingTimerEntry.get();
            validateTimerEntryOwnership(entry, authUser);

            return timerEntryMapper.toResponseDto(entry);
        }

        TimerEntry timerEntry = timerEntryMapper.toTimerEntry(request, authUser);

        Label label = labelRepository.findById(request.labelId())
                .orElseThrow(() -> new IllegalArgumentException("Label not found"));
        timerEntry.setLabel(label);
        
        TimerEntry savedEntry = timerEntryRepository.save(timerEntry);

        return timerEntryMapper.toResponseDto(savedEntry);
    }

    @Override
    public List<TimerEntryResponseDto> findAll(LocalDateTime updatedAfter, User authUser) {
        List<TimerEntry> entries;

        if (updatedAfter != null) {
            log.info("Fetching delta updates for timer entries after: {}", updatedAfter);
            entries = timerEntryRepository.findByUserIdAndUpdatedAtAfter(authUser.getId(), updatedAfter);
        } else {
            log.info("Fetching all active timer entries from the database");
            entries = timerEntryRepository.findByUserIdAndIsDeletedFalse(authUser.getId());
        }

        return entries.stream()
                .map(timerEntryMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TimerEntryResponseDto update(UUID id, TimerEntryUpdateRequestDto request, User authUser) {
        log.info("Updating timer entry with id: {} for user id: {}", id, authUser.getId());

        TimerEntry existingEntry = timerEntryRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Timer entry with id " + id + " not found")
        );

        validateTimerEntryOwnership(existingEntry, authUser);

        if (request.labelId() != null) {
            Label label = labelRepository.findById(request.labelId()).orElseThrow(
                    () -> new EntityNotFoundException("Label with id " + request.labelId() + " not found")
            );

            validateLabelOwnership(label, authUser);
            existingEntry.setLabel(label);
        }

        if (request.durationSeconds() != null) {
            existingEntry.setDurationSeconds(request.durationSeconds());
        }
        if (request.startTime() != null) {
            existingEntry.setStartTime(request.startTime());
        }

        if (request.updatedAt() != null) {
            existingEntry.setUpdatedAt(request.updatedAt());
        } else {
            log.debug("Timer's entry property updatedAt with id: {} for user id: {} not provided", id, authUser.getId());
            existingEntry.setUpdatedAt(LocalDateTime.now());
        }

        TimerEntry updatedEntry = timerEntryRepository.save(existingEntry);
        log.debug("Successfully updated timer entry with ID: {}", updatedEntry.getUuid());

        return timerEntryMapper.toResponseDto(updatedEntry);
    }

    @Override
    @Transactional
    public void delete(UUID id, User authUser) {
        log.info("Soft deleting timer entry with id: {} for user id: {}", id, authUser.getId());

        TimerEntry entry = timerEntryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Timer entry with id " + id + " not found"));

        validateTimerEntryOwnership(entry, authUser);

        entry.setDeleted(true);
        entry.setUpdatedAt(LocalDateTime.now());
        timerEntryRepository.save(entry);

        log.info("Timer entry with id: {} deleted successfully", id);
    }

    private void validateTimerEntryOwnership(TimerEntry entry, User authUser) {
        if (!entry.getUser().getId().equals(authUser.getId())) {
            log.warn("Access denied: User {} tried to access timer entry {} owned by user {}",
                    authUser.getId(), entry.getUuid(), entry.getUser().getId());
            throw new AccessDeniedException("User do not have permission to access this timer entry");
        }
    }

    private void validateLabelOwnership(Label label, User authUser) {
        if (!label.getUser().getId().equals(authUser.getId())) {
            log.warn("Access denied: User {} tried to assign label {} owned by user {}",
                    authUser.getId(), label.getUuid(), label.getUser().getId());
            throw new AccessDeniedException("User do not have permission to use this label");
        }
    }
}
