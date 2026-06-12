package com.literandltx.timer.service.impl;

import com.literandltx.timer.dto.user.UserRegistrationRequestDto;
import com.literandltx.timer.dto.user.UserRegistrationResponseDto;
import com.literandltx.timer.exception.custom.UserAlreadyExistsException;
import com.literandltx.timer.mapper.UserMapper;
import com.literandltx.timer.model.Role;
import com.literandltx.timer.model.RoleName;
import com.literandltx.timer.model.User;
import com.literandltx.timer.repository.RoleRepository;
import com.literandltx.timer.repository.UserRepository;
import com.literandltx.timer.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserRegistrationResponseDto register(UserRegistrationRequestDto request) {
        log.info("Attempting to register new user with email: {}", request.getEmail());

        checkEmailAvailability(request.getEmail());

        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new EntityNotFoundException("Role '" + RoleName.USER + "' not found in the database."));

        User user = userMapper.toEntity(
                request,
                passwordEncoder.encode(request.getPassword()),
                Set.of(userRole)
        );

        User savedUser = userRepository.save(user);
        log.info("User registered with id: {} and email: {}", savedUser.getId(), savedUser.getEmail());

        return userMapper.toModel(savedUser);
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("User with id " + id + " not found")
        );
    }

    private void checkEmailAvailability(String email) {
        if (userRepository.existsByEmail(email)) {
            log.warn("Operation failed. The email '{}' is already in use.", email);
            throw new UserAlreadyExistsException("The email address '" + email + "' is already in use.");
        }
    }
}
