package com.literandltx.timer.repository;

import com.literandltx.timer.model.TimerOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimerOptionRepository extends JpaRepository<TimerOption, UUID> {
    List<TimerOption> findByUserIdAndIsDeletedFalse(Long userId);

    List<TimerOption> findByUserIdAndUpdatedAtAfter(Long userId, LocalDateTime updatedAfter);
}
