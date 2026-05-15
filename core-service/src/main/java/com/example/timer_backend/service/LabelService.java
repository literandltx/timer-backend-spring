package com.example.timer_backend.service;

import com.example.timer_backend.dto.label.CreateLabelRequestDto;
import com.example.timer_backend.dto.label.CreateLabelResponseDto;
import com.example.timer_backend.dto.label.LabelRequestDto;
import com.example.timer_backend.dto.label.LabelResponseDto;
import com.example.timer_backend.model.User;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public interface LabelService {
    CreateLabelResponseDto save(CreateLabelRequestDto request, User authUser);

    LabelResponseDto findById(Long id, User authUser);

    LabelResponseDto updateById(Long id, LabelRequestDto request, User authUser);

    void deleteById(Long id, User authUser);

    List<LabelResponseDto> findAll(User user, Pageable pageable);
}
