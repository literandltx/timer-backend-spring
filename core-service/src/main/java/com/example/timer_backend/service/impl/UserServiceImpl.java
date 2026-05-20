package com.example.timer_backend.service.impl;

import com.example.timer_backend.dto.user.ChangeEmailRequestDto;
import com.example.timer_backend.dto.user.ChangePasswordRequestDto;
import com.example.timer_backend.dto.user.UserRegistrationRequestDto;
import com.example.timer_backend.dto.user.UserRegistrationResponseDto;
import com.example.timer_backend.dto.user.UserResponseDto;
import com.example.timer_backend.dto.user.UserUpdateRequestDto;
import com.example.timer_backend.event.NotificationRequestedEvent;
import com.example.timer_backend.exception.custom.UserAlreadyExistsException;
import com.example.timer_backend.mapper.UserMapper;
import com.example.timer_backend.model.Role;
import com.example.timer_backend.model.RoleName;
import com.example.timer_backend.model.User;
import com.example.timer_backend.producer.NotificationEventPublisher;
import com.example.timer_backend.repository.RoleRepository;
import com.example.timer_backend.repository.UserRepository;
import com.example.timer_backend.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
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
    private final NotificationEventPublisher notificationEventPublisher;

    @Override
    @Transactional
    public UserRegistrationResponseDto register(UserRegistrationRequestDto request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Unable to complete registration. User already exists.");
        }

        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new RuntimeException("Error: Role not found."));

        User user = userMapper.toEntity(request, passwordEncoder.encode(request.getPassword()), Set.of(userRole));
        User saved = userRepository.save(user);

        NotificationRequestedEvent event = new NotificationRequestedEvent();
        event.setUserId(saved.getId());
        event.setEmail(request.getEmail());
        event.setNotificationType("REGISTER");
        notificationEventPublisher.publishNotificationRequest(event);

        return userMapper.toModel(saved);
    }

    @Override
    public UserResponseDto getCurrentUser(User authUser) {
        log.info("Fetching current user details for user id: {}", authUser.getId());

        User user = getUserOrThrow(authUser.getId());

        return userMapper.toResponseDto(user);
    }

    @Override
    @Transactional
    public UserResponseDto updateAccount(User authUser, UserUpdateRequestDto request) {
        log.info("Updating account details for user id: {}", authUser.getId());

        User user = getUserOrThrow(authUser.getId());

        if (!user.getEmail().equals(request.getEmail())) {
            checkEmailAvailability(request.getEmail());
            user.setEmail(request.getEmail());
        }

        User savedUser = userRepository.save(user);

        log.info("Account details updated successfully for user id: {}", savedUser.getId());
        return userMapper.toResponseDto(savedUser);
    }

    @Override
    @Transactional
    public void changePassword(User authUser, ChangePasswordRequestDto request) {
        log.info("Processing password change for user id: {}", authUser.getId());

        User user = getUserOrThrow(authUser.getId());

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Password change failed. Invalid current password for user id: {}", user.getId());
            throw new BadCredentialsException("Current password does not match.");
        }

        if (!request.getNewPassword().equals(request.getConfirmationPassword())) {
            log.warn("Password change failed. Password confirmation mismatch for user id: {}", user.getId());
            throw new IllegalArgumentException("New password and confirmation password do not match.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user id: {}", user.getId());
    }

    @Override
    @Transactional
    public UserResponseDto changeEmail(User authUser, ChangeEmailRequestDto request) {
        log.info("Processing email change for user id: {}", authUser.getId());

        User user = getUserOrThrow(authUser.getId());

        if (request.getNewEmail().equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("New email must be different from the current email.");
        }

        checkEmailAvailability(request.getNewEmail());

        user.setEmail(request.getNewEmail());
        User savedUser = userRepository.save(user);

        log.info("Email changed successfully to '{}' for user id: {}", savedUser.getEmail(), savedUser.getId());
        return userMapper.toResponseDto(savedUser);
    }

    @Override
    @Transactional
    public void deleteAccount(User authUser) {
        log.info("Deleting account for user id: {}", authUser.getId());

        User user = getUserOrThrow(authUser.getId());

        userRepository.delete(user);

        log.info("Account deleted successfully for user id: {}", authUser.getId());
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("User with id " + id + " not found")
        );
    }

    private void checkEmailAvailability(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            log.warn("Email change failed. Email '{}' is already in use.", email);
            throw new UserAlreadyExistsException("The email address '" + email + "' is already in use.");
        }
    }
}
