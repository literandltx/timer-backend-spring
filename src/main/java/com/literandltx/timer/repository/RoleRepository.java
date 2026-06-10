package com.literandltx.timer.repository;

import com.literandltx.timer.model.Role;
import com.literandltx.timer.model.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
