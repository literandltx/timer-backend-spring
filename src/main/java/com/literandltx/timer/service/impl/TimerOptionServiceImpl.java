package com.literandltx.timer.service.impl;

import static com.literandltx.timer.validation.OwnershipValidator.validateOwnership;

import com.literandltx.timer.dto.option.TimerOptionCreateRequestDto;
import com.literandltx.timer.dto.option.TimerOptionResponseDto;
import com.literandltx.timer.dto.option.TimerOptionUpdateRequestDto;
import com.literandltx.timer.mapper.TimerOptionMapper;
import com.literandltx.timer.model.TimerOption;
import com.literandltx.timer.model.User;
import com.literandltx.timer.repository.TimerOptionRepository;
import com.literandltx.timer.service.TimerOptionService;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimerOptionServiceImpl implements TimerOptionService {

    private final TimerOptionRepository timerOptionRepository;
    private final TimerOptionMapper timerOptionMapper;

    @Override
    @Transactional
    public TimerOptionResponseDto save(TimerOptionCreateRequestDto request, User authUser) {
        log.info("Creating new timer option with time: '{}' and UUID: {} for user: {}", request.value(), request.uuid(), authUser.getId());

        Optional<TimerOption> existingTimerOption = timerOptionRepository.findById(request.uuid());

        if (existingTimerOption.isPresent()) {
            log.info("Timer option already exists, skipping update.");

            TimerOption option = existingTimerOption.get();
            validateOwnership(option, authUser);

            return timerOptionMapper.toResponseDto(option);
        }

        TimerOption timerOption = timerOptionMapper.toTimerOption(request, authUser);
        TimerOption savedTimerOption = timerOptionRepository.save(timerOption);

        return timerOptionMapper.toResponseDto(savedTimerOption);
    }

    @Override
    public List<TimerOptionResponseDto> findAll(LocalDateTime updatedAfter, User authUser) {
        List<TimerOption> options;

        if (updatedAfter != null) {
            log.info("Fetching delta updates for timer options after: {}", updatedAfter);
            options = timerOptionRepository.findByUserIdAndUpdatedAtAfter(authUser.getId(), updatedAfter);
        } else {
            log.info("Fetching all active timer options");
            options = timerOptionRepository.findByUserIdAndIsDeletedFalse(authUser.getId());
        }

        return options.stream()
                .map(timerOptionMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TimerOptionResponseDto update(UUID id, TimerOptionUpdateRequestDto request, User authUser) {
        log.info("Updating timer option with id: {} for user id: {}", id, authUser.getId());

        TimerOption existingOption = timerOptionRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Timer option with id " + id + " not found")
        );

        validateOwnership(existingOption, authUser);

        timerOptionMapper.updateOptionFromDto(request, existingOption);

        if (request.updatedAt() != null) {
            existingOption.setUpdatedAt(request.updatedAt());
        } else {
            existingOption.setUpdatedAt(LocalDateTime.now());
        }

        TimerOption updatedOption = timerOptionRepository.save(existingOption);
        log.debug("Successfully updated timer option with ID: {}", updatedOption.getUuid());

        return timerOptionMapper.toResponseDto(updatedOption);
    }

    @Override
    @Transactional
    public void delete(UUID id, User authUser) {
        log.info("Soft deleting timer option with id: {} for user id: {}", id, authUser.getId());

        TimerOption option = timerOptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Timer option with id " + id + " not found"));

        validateOwnership(option, authUser);

        option.setDeleted(true);
        option.setUpdatedAt(LocalDateTime.now());
        timerOptionRepository.save(option);

        log.info("Timer option with id: {} deleted successfully", id);
    }

}
