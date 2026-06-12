package com.literandltx.timer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.literandltx.timer.dto.settings.TimerSettingRequestDto;
import com.literandltx.timer.dto.user.UserLoginRequestDto;
import com.literandltx.timer.model.TimerOption;
import com.literandltx.timer.model.TimerSetting;
import com.literandltx.timer.model.User;
import com.literandltx.timer.repository.TimerOptionRepository;
import com.literandltx.timer.repository.TimerSettingRepository;
import com.literandltx.timer.repository.UserRepository;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

public class TimerSettingControllerIT extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TimerSettingRepository timerSettingRepository;

    @Autowired
    private TimerOptionRepository timerOptionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String userEmail = "settings_test@example.com";
    private final String userPlainPassword = "password";
    private String authToken;
    private User testUser;
    private TimerOption defaultOption;

    @BeforeEach
    void setUpUser() {
        super.setUp();

        testUser = User.builder()
                .email(userEmail)
                .password(passwordEncoder.encode(userPlainPassword))
                .build();
        userRepository.save(testUser);

        defaultOption = TimerOption.builder()
                .uuid(UUID.randomUUID())
                .value(1500L)
                .user(testUser)
                .createdAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .updatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .isDeleted(false)
                .build();
        defaultOption = timerOptionRepository.save(defaultOption);

        UserLoginRequestDto loginRequest = new UserLoginRequestDto();
        loginRequest.setUsername(userEmail);
        loginRequest.setPassword(userPlainPassword);

        authToken = given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
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
        jdbcTemplate.execute("DELETE FROM timer_settings");
        jdbcTemplate.execute("DELETE FROM timer_options");
        jdbcTemplate.execute("DELETE FROM users_roles");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    void shouldCreateTimerSetting_WhenUpsertingAndNoneExists() {
        // 1. Arrange
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        UUID settingId = UUID.randomUUID();

        TimerSettingRequestDto request = new TimerSettingRequestDto(
                settingId,
                defaultOption.getUuid(),
                now,
                now
        );

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(request)
                .when()
                .put("/api/settings");

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("uuid", notNullValue());
    }

    @Test
    void shouldUpdateTimerSetting_WhenUpsertingAndSettingAlreadyExists() {
        // 1. Arrange
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime future = now.plusHours(1);

        TimerSetting existingSetting = TimerSetting.builder()
                .uuid(UUID.randomUUID())
                .user(testUser)
                .preference(defaultOption)
                .lastUpdated(System.currentTimeMillis())
                .updatedAt(now)
                .build();
        timerSettingRepository.save(existingSetting);

        TimerSettingRequestDto updateRequest = new TimerSettingRequestDto(
                existingSetting.getUuid(),
                defaultOption.getUuid(),
                now,
                future
        );

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(updateRequest)
                .when()
                .put("/api/settings");

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("uuid", equalTo(existingSetting.getUuid().toString()));
    }

    @Test
    void shouldReturnTimerSetting_WhenPullingSettingsAndNoUpdatedAfterIsProvided() {
        // 1. Arrange
        TimerSetting existingSetting = TimerSetting.builder()
                .uuid(UUID.randomUUID())
                .user(testUser)
                .preference(defaultOption)
                .lastUpdated(System.currentTimeMillis())
                .updatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .build();
        timerSettingRepository.save(existingSetting);

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/settings/sync");

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("uuid", equalTo(existingSetting.getUuid().toString()));
    }

    @Test
    void shouldReturnNotFound_WhenPullingSettingsAndNoneExist() {
        // 1. Arrange

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/settings/sync");

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void shouldReturnTimerSetting_WhenUpdatedAfterIsProvided_AndSettingIsNewer() {
        // 1. Arrange
        LocalDateTime pastQueryDate = LocalDateTime.now().minusDays(5).truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime recentUpdate = LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.SECONDS);

        TimerSetting setting = TimerSetting.builder()
                .uuid(UUID.randomUUID())
                .updatedAt(recentUpdate)
                .user(testUser)
                .preference(defaultOption)
                .lastUpdated(System.currentTimeMillis())
                .build();
        timerSettingRepository.save(setting);

        String isoDate = pastQueryDate.format(DateTimeFormatter.ISO_DATE_TIME);

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .queryParam("updatedAfter", isoDate)
                .when()
                .get("/api/settings/sync");

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("uuid", equalTo(setting.getUuid().toString()));
    }

    @Test
    void shouldReturnNotFound_WhenUpdatedAfterIsProvided_AndSettingIsOlder() {
        // 1. Arrange
        LocalDateTime futureQueryDate = LocalDateTime.now().plusDays(5).truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime oldUpdate = LocalDateTime.now().minusDays(5).truncatedTo(ChronoUnit.SECONDS);

        TimerSetting setting = TimerSetting.builder()
                .uuid(UUID.randomUUID())
                .updatedAt(oldUpdate)
                .user(testUser)
                .preference(defaultOption)
                .lastUpdated(System.currentTimeMillis())
                .build();
        timerSettingRepository.save(setting);

        String isoDate = futureQueryDate.format(DateTimeFormatter.ISO_DATE_TIME);

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .queryParam("updatedAfter", isoDate)
                .when()
                .get("/api/settings/sync");

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NOT_MODIFIED.value());
    }
}
