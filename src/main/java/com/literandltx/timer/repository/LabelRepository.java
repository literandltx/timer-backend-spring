package com.literandltx.timer.repository;

import com.literandltx.timer.model.Label;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LabelRepository extends JpaRepository<Label, UUID> {
    List<Label> findByUserIdAndIsDeletedFalse(Long userId);

    List<Label> findByUserIdAndUpdatedAtAfter(Long userId, LocalDateTime updatedAfter);
}
