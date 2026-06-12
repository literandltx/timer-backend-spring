package com.literandltx.timer.service.impl;

import com.literandltx.timer.dto.label.LabelCreateRequestDto;
import com.literandltx.timer.dto.label.LabelResponseDto;
import com.literandltx.timer.dto.label.LabelUpdateRequestDto;
import com.literandltx.timer.mapper.LabelMapper;
import com.literandltx.timer.model.Label;
import com.literandltx.timer.model.User;
import com.literandltx.timer.repository.LabelRepository;
import com.literandltx.timer.service.LabelService;
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
public class LabelServiceImpl implements LabelService {

    private final LabelRepository labelRepository;
    private final LabelMapper labelMapper;

    @Override
    @Transactional
    public LabelResponseDto save(LabelCreateRequestDto request, User authUser) {
        log.info("Creating new label with name: '{}' and UUID: {} for user: {}", request.name(), request.uuid(), authUser.getId());

        Optional<Label> existingLabel = labelRepository.findById(request.uuid());

        if (existingLabel.isPresent()) {
            log.info("Label already exists, skipping update.");

            Label label = existingLabel.get();
            validateLabelOwnership(label, authUser);

            return labelMapper.toResponseDto(label);
        }

        Label newLabel = labelMapper.toLabel(request, authUser);
        Label savedLabel = labelRepository.save(newLabel);

        return labelMapper.toResponseDto(savedLabel);
    }

    @Override
    public List<LabelResponseDto> findAll(LocalDateTime updatedAfter, User authUser) {
        List<Label> labels;

        if (updatedAfter != null) {
            log.info("Fetching delta updates after: {}", updatedAfter);
            labels = labelRepository.findByUserIdAndUpdatedAtAfter(authUser.getId(), updatedAfter);
        } else {
            log.debug("Fetching all active labels from the database");
            labels = labelRepository.findByUserIdAndIsDeletedFalse(authUser.getId());
        }

        return labels.stream()
                .map(labelMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LabelResponseDto update(UUID id, LabelUpdateRequestDto request, User authUser) {
        log.info("Updating label with id: {} for user id: {}", id, authUser.getId());

        Label existingLabel = labelRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Label with id " + id + " not found")
        );

        validateLabelOwnership(existingLabel, authUser);

        existingLabel.setName(request.name());
        existingLabel.setColor(request.color());

        if (request.updatedAt() != null) {
            existingLabel.setUpdatedAt(request.updatedAt());
        } else {
            existingLabel.setUpdatedAt(LocalDateTime.now());
        }

        Label updatedLabel = labelRepository.save(existingLabel);
        log.debug("Successfully updated label with ID: {}", updatedLabel.getUuid());

        return labelMapper.toResponseDto(updatedLabel);
    }

    @Override
    @Transactional
    public void delete(UUID id, User authUser) {
        log.info("Soft deleting label with id: {} for user id: {}", id, authUser.getId());

        Label label = labelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Label with id " + id + " not found"));

        validateLabelOwnership(label, authUser);

        label.setDeleted(true);
        label.setUpdatedAt(LocalDateTime.now());
        labelRepository.save(label);
        log.info("Label with id: {} deleted successfully", id);
    }

    private void validateLabelOwnership(Label label, User authUser) {
        if (!label.getUser().getId().equals(authUser.getId())) {
            log.warn("Access denied: User {} tried to access label {} owned by user {}",
                    authUser.getId(), label.getUuid(), label.getUser().getId());
            throw new AccessDeniedException("You do not have permission to access this label");
        }
    }
}
