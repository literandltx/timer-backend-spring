package com.literandltx.timer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.literandltx.timer.dto.user.UserLoginRequestDto;
import com.literandltx.timer.model.TimerOption;
import com.literandltx.timer.model.TimerSetting;
import com.literandltx.timer.model.User;
import com.literandltx.timer.repository.TimerOptionRepository;
import com.literandltx.timer.repository.TimerSettingRepository;
import com.literandltx.timer.repository.UserRepository;
import io.restassured.http.ContentType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
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

        testUser = new User();
        testUser.setEmail(userEmail);
        testUser.setPassword(passwordEncoder.encode(userPlainPassword));
        userRepository.save(testUser);

        defaultOption = new TimerOption();
        defaultOption.setUuid(UUID.randomUUID());
        defaultOption.setValue(1500L);
        defaultOption.setUser(testUser);
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
        UUID settingId = UUID.randomUUID();

        Map<String, Object> request = Map.of(
                "uuid", settingId,
                "timerOptionId", defaultOption.getUuid()
        );

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(request)
                .when()
                .put("/api/settings")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("uuid", notNullValue());
    }

    @Test
    void shouldUpdateTimerSetting_WhenUpsertingAndSettingAlreadyExists() {
        TimerSetting existingSetting = new TimerSetting();
        existingSetting.setUuid(UUID.randomUUID());
        existingSetting.setUser(testUser);
        existingSetting.setPreference(defaultOption);
        existingSetting.setLastUpdated(System.currentTimeMillis());
        timerSettingRepository.save(existingSetting);

        Map<String, Object> updateRequest = Map.of(
                "uuid", existingSetting.getUuid(),
                "timerOptionId", defaultOption.getUuid()
        );

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(updateRequest)
                .when()
                .put("/api/settings")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("uuid", equalTo(existingSetting.getUuid().toString()));
    }

    @Test
    void shouldReturnTimerSetting_WhenPullingSettingsAndNoUpdatedAfterIsProvided() {
        TimerSetting existingSetting = new TimerSetting();
        existingSetting.setUuid(UUID.randomUUID());
        existingSetting.setUser(testUser);
        existingSetting.setPreference(defaultOption);
        existingSetting.setLastUpdated(System.currentTimeMillis());
        timerSettingRepository.save(existingSetting);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/settings/sync")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("uuid", equalTo(existingSetting.getUuid().toString()));
    }

    @Test
    void shouldReturnNotFound_WhenPullingSettingsAndNoneExist() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/settings/sync")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void shouldReturnTimerSetting_WhenUpdatedAfterIsProvided_AndSettingIsNewer() {
        LocalDateTime pastQueryDate = LocalDateTime.now().minusDays(5);
        LocalDateTime recentUpdate = LocalDateTime.now().minusDays(1);

        TimerSetting setting = new TimerSetting();
        setting.setUuid(UUID.randomUUID());
        setting.setUpdatedAt(recentUpdate);
        setting.setUser(testUser);
        setting.setPreference(defaultOption);
        setting.setLastUpdated(System.currentTimeMillis());
        timerSettingRepository.save(setting);

        String isoDate = pastQueryDate.format(DateTimeFormatter.ISO_DATE_TIME);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .queryParam("updatedAfter", isoDate)
                .when()
                .get("/api/settings/sync")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("uuid", equalTo(setting.getUuid().toString()));
    }

    @Test
    void shouldReturnNotFound_WhenUpdatedAfterIsProvided_AndSettingIsOlder() {
        LocalDateTime futureQueryDate = LocalDateTime.now().plusDays(5);
        LocalDateTime oldUpdate = LocalDateTime.now().minusDays(5);

        TimerSetting setting = new TimerSetting();
        setting.setUuid(UUID.randomUUID());
        setting.setUpdatedAt(oldUpdate);
        setting.setUser(testUser);
        setting.setPreference(defaultOption);
        setting.setLastUpdated(System.currentTimeMillis());
        timerSettingRepository.save(setting);

        String isoDate = futureQueryDate.format(DateTimeFormatter.ISO_DATE_TIME);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .queryParam("updatedAfter", isoDate)
                .when()
                .get("/api/settings/sync")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }
}
