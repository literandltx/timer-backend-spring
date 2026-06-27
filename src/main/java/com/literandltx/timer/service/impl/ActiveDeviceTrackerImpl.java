package com.literandltx.timer.service.impl;

import static com.literandltx.timer.security.AuthenticationService.MAX_ACTIVE_DEVICES;

import com.literandltx.timer.dto.actuator.AdminDevicesResponse;
import com.literandltx.timer.dto.actuator.SystemStatus;
import com.literandltx.timer.dto.actuator.UserPingResponse;
import com.literandltx.timer.service.ActiveDeviceTracker;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ActiveDeviceTrackerImpl implements ActiveDeviceTracker {

    private static final long DEVICE_TTL_MS = 60 * 1000;

    private final Map<String, Map<UUID, Long>> userDevicesCache = new ConcurrentHashMap<>();

    @Override
    public void unregisterDevice(String username, UUID deviceUuid) {
        Map<UUID, Long> devices = userDevicesCache.get(username);
        if (devices != null) {
            devices.remove(deviceUuid);
            if (devices.isEmpty()) {
                userDevicesCache.remove(username);
            }
        }
    }

    @Override
    public AdminDevicesResponse findAllActiveDevicesSummary() {
        Map<String, Integer> summary = new ConcurrentHashMap<>();
        userDevicesCache.forEach((username, devices) -> summary.put(username, devices.size()));

        return new AdminDevicesResponse(
                SystemStatus.UP,
                summary.size(),
                summary
        );
    }

    @Override
    public UserPingResponse registerAndFindUserPing(String username, UUID deviceUuid) {
        Map<UUID, Long> userDevices = userDevicesCache.computeIfAbsent(username, k -> new ConcurrentHashMap<>());

        userDevices.put(deviceUuid, System.currentTimeMillis());

        if (userDevices.size() > MAX_ACTIVE_DEVICES) {
            UUID oldestDeviceUuid = Collections.min(userDevices.entrySet(), Map.Entry.comparingByValue()).getKey();
            userDevices.remove(oldestDeviceUuid);
            log.debug("Removed stale tracked device {} for user {} due to MAX_TRACKED_DEVICES limit", oldestDeviceUuid, username);
        }

        int activeCount = userDevices.size();
        log.info("Active devices: {}, uuid: {}", activeCount, deviceUuid);
        return new UserPingResponse(
                SystemStatus.UP,
                username,
                activeCount
        );
    }

    @Override
    public void clear() {
        userDevicesCache.clear();
    }

    /**
     * Runs every 65 seconds (65000 ms).
     * Removes devices that haven't pinged within the DEVICE_TTL_MS window.
     */
    @Scheduled(fixedRate = 65_000)
    public void cleanupStaleDevices() {
        long now = System.currentTimeMillis();

        userDevicesCache.entrySet().removeIf(entry -> {
            Map<UUID, Long> devices = entry.getValue();
            devices.entrySet().removeIf(deviceEntry -> (now - deviceEntry.getValue()) > DEVICE_TTL_MS);
            return devices.isEmpty();
        });

    }

}
