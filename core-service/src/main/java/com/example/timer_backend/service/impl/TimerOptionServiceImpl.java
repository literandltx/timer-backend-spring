package com.example.timer_backend.service.impl;

import com.example.timer_backend.dto.timer.option.CreateTimerOptionRequestDto;
import com.example.timer_backend.dto.timer.option.CreateTimerOptionResponseDto;
import com.example.timer_backend.dto.timer.option.TimerOptionRequestDto;
import com.example.timer_backend.dto.timer.option.TimerOptionResponseDto;
import com.example.timer_backend.mapper.TimerOptionMapper;
import com.example.timer_backend.model.TimerOption;
import com.example.timer_backend.model.User;
import com.example.timer_backend.repository.TimerOptionRepository;
import com.example.timer_backend.service.TimerOptionService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimerOptionServiceImpl implements TimerOptionService {
    private final TimerOptionRepository timerOptionRepository;
    private final TimerOptionMapper timerOptionMapper;

    @Override
    public CreateTimerOptionResponseDto save(CreateTimerOptionRequestDto request, User authUser) {
        log.info("Creating new timer option '{}' minutes for user id: {}", request.getValue(), authUser.getId());

        TimerOption timerOption = timerOptionMapper.toTimerOption(request, authUser);
        TimerOption saved = timerOptionRepository.save(timerOption);

        log.info("Timer option created successfully with id: {}", saved.getId());
        return timerOptionMapper.toCreateTimerOptionResponse(saved);
    }

    @Override
    public TimerOptionResponseDto findById(Long id, User authUser) {
        log.info("Fetching timer option with id: {}", id);

        TimerOption timerOption = timerOptionRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("TimerOption with id " + id + " not found")
        );

        checkOwnership(timerOption, authUser);

        log.info("Timer option fetched successfully with id: {}", id);
        return timerOptionMapper.toTimerOptionResponse(timerOption);
    }

    @Override
    @Transactional
    public TimerOptionResponseDto updateById(Long id, TimerOptionRequestDto request, User authUser) {
        log.info("Updating timer option with id: {} for user id: {}", id, authUser.getId());

        TimerOption existingTimerOption = timerOptionRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("TimerOption with id " + id + " not found")
        );

        checkOwnership(existingTimerOption, authUser);

        existingTimerOption.setValue(request.getValue());

        TimerOption savedTimerOption = timerOptionRepository.save(existingTimerOption);
        return timerOptionMapper.toTimerOptionResponse(savedTimerOption);
    }

    @Override
    @Transactional
    public void deleteById(Long id, User authUser) {
        log.info("Deleting timer option with id: {} for user id: {}", id, authUser.getId());

        TimerOption timerOption = timerOptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TimerOption with id " + id + " not found"));

        checkOwnership(timerOption, authUser);

        timerOptionRepository.delete(timerOption);
        log.info("Timer option with id: {} deleted successfully", id);
    }

    @Override
    public List<TimerOptionResponseDto> findAll(User authUser, Pageable pageable) {
        log.info("Fetching all timer options for user id: {}", authUser.getId());

        List<TimerOptionResponseDto> options = timerOptionRepository.findAllByUserId(authUser.getId(), pageable).stream()
                .map(timerOptionMapper::toTimerOptionResponse)
                .toList();

        log.info("Found {} timer options for user id: {}", options.size(), authUser.getId());
        return options;
    }

    private void checkOwnership(TimerOption timerOption, User authUser) {
        if (!timerOption.getUser().getId().equals(authUser.getId())) {
            log.warn("Access denied: User {} tried to access timer option {} owned by user {}",
                    authUser.getId(), timerOption.getId(), timerOption.getUser().getId());
            throw new AccessDeniedException("User with id " + authUser.getId() + " does not own timer option with id " + timerOption.getId());
        }
    }
}
