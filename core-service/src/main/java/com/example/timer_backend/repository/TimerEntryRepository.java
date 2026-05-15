package com.example.timer_backend.repository;

import com.example.timer_backend.model.TimerEntry;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimerEntryRepository extends JpaRepository<TimerEntry, Long> {
    List<TimerEntry> findAllByUserId(Long userId);

    List<TimerEntry> findAllByUserId(Long userId, Pageable pageable);
}
