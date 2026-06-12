package com.literandltx.timer.repository;

import com.literandltx.timer.model.TimerEntry;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimerEntryRepository extends JpaRepository<TimerEntry, UUID> {

    List<TimerEntry> findByUserIdAndUpdatedAtAfter(Long userId, LocalDateTime updatedAfter);

    List<TimerEntry> findByUserIdAndIsDeletedFalse(Long userId);
}
