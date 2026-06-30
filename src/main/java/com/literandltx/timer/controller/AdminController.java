package com.literandltx.timer.controller;

import com.literandltx.timer.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    @PatchMapping("/users/{id}/enable")
    public ResponseEntity<Void> enableUser(@PathVariable Long id) {
        adminService.enableUser(id);
        return ResponseEntity
                .noContent()
                .build();
    }

    @PatchMapping("/users/enable")
    public ResponseEntity<Void> enableAllUsers() {
        adminService.enableAllUsers();
        return ResponseEntity
                .noContent()
                .build();
    }

    @PatchMapping("/users/{id}/disable")
    public ResponseEntity<Void> disableUser(@PathVariable Long id) {
        adminService.disableUser(id);
        return ResponseEntity
                .noContent()
                .build();
    }

    @PatchMapping("/users/disable")
    public ResponseEntity<Void> disableAllUsers() {
        adminService.disableAllUsers();
        return ResponseEntity
                .noContent()
                .build();
    }

}
