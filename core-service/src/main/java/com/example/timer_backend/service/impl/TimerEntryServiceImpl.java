package com.example.timer_backend.service.impl;

import com.example.timer_backend.dto.export.ExportResource;
import com.example.timer_backend.dto.timer.entry.CreateTimerEntryRequestDto;
import com.example.timer_backend.dto.timer.entry.CreateTimerEntryResponseDto;
import com.example.timer_backend.dto.timer.entry.TimerEntryRequestDto;
import com.example.timer_backend.dto.timer.entry.TimerEntryResponseDto;
import com.example.timer_backend.mapper.TimerEntryMapper;
import com.example.timer_backend.model.Label;
import com.example.timer_backend.model.TimerEntry;
import com.example.timer_backend.model.User;
import com.example.timer_backend.provider.FileHandlerProvider;
import com.example.timer_backend.provider.FileType;
import com.example.timer_backend.repository.LabelRepository;
import com.example.timer_backend.repository.TimerEntryRepository;
import com.example.timer_backend.service.FileHandler;
import com.example.timer_backend.service.TimerEntryService;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimerEntryServiceImpl implements TimerEntryService {
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final String DEFAULT_COLOR = "#FFFFFF";

    private final TimerEntryRepository timerEntryRepository;
    private final LabelRepository labelRepository;
    private final TimerEntryMapper timerEntryMapper;
    private final FileHandlerProvider fileHandlerProvider;

    @Override
    @Transactional
    public CreateTimerEntryResponseDto save(CreateTimerEntryRequestDto request, User authUser) {
        log.info("Creating timer entry for user id: {}", authUser.getId());

        Label label = labelRepository.findById(request.getLabelId())
                .orElseThrow(() -> new EntityNotFoundException("Label with id " + request.getLabelId() + " not found"));

        validateLabelOwnership(label, authUser);

        TimerEntry timerEntry = timerEntryMapper.toTimerEntry(request, authUser, label);
        TimerEntry savedEntry = timerEntryRepository.save(timerEntry);

        log.info("Timer entry created successfully with id: {}", savedEntry.getId());

        return timerEntryMapper.toCreateTimerEntryResponse(savedEntry);
    }

    @Override
    public TimerEntryResponseDto findById(Long id, User authUser) {
        log.info("Fetching timer entry with id: {}", id);

        TimerEntry timerEntry = timerEntryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TimerEntry with id " + id + " not found"));

        validateTimerEntryOwnership(timerEntry, authUser);

        return timerEntryMapper.toTimerEntryResponse(timerEntry);
    }

    @Override
    @Transactional
    public TimerEntryResponseDto updateById(Long id, TimerEntryRequestDto request, User authUser) {
        log.info("Updating timer entry id: {} for user id: {}", id, authUser.getId());

        TimerEntry existingEntry = timerEntryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TimerEntry with id " + id + " not found"));

        validateTimerEntryOwnership(existingEntry, authUser);

        Label label = labelRepository.findById(request.getLabelId())
                .orElseThrow(() -> new EntityNotFoundException("Label with id " + request.getLabelId() + " not found"));

        validateLabelOwnership(label, authUser);

        timerEntryMapper.updateTimerEntryFromDto(request, label, existingEntry);

        TimerEntry updatedEntry = timerEntryRepository.save(existingEntry);
        return timerEntryMapper.toTimerEntryResponse(updatedEntry);
    }

    @Override
    @Transactional
    public void deleteById(Long id, User authUser) {
        log.info("Deleting timer entry id: {} for user id: {}", id, authUser.getId());

        TimerEntry timerEntry = timerEntryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TimerEntry with id " + id + " not found"));

        validateTimerEntryOwnership(timerEntry, authUser);

        timerEntryRepository.delete(timerEntry);
        log.info("Timer entry with id: {} deleted successfully", id);
    }

    @Override
    public List<TimerEntryResponseDto> findAll(User authUser, Pageable pageable) {
        return timerEntryRepository.findAllByUserId(authUser.getId(), pageable)
                .stream()
                .map(timerEntryMapper::toTimerEntryResponse)
                .toList();
    }

    // todo: add user ability to do import based on certain subscriptions
    // todo: improve performance
    @Override
    @Transactional
    public void importFile(User authUser, MultipartFile file, FileType format) {
        validateFile(file);

        FileHandler fileHandler = fileHandlerProvider.getHandler(format);
        List<TimerEntry> importedEntries = fileHandler.importFile(file);
        Map<String, Label> labelCache = labelRepository.findAllByUserId(authUser.getId())
                .stream()
                .collect(Collectors.toMap(Label::getName, l -> l));
        Set<String> existingEntryKeys = timerEntryRepository.findAllByUserId(authUser.getId())
                .stream()
                .map(this::generateUniqueTimerEntryKey)
                .collect(Collectors.toSet());
        List<TimerEntry> entriesToSave = new ArrayList<>();

        for (TimerEntry newEntry : importedEntries) {
            String labelName = newEntry.getLabel().getName();
            String labelColor = newEntry.getLabel().getColor();
            Label label = labelCache.computeIfAbsent(labelName, name ->
                    labelRepository.save(Label.builder()
                            .name(name)
                            .user(authUser)
                            .color(labelColor != null ? labelColor : DEFAULT_COLOR)
                            .build())
            );

            if (!existingEntryKeys.contains(generateUniqueTimerEntryKey(newEntry))) {
                newEntry.setUser(authUser);
                newEntry.setLabel(label);
                entriesToSave.add(newEntry);
            }
        }

        timerEntryRepository.saveAll(entriesToSave);
    }

    // todo: add user ability to do export based on certain subscriptions
    @Override
    public ExportResource exportFile(User authUser, FileType format) {
        List<TimerEntry> entries = timerEntryRepository.findAllByUserId(authUser.getId());
        FileHandler fileHandler = fileHandlerProvider.getHandler(format);

        byte[] data = fileHandler.exportFile(entries);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"));
        String filename = String.format("export_%s_%s.%s",
                authUser.getUsername(),
                timestamp,
                format.getExtension());

        return new ExportResource(
                new ByteArrayResource(data),
                filename,
                format.getMimeType()
        );
    }

    private void validateTimerEntryOwnership(TimerEntry timerEntry, User authUser) {
        if (!timerEntry.getUser().getId().equals(authUser.getId())) {
            log.warn("Access denied: User {} tried to access timer entry {} owned by user {}",
                    authUser.getId(), timerEntry.getId(), timerEntry.getUser().getId());
            throw new AccessDeniedException("User does not own this timer entry");
        }
    }

    private void validateLabelOwnership(Label label, User authUser) {
        if (!label.getUser().getId().equals(authUser.getId())) {
            log.warn("Access denied: User {} tried to associate label {} owned by user {}",
                    authUser.getId(), label.getId(), label.getUser().getId());
            throw new AccessDeniedException("User does not own this label");
        }
    }

    private void validateFile(final MultipartFile file) {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds the maximum limit of 5MB");
        }
    }

    private String generateUniqueTimerEntryKey(TimerEntry entry) {
        return entry.getStartTime().toString() + "_" + entry.getDurationSeconds();
    }
}
