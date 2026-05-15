package com.example.timer_backend.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import com.example.timer_backend.dto.timer.setting.CreateTimerSettingRequestDto;
import com.example.timer_backend.dto.timer.setting.TimerSettingRequestDto;
import com.example.timer_backend.model.TimerOption;
import com.example.timer_backend.model.TimerSetting;
import com.example.timer_backend.model.User;
import com.example.timer_backend.repository.LabelRepository;
import com.example.timer_backend.repository.TimerOptionRepository;
import com.example.timer_backend.repository.TimerSettingRepository;
import com.example.timer_backend.repository.UserRepository;
import io.restassured.http.ContentType;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

class TimerSettingControllerIT extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LabelRepository labelRepository;

    @Autowired
    private TimerOptionRepository timerOptionRepository;

    @Autowired
    private TimerSettingRepository timerSettingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String userEmail = "settinguser@example.com";
    private final String userPlainPassword = "password";
    private User testUser;
    private TimerOption testOption;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email(userEmail)
                .password(passwordEncoder.encode(userPlainPassword))
                .build();
        userRepository.save(testUser);

        testOption = TimerOption.builder()
                .value(25L)
                .user(testUser)
                .build();
        timerOptionRepository.save(testOption);
    }

    @AfterEach
    void tearDown() {
        timerSettingRepository.deleteAll();
        timerOptionRepository.deleteAll();
        labelRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateTimerSetting_WhenUserIsAuthenticated_AndOptionExists() {
        CreateTimerSettingRequestDto request = CreateTimerSettingRequestDto.builder()
                .timerOptionId(testOption.getId())
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(request)
                .when()
                .post("/api/v1/timer-settings")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.CREATED.value())
                .body("id", notNullValue())
                .body("timerOptionId", equalTo(testOption.getId().intValue()))
                .body("value", equalTo(25));
    }

    @Test
    void shouldReturnBadRequest_WhenCreatingSettings_WithNullOptionId() {
        CreateTimerSettingRequestDto request = CreateTimerSettingRequestDto.builder()
                .timerOptionId(null)
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(request)
                .when()
                .post("/api/v1/timer-settings")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void shouldReturnNotFound_WhenCreatingSettings_WithNonExistentOptionId() {
        CreateTimerSettingRequestDto request = CreateTimerSettingRequestDto.builder()
                .timerOptionId(99999L)
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(request)
                .when()
                .post("/api/v1/timer-settings")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void shouldGetTimerSettingById_WhenUserIsAuthenticated() {
        TimerSetting setting = TimerSetting.builder()
                .user(testUser)
                .preference(testOption)
                .lastUpdated(Instant.now().toEpochMilli())
                .build();
        TimerSetting savedSetting = timerSettingRepository.save(setting);

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .get("/api/v1/timer-settings/{id}", savedSetting.getId())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(savedSetting.getId().intValue()))
                .body("timerOptionId", equalTo(testOption.getId().intValue()));
    }

    @Test
    void shouldReturnForbidden_WhenGettingSettingsBelongingToAnotherUser() {
        User otherUser = User.builder().email("other@example.com").password("pass").build();
        userRepository.save(otherUser);

        TimerOption otherOption = TimerOption.builder().value(50L).user(otherUser).build();
        timerOptionRepository.save(otherOption);

        TimerSetting otherSetting = TimerSetting.builder().user(otherUser).preference(otherOption).lastUpdated(1L).build();
        TimerSetting savedOtherSetting = timerSettingRepository.save(otherSetting);

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .get("/api/v1/timer-settings/{id}", savedOtherSetting.getId())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void shouldReturnNotFound_WhenGettingNonExistentTimerSetting() {
        long nonExistentId = 99999L;

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .get("/api/v1/timer-settings/{id}", nonExistentId)
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void shouldReturnNotFound_WhenUpdatingNonExistentTimerSetting() {
        long nonExistentId = 99999L;

        TimerSettingRequestDto updateRequest = TimerSettingRequestDto.builder()
                .timerOptionId(testOption.getId())
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(updateRequest)
                .when()
                .put("/api/v1/timer-settings/{id}", nonExistentId)
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void shouldUpdateTimerSetting_WhenUserIsAuthenticated() {
        TimerSetting setting = TimerSetting.builder().user(testUser).preference(testOption).lastUpdated(1L).build();
        TimerSetting savedSetting = timerSettingRepository.save(setting);

        TimerOption newOption = TimerOption.builder().value(50L).user(testUser).build();
        timerOptionRepository.save(newOption);

        TimerSettingRequestDto updateRequest = TimerSettingRequestDto.builder()
                .timerOptionId(newOption.getId())
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(updateRequest)
                .when()
                .put("/api/v1/timer-settings/{id}", savedSetting.getId())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(savedSetting.getId().intValue()))
                .body("timerOptionId", equalTo(newOption.getId().intValue()))
                .body("value", equalTo(50));
    }

    @Test
    void shouldReturnNotFound_WhenUpdatingWithNonExistentOptionId() {
        TimerSetting setting = TimerSetting.builder().user(testUser).preference(testOption).lastUpdated(1L).build();
        TimerSetting savedSetting = timerSettingRepository.save(setting);

        TimerSettingRequestDto updateRequest = TimerSettingRequestDto.builder()
                .timerOptionId(999999L)
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(updateRequest)
                .when()
                .put("/api/v1/timer-settings/{id}", savedSetting.getId())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void shouldDeleteTimerSetting_WhenUserIsAuthenticated_AndOwnsSettings() {
        TimerSetting setting = TimerSetting.builder().user(testUser).preference(testOption).lastUpdated(1L).build();
        TimerSetting savedSetting = timerSettingRepository.save(setting);

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .delete("/api/v1/timer-settings/{id}", savedSetting.getId())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NO_CONTENT.value());

        boolean exists = timerSettingRepository.existsById(savedSetting.getId());
        Assertions.assertFalse(exists, "Timer settings should have been deleted");
    }

    @Test
    void shouldReturnForbidden_WhenDeletingSettingsBelongingToAnotherUser() {
        User otherUser = User.builder().email("victim@example.com").password("pass").build();
        userRepository.save(otherUser);

        TimerOption otherOption = TimerOption.builder().value(50L).user(otherUser).build();
        timerOptionRepository.save(otherOption);

        TimerSetting otherSetting = TimerSetting.builder().user(otherUser).preference(otherOption).lastUpdated(1L).build();
        TimerSetting savedOtherSetting = timerSettingRepository.save(otherSetting);

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .delete("/api/v1/timer-settings/{id}", savedOtherSetting.getId())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.FORBIDDEN.value());

        Assertions.assertTrue(timerSettingRepository.existsById(savedOtherSetting.getId()));
    }

    @Test
    void shouldReturnAllSettings_WhenUserHasSettings() {
        TimerSetting setting = TimerSetting.builder().user(testUser).preference(testOption).lastUpdated(1L).build();
        timerSettingRepository.save(setting);

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .get("/api/v1/timer-settings")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(1))
                .body("[0].id", equalTo(setting.getId().intValue()));
    }

    @Test
    void shouldReturnEmptyList_WhenUserHasNoSettings() {
        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .get("/api/v1/timer-settings")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(0));
    }
}
