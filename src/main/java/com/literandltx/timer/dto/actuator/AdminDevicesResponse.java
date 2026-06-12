package com.literandltx.timer.dto.actuator;

import java.util.Map;

public record AdminDevicesResponse(
        SystemStatus status,
        int totalActiveUsers,
        Map<String, Integer> deviceSummaryPerUser
) {
}
