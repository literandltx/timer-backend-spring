package com.literandltx.timer.service;

import com.literandltx.timer.dto.entry.TimerEntryCreateRequestDto;
import com.literandltx.timer.dto.entry.TimerEntryResponseDto;
import com.literandltx.timer.dto.entry.TimerEntryUpdateRequestDto;
import com.literandltx.timer.model.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public interface TimerEntryService {
    TimerEntryResponseDto save(TimerEntryCreateRequestDto request, User authUser);

    List<TimerEntryResponseDto> findAll(LocalDateTime updatedAfter, User authUser);

    TimerEntryResponseDto update(UUID id, TimerEntryUpdateRequestDto request, User authUser);

    void delete(UUID id, User authUser);
}
