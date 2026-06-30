package com.literandltx.timer.service;

import com.literandltx.timer.dto.actuator.AdminDevicesResponse;
import com.literandltx.timer.dto.actuator.UserPingResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public interface ActiveDeviceTracker {

    /**
     * Manually unregisters a device for a specific user.
     * Useful for explicit logout actions or token revocations.
     *
     * @param username   the username of the device owner
     * @param deviceUuid the unique identifier of the device to remove
     */
    void unregisterDevice(String username, UUID deviceUuid);

    /**
     * Retrieves a summary of all active users and their active device counts.
     *
     * @return AdminDevicesResponse containing system status, total user count,
     * and a map of users to their active device counts
     */
    AdminDevicesResponse findAllActiveDevicesSummary();

    /**
     * Registers or updates the last seen timestamp for a user's device,
     * and returns the current ping status.
     *
     * @param username   the username of the device owner
     * @param deviceUuid the unique identifier of the device
     * @return UserPingResponse containing system status, the username,
     * and their current active device count
     */
    UserPingResponse registerAndFindUserPing(String username, UUID deviceUuid);

    /**
     * Clears all active devices from the cache entirely.
     */
    void clear();

}
