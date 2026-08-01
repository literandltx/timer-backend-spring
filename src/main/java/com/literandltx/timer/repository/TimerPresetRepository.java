package com.literandltx.timer.repository;

import com.literandltx.timer.model.TimerPreset;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimerPresetRepository extends JpaRepository<TimerPreset, UUID> {

    Optional<TimerPreset> findByUserId(Long userId);

    Optional<TimerPreset> findByUserIdAndServerUpdatedAtAfter(Long userId, LocalDateTime updatedAfter);
}
