package com.literandltx.timer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.literandltx.timer.dto.user.UserLoginRequestDto;
import com.literandltx.timer.model.TimerOption;
import com.literandltx.timer.model.User;
import com.literandltx.timer.repository.TimerOptionRepository;
import com.literandltx.timer.repository.UserRepository;
import io.restassured.http.ContentType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

public class TimerOptionControllerIT extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TimerOptionRepository timerOptionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String userEmail = "testuser@example.com";
    private final String userPlainPassword = "password";
    private String authToken;
    private User testUser;

    @BeforeEach
    void setUpUser() {
        super.setUp();

        testUser = new User();
        testUser.setEmail(userEmail);
        testUser.setPassword(passwordEncoder.encode(userPlainPassword));
        userRepository.save(testUser);

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
        jdbcTemplate.execute("DELETE FROM timer_options");
        jdbcTemplate.execute("DELETE FROM users_roles");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    void shouldCreateTimerOption_WhenUserIsAuthenticated() {
        UUID newOptionId = UUID.randomUUID();

        Map<String, Object> request = Map.of(
                "uuid", newOptionId,
                "value", 25 
        );

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(request)
                .when()
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.CREATED.value())
                .body("uuid", notNullValue())
                .body("value", equalTo(25));
    }

    @Test
    void shouldReturnAllActiveTimerOptions_WhenNoUpdatedAfterIsProvided() {
        TimerOption option1 = new TimerOption();
        option1.setUuid(UUID.randomUUID());
        option1.setValue(15L);
        option1.setUser(testUser);
        option1.setDeleted(false);

        TimerOption option2 = new TimerOption();
        option2.setUuid(UUID.randomUUID());
        option2.setValue(45L);
        option2.setUser(testUser);
        option2.setDeleted(true);

        timerOptionRepository.saveAll(List.of(option1, option2));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/v1/timer-options")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(1))
                .body("value", hasItem(15))
                .body("value", not(hasItem(45)));
    }

    @Test
    void shouldReturnDeltaUpdates_WhenUpdatedAfterIsProvided() {
        LocalDateTime past = LocalDateTime.now().minusDays(5);
        LocalDateTime future = LocalDateTime.now().plusDays(5);

        TimerOption oldOption = new TimerOption();
        oldOption.setUuid(UUID.randomUUID());
        oldOption.setValue(10L);
        oldOption.setUpdatedAt(past);
        oldOption.setUser(testUser);

        TimerOption newOption = new TimerOption();
        newOption.setUuid(UUID.randomUUID());
        newOption.setValue(60L);
        newOption.setUpdatedAt(future);
        newOption.setUser(testUser);

        timerOptionRepository.saveAll(List.of(oldOption, newOption));

        String isoDate = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .queryParam("updatedAfter", isoDate)
                .when()
                .get("/api/v1/timer-options")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(1))
                .body("value", hasItem(60));
    }

    @Test
    void shouldUpdateTimerOption_WhenUserIsAuthenticated_AndOptionExists() {
        TimerOption originalOption = new TimerOption();
        originalOption.setUuid(UUID.randomUUID());
        originalOption.setValue(25L);
        originalOption.setUser(testUser);
        TimerOption savedOption = timerOptionRepository.save(originalOption);

        Map<String, Object> updateRequest = Map.of(
                "value", 30
        );

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(updateRequest)
                .when()
                .put("/api/v1/timer-options/{id}", savedOption.getUuid())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("uuid", equalTo(savedOption.getUuid().toString()))
                .body("value", equalTo(30));
    }

    @Test
    void shouldReturnForbidden_WhenUpdatingTimerOptionBelongingToAnotherUser() {
        User victimUser = new User();
        victimUser.setEmail("victim@example.com");
        victimUser.setPassword(passwordEncoder.encode("password"));
        userRepository.save(victimUser);

        TimerOption victimOption = new TimerOption();
        victimOption.setUuid(UUID.randomUUID());
        victimOption.setValue(120L);
        victimOption.setUser(victimUser);
        TimerOption savedVictimOption = timerOptionRepository.save(victimOption);

        Map<String, Object> updateRequest = Map.of(
                "value", 5
        );

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(updateRequest)
                .when()
                .put("/api/v1/timer-options/{id}", savedVictimOption.getUuid())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void shouldSoftDeleteTimerOption_WhenUserIsAuthenticated_AndOwnsOption() {
        TimerOption option = new TimerOption();
        option.setUuid(UUID.randomUUID());
        option.setValue(90L);
        option.setUser(testUser);
        option.setDeleted(false);
        TimerOption savedOption = timerOptionRepository.save(option);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .delete("/api/v1/timer-options/{id}", savedOption.getUuid())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NO_CONTENT.value());

        TimerOption fetchedOption = timerOptionRepository.findById(savedOption.getUuid()).orElseThrow();
        assertTrue(fetchedOption.isDeleted(), "Timer option should be marked as deleted");
    }

    @Test
    void shouldReturnNotFound_WhenDeletingNonExistentTimerOption() {
        UUID nonExistentId = UUID.randomUUID();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .delete("/api/v1/timer-options/{id}", nonExistentId)
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }
}
