package com.example.timer_backend.repository;

import com.example.timer_backend.model.Label;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LabelRepository extends JpaRepository<Label, Long> {
    List<Label> findAllByUserId(Long userId);

    List<Label> findAllByUserId(Long userId, Pageable pageable);
}
