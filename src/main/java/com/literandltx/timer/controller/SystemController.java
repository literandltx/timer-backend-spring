package com.literandltx.timer.controller;

import com.literandltx.timer.dto.actuator.AdminDevicesResponse;
import com.literandltx.timer.dto.actuator.PublicPingResponse;
import com.literandltx.timer.dto.actuator.SystemStatus;
import com.literandltx.timer.dto.actuator.UserPingResponse;
import com.literandltx.timer.model.User;
import com.literandltx.timer.service.ActiveDeviceTracker;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemController {

    private final ActiveDeviceTracker deviceTracker;

    @GetMapping("/ping/public")
    public ResponseEntity<PublicPingResponse> publicPing() {
        return ResponseEntity.ok(new PublicPingResponse(SystemStatus.UP));
    }

    @GetMapping("/admin/devices")
    public ResponseEntity<AdminDevicesResponse> findAllTrackedDevices() {
        return ResponseEntity.ok(deviceTracker.findAllActiveDevicesSummary());
    }

    @PostMapping("/ping/user")
    public ResponseEntity<UserPingResponse> userPing(
            @AuthenticationPrincipal User user,
            @RequestParam UUID deviceUuid
    ) {
        return ResponseEntity.ok(deviceTracker.registerAndFindUserPing(user.getUsername(), deviceUuid));
    }

}
