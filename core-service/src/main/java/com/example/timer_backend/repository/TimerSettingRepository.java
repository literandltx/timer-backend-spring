package com.example.timer_backend.repository;

import com.example.timer_backend.model.TimerSetting;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimerSettingRepository extends JpaRepository<TimerSetting, Long> {
    List<TimerSetting> findAllByUserId(Long userId);

    List<TimerSetting> findAllByUserId(Long userId, Pageable pageable);

    boolean existsByUserId(Long userId);
}
