package com.example.timer_backend.service.impl;

import com.example.timer_backend.dto.label.CreateLabelRequestDto;
import com.example.timer_backend.dto.label.CreateLabelResponseDto;
import com.example.timer_backend.dto.label.LabelRequestDto;
import com.example.timer_backend.dto.label.LabelResponseDto;
import com.example.timer_backend.mapper.LabelMapper;
import com.example.timer_backend.model.Label;
import com.example.timer_backend.model.User;
import com.example.timer_backend.repository.LabelRepository;
import com.example.timer_backend.service.LabelService;
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
public class LabelServiceImpl implements LabelService {
    private final LabelRepository labelRepository;
    private final LabelMapper labelMapper;

    public CreateLabelResponseDto save(CreateLabelRequestDto request, User authUser) {
        log.info("Creating new label '{}' for user id: {}", request.getName(), authUser.getId());

        Label label = labelMapper.toLabel(request, authUser);
        Label saved = labelRepository.save(label);

        log.info("Label created successfully with id: {}", saved.getId());
        return labelMapper.toCreateLabelResponse(saved);
    }

    @Override
    public LabelResponseDto findById(Long id, User authUser) {
        log.info("Fetching label with id: {}", id);

        Label label = labelRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Label with id " + id + " not found")
        );

        checkOwnership(label, authUser);

        log.info("Label updated successfully with id: {}", id);
        return labelMapper.toLabelResponse(label);
    }

    @Override
    @Transactional
    public LabelResponseDto updateById(Long id, LabelRequestDto request, User authUser) {
        log.info("Updating label with id: {} for user id: {}", id, authUser.getId());

        Label existingLabel = labelRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Label with id " + id + " not found")
        );

        checkOwnership(existingLabel, authUser);

        existingLabel.setName(request.getName());
        existingLabel.setColor(request.getColor());

        Label savedLabel = labelRepository.save(existingLabel);
        return labelMapper.toLabelResponse(savedLabel);
    }

    @Override
    @Transactional
    public void deleteById(Long id, User authUser) {
        log.info("Deleting label with id: {} for user id: {}", id, authUser.getId());

        Label label = labelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Label with id " + id + " not found"));

        checkOwnership(label, authUser);

        labelRepository.delete(label);
        log.info("Label with id: {} deleted successfully", id);
    }

    public List<LabelResponseDto> findAll(User authUser, Pageable pageable) {
        log.info("Fetching all labels for user id: {}", authUser.getId());

        List<LabelResponseDto> labels = labelRepository.findAllByUserId(authUser.getId(), pageable).stream()
                .map(labelMapper::toLabelResponse)
                .toList();

        log.info("Found {} labels for user id: {}", labels.size(), authUser.getId());
        return labels;
    }

    private void checkOwnership(Label label, User authUser) {
        if (!label.getUser().getId().equals(authUser.getId())) {
            log.warn("Access denied: User {} tried to access label {} owned by user {}",
                    authUser.getId(), label.getId(), label.getUser().getId());
            throw new AccessDeniedException("You do not have permission to delete this label");
        }
    }
}
