package com.example.timer_backend.repository;

import com.example.timer_backend.model.TimerOption;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimerOptionRepository extends JpaRepository<TimerOption, Long> {
    List<TimerOption> findAllByUserId(Long userId);

    List<TimerOption> findAllByUserId(Long userId, Pageable pageable);
}
