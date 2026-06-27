package com.literandltx.timer.repository;

import com.literandltx.timer.model.RefreshToken;
import com.literandltx.timer.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    void deleteByUser(User user);

    List<RefreshToken> findAllByUserOrderByIdAsc(User user);
}
