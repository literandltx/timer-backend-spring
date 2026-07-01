package com.literandltx.timer.config;

import com.literandltx.timer.config.env.AppProperties;
import com.literandltx.timer.model.Role;
import com.literandltx.timer.model.RoleName;
import com.literandltx.timer.model.User;
import com.literandltx.timer.repository.RoleRepository;
import com.literandltx.timer.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting data initialization...");

        initializeAdminUser();

        log.info("Data initialization completed.");
    }

    private void initializeRoles() {
        log.info("Starting role data initialization...");

        for (RoleName roleName : RoleName.values()) {
            roleRepository.findByName(roleName)
                    .orElseGet(() -> {
                        log.info("Role '{}' not found.", roleName);
                        Role newRole = roleRepository.save(new Role().setName(roleName));
                        log.info("Created new role: '{}'", roleName);
                        return newRole;
                    });
        }
    }

    private void initializeAdminUser() {
        String adminEmail = properties.init().adminEmail();

        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Admin user '{}' already exists. Skipping creation.", adminEmail);
            return;
        }

        log.info("Admin user '{}' not found. Creating default admin account...", adminEmail);

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role should have been initialized."));

        User admin = User.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode(properties.init().defaultAdminPassword()))
                .roles(Set.of(adminRole))
                .isDeleted(false)
                .build();

        userRepository.save(admin);
        log.info("Created default admin user: '{}'", adminEmail);
    }

}
