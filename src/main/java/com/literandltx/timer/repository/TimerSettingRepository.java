package com.literandltx.timer.repository;

import com.literandltx.timer.model.TimerSetting;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimerSettingRepository extends JpaRepository<TimerSetting, UUID> {
    Optional<TimerSetting> findByUuidAndUserId(UUID uuid, Long userId);

    Optional<TimerSetting> findByUserId(Long userId);

    Optional<TimerSetting> findByUserIdAndUpdatedAtAfter(Long userId, LocalDateTime updatedAfter);
}
