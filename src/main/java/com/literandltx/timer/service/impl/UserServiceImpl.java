package com.literandltx.timer.service.impl;

import com.literandltx.timer.dto.user.UserChangeEmailRequestDto;
import com.literandltx.timer.dto.user.UserChangePasswordRequestDto;
import com.literandltx.timer.dto.user.register.UserRegistrationRequestDto;
import com.literandltx.timer.dto.user.register.UserRegistrationResponseDto;
import com.literandltx.timer.exception.custom.UserAlreadyExistsException;
import com.literandltx.timer.mapper.UserMapper;
import com.literandltx.timer.model.Role;
import com.literandltx.timer.model.RoleName;
import com.literandltx.timer.model.User;
import com.literandltx.timer.repository.RoleRepository;
import com.literandltx.timer.repository.TimerPresetRepository;
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
    private final TimerPresetRepository timerPresetRepository;
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

    @Override
    public void changeEmail(UserChangeEmailRequestDto request, User user) {
        log.info("Attempting to change user's with email: {}", user.getEmail());

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password.");
        }
        user.setEmail(request.getNewEmail());

        userRepository.save(user);
        log.info("Successfully updated user with id: {}", user.getId());
    }

    @Override
    public void changePassword(UserChangePasswordRequestDto request, User user) {
        log.info("Attempting to change user's email with email: {}", user.getEmail());

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password.");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);
        log.info("Successfully updated user with id: {}", user.getId());
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        log.info("Attempting to permanently delete user and all data for id: {}", id);

        User user = getUserOrThrow(id);

        if (user.getTimerPreset() != null) {
            timerPresetRepository.delete(user.getTimerPreset());
            user.setTimerPreset(null);
            userRepository.flush();
        }

        userRepository.delete(user);

        log.info("Successfully deleted user with id: {}", id);
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
