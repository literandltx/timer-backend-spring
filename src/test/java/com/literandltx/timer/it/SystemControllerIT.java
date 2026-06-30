package com.literandltx.timer.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.literandltx.timer.dto.actuator.SystemStatus;
import com.literandltx.timer.dto.user.UserLoginRequestDto;
import com.literandltx.timer.model.Role;
import com.literandltx.timer.model.RoleName;
import com.literandltx.timer.model.User;
import com.literandltx.timer.repository.RoleRepository;
import com.literandltx.timer.repository.UserRepository;
import com.literandltx.timer.service.ActiveDeviceTracker;
import com.literandltx.timer.service.impl.ActiveDeviceTrackerImpl;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

public class SystemControllerIT extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ActiveDeviceTracker activeDeviceTracker;

    private final String password = "password";

    private final String userEmail1 = "testuser1@example.com";
    private final String userEmail2 = "testuser2@example.com";
    private String authToken1;
    private String authToken2;
    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUpUser() {
        super.setUp();

        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException("USER role not found in test DB"));

        testUser1 = User.builder()
                .email(userEmail1)
                .password(passwordEncoder.encode(password))
                .roles(Set.of(userRole))
                .build();
        userRepository.save(testUser1);

        testUser2 = User.builder()
                .email(userEmail2)
                .password(passwordEncoder.encode(password))
                .roles(Set.of(userRole))
                .build();
        userRepository.save(testUser2);

        UserLoginRequestDto loginRequest1 = new UserLoginRequestDto();
        loginRequest1.setUsername(userEmail1);
        loginRequest1.setPassword(password);

        UserLoginRequestDto loginRequest2 = new UserLoginRequestDto();
        loginRequest2.setUsername(userEmail2);
        loginRequest2.setPassword(password);

        authToken1 = given()
                .contentType(ContentType.JSON)
                .body(loginRequest1)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .path("token");

        authToken2 = given()
                .contentType(ContentType.JSON)
                .body(loginRequest2)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .path("token");
    }

    @AfterEach
    void tearDown() {
        super.tearDown();

        // Clear the in-memory cache so device counts don't bleed between tests
        activeDeviceTracker.clear();

        jdbcTemplate.execute("DELETE FROM users_roles");
        jdbcTemplate.execute("DELETE FROM refresh_tokens");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    void shouldReturnSystemStatusUp_WhenUserPing() {
        // 1. Arrange
        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken1)
                .when()
                .get("/api/v1/system/ping/public");

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void shouldReturnSystemStatus_WhenUserIsAuthenticated() {
        // 1. Arrange
        UUID testDeviceUuid = UUID.randomUUID();

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken1)
                .queryParam("deviceUuid", testDeviceUuid)
                .when()
                .post("/api/v1/system/ping/user");

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("status", equalTo(SystemStatus.UP.name()))
                .body("user", equalTo(userEmail1))
                .body("activeDevices", notNullValue());
    }

    @Test
    void shouldIncrementActiveDevices_WhenSameUserPingsWithDifferentDevices() {
        // 1. Arrange
        UUID device1 = UUID.randomUUID();
        UUID device2 = UUID.randomUUID();

        // 2. Act & Assert - First Device
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken1)
                .queryParam("deviceUuid", device1)
                .when()
                .post("/api/v1/system/ping/user")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("activeDevices", equalTo(1));

        // 3. Act & Assert - Second Device
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken1)
                .queryParam("deviceUuid", device2)
                .when()
                .post("/api/v1/system/ping/user")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("activeDevices", equalTo(2));
    }

    @Test
    void shouldNotIncrementActiveDevices_WhenSameUserPingsWithSameDevice() {
        // 1. Arrange
        UUID device = UUID.randomUUID();

        // 2. Act - First Ping
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken1)
                .queryParam("deviceUuid", device)
                .when()
                .post("/api/v1/system/ping/user");

        // 3. Act & Assert - Second Ping (Should still be 1)
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken1)
                .queryParam("deviceUuid", device)
                .when()
                .post("/api/v1/system/ping/user")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("activeDevices", equalTo(1));
    }

    @Test
    void shouldTrackDevicesSeparately_WhenDifferentUsersPing() {
        // 1. Arrange
        UUID user1Device = UUID.randomUUID();
        UUID user2Device = UUID.randomUUID();

        // 2. Act & Assert - User 1
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken1)
                .queryParam("deviceUuid", user1Device)
                .when()
                .post("/api/v1/system/ping/user")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("user", equalTo(userEmail1))
                .body("activeDevices", equalTo(1));

        // 3. Act & Assert - User 2
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken2)
                .queryParam("deviceUuid", user2Device)
                .when()
                .post("/api/v1/system/ping/user")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("user", equalTo(userEmail2))
                .body("activeDevices", equalTo(1));
    }

    @Test
    void shouldRemoveDevice_WhenTtlExpires() {
        // 1. Arrange
        UUID deviceUuid = UUID.randomUUID();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken1)
                .queryParam("deviceUuid", deviceUuid)
                .when()
                .post("/api/v1/system/ping/user")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("activeDevices", equalTo(1));

        @SuppressWarnings("unchecked")
        Map<String, Map<UUID, Long>> cache = (Map<String, Map<UUID, Long>>)
                ReflectionTestUtils.getField(activeDeviceTracker, "userDevicesCache");

        cache.get(userEmail1).put(deviceUuid, System.currentTimeMillis() - 61_000);

        // 2. Act
        ((ActiveDeviceTrackerImpl) activeDeviceTracker).cleanupStaleDevices();

        // 3. Assert
        UUID newDeviceUuid = UUID.randomUUID();
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken1)
                .queryParam("deviceUuid", newDeviceUuid)
                .when()
                .post("/api/v1/system/ping/user")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("activeDevices", equalTo(1)); // Asserts the previous device was purged
    }
}
