package com.literandltx.timer.service;

import com.literandltx.timer.dto.option.TimerOptionCreateRequestDto;
import com.literandltx.timer.dto.option.TimerOptionResponseDto;
import com.literandltx.timer.dto.option.TimerOptionUpdateRequestDto;
import com.literandltx.timer.model.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public interface TimerOptionService {
    TimerOptionResponseDto save(TimerOptionCreateRequestDto request, User authUser);

    List<TimerOptionResponseDto> findAll(LocalDateTime updatedAfter, User authUser);

    TimerOptionResponseDto update(UUID id, TimerOptionUpdateRequestDto request, User authUser);

    void delete(UUID id, User authUser);
}
