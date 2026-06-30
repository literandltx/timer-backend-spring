package com.literandltx.timer.service.impl;

import com.literandltx.timer.model.User;
import com.literandltx.timer.repository.UserRepository;
import com.literandltx.timer.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;

    @Transactional
    public void disableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        user.setEnabled(false);
        userRepository.save(user);
    }

    @Transactional
    public void disableAllUsers() {
        userRepository.disableAllUsers();
    }

    @Transactional
    public void enableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        user.setEnabled(true);
        userRepository.save(user);
    }

    @Transactional
    public void enableAllUsers() {
        userRepository.enableAllUsers();
    }
}
