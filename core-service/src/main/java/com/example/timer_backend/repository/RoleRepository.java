package com.example.timer_backend.repository;

import com.example.timer_backend.model.Role;
import com.example.timer_backend.model.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
