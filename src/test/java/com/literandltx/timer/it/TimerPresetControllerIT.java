package com.literandltx.timer.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.literandltx.timer.dto.preset.TimerPresetRequestDto;
import com.literandltx.timer.dto.user.UserLoginRequestDto;
import com.literandltx.timer.model.Label;
import com.literandltx.timer.model.Role;
import com.literandltx.timer.model.RoleName;
import com.literandltx.timer.model.TimerOption;
import com.literandltx.timer.model.TimerPreset;
import com.literandltx.timer.model.User;
import com.literandltx.timer.repository.LabelRepository;
import com.literandltx.timer.repository.RoleRepository;
import com.literandltx.timer.repository.TimerOptionRepository;
import com.literandltx.timer.repository.TimerPresetRepository;
import com.literandltx.timer.repository.UserRepository;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

public class TimerPresetControllerIT extends BaseIntegrationTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TimerPresetRepository timerPresetRepository;

    @Autowired
    private TimerOptionRepository timerOptionRepository;

    @Autowired
    private LabelRepository labelRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String userEmail = "preset_test@example.com";
    private final String userPlainPassword = "password";
    private String authToken;
    private User testUser;
    private TimerOption defaultOption;
    private Label defaultLabel;

    @BeforeEach
    void setUpUser() {
        super.setUp();

        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException("USER role not found in test DB"));

        testUser = User.builder()
                .email(userEmail)
                .password(passwordEncoder.encode(userPlainPassword))
                .roles(Set.of(userRole))
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

        defaultLabel = Label.builder()
                .uuid(UUID.randomUUID())
                .name("Test Label")
                .color("#FFFFFF")
                .user(testUser)
                .createdAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .updatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .isDeleted(false)
                .build();
        defaultLabel = labelRepository.save(defaultLabel);

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
        jdbcTemplate.execute("DELETE FROM timer_presets");
        jdbcTemplate.execute("DELETE FROM timer_options");
        jdbcTemplate.execute("DELETE FROM labels");
        jdbcTemplate.execute("DELETE FROM users_roles");
        jdbcTemplate.execute("DELETE FROM refresh_tokens");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    void shouldCreateTimerPreset_WhenUpsertingAndNoneExists() {
        // 1. Arrange
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        UUID presetId = UUID.randomUUID();

        TimerPresetRequestDto request = new TimerPresetRequestDto(
                presetId,
                defaultLabel.getUuid(),
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
                .put("/api/v1/timer-presets");

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("uuid", notNullValue());
    }

    @Test
    void shouldUpdateTimerPreset_WhenUpsertingAndPresetAlreadyExists() {
        // 1. Arrange
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime future = now.plusHours(1);

        TimerPreset existingPreset = TimerPreset.builder()
                .uuid(UUID.randomUUID())
                .user(testUser)
                .timerOption(defaultOption)
                .label(defaultLabel)
                .lastUpdated(System.currentTimeMillis())
                .updatedAt(now)
                .build();
        timerPresetRepository.save(existingPreset);

        TimerPresetRequestDto updateRequest = new TimerPresetRequestDto(
                existingPreset.getUuid(),
                defaultLabel.getUuid(),
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
                .put("/api/v1/timer-presets");

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("uuid", equalTo(existingPreset.getUuid().toString()));
    }

    @Test
    void shouldReturnTimerPreset_WhenPullingPresetsAndNoUpdatedAfterIsProvided() {
        // 1. Arrange
        TimerPreset existingPreset = TimerPreset.builder()
                .uuid(UUID.randomUUID())
                .user(testUser)
                .timerOption(defaultOption)
                .label(defaultLabel)
                .lastUpdated(System.currentTimeMillis())
                .updatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .build();
        timerPresetRepository.save(existingPreset);

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/v1/timer-presets/sync");

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("uuid", equalTo(existingPreset.getUuid().toString()));
    }

    @Test
    void shouldReturnNotFound_WhenPullingPresetsAndNoneExist() {
        // 1. Arrange

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/v1/timer-presets/sync");

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void shouldReturnTimerPreset_WhenUpdatedAfterIsProvided_AndPresetIsNewer() {
        // 1. Arrange
        LocalDateTime pastQueryDate = LocalDateTime.now().minusDays(5).truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime recentUpdate = LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.SECONDS);

        TimerPreset preset = TimerPreset.builder()
                .uuid(UUID.randomUUID())
                .updatedAt(recentUpdate)
                .user(testUser)
                .timerOption(defaultOption)
                .label(defaultLabel)
                .lastUpdated(System.currentTimeMillis())
                .build();
        timerPresetRepository.save(preset);

        String isoDate = pastQueryDate.format(FORMATTER);

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .queryParam("updatedAfter", isoDate)
                .when()
                .get("/api/v1/timer-presets/sync");

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("uuid", equalTo(preset.getUuid().toString()));
    }

    @Test
    void shouldReturnNotFound_WhenUpdatedAfterIsProvided_AndPresetIsOlder() {
        // 1. Arrange
        LocalDateTime futureQueryDate = LocalDateTime.now().plusDays(5).truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime oldUpdate = LocalDateTime.now().minusDays(5).truncatedTo(ChronoUnit.SECONDS);

        TimerPreset preset = TimerPreset.builder()
                .uuid(UUID.randomUUID())
                .updatedAt(oldUpdate)
                .user(testUser)
                .timerOption(defaultOption)
                .label(defaultLabel)
                .lastUpdated(System.currentTimeMillis())
                .build();
        timerPresetRepository.save(preset);

        String isoDate = futureQueryDate.format(FORMATTER);

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .queryParam("updatedAfter", isoDate)
                .when()
                .get("/api/v1/timer-presets/sync");

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }
}
