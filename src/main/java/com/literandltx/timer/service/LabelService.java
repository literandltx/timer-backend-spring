package com.literandltx.timer.service;

import com.literandltx.timer.dto.label.LabelCreateRequestDto;
import com.literandltx.timer.dto.label.LabelResponseDto;
import com.literandltx.timer.dto.label.LabelUpdateRequestDto;
import com.literandltx.timer.model.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public interface LabelService {
    LabelResponseDto save(LabelCreateRequestDto request, User authUser);

    List<LabelResponseDto> findAll(LocalDateTime updatedAfter, User authUser);

    LabelResponseDto update(UUID id, LabelUpdateRequestDto request, User authUser);

    void delete(UUID id, User authUser);
}
