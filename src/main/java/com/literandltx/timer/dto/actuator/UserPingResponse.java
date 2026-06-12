package com.literandltx.timer.dto.actuator;

public record UserPingResponse(
        SystemStatus status,
        String user,
        int activeDevices
) {
}
