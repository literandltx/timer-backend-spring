package com.literandltx.timer.service;

import org.springframework.stereotype.Service;

@Service
public interface AdminService {

    void enableUser(Long userId);

    void enableAllUsers();

    void disableUser(Long userId);

    void disableAllUsers();

}
