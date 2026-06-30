package com.literandltx.timer.repository;

import com.literandltx.timer.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmailForAuth(@Param("email") String email);

    @Modifying
    @Query("UPDATE User u SET u.isEnabled = false WHERE u.id NOT IN "
            + "(SELECT u2.id FROM User u2 JOIN u2.roles r WHERE r.name = 'ADMIN')")
    void disableAllUsers();

    @Modifying
    @Query("UPDATE User u SET u.isEnabled = true WHERE u.id NOT IN "
            + "(SELECT u2.id FROM User u2 JOIN u2.roles r WHERE r.name = 'ADMIN')")
    void enableAllUsers();

}
