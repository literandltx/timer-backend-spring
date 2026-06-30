package com.literandltx.timer.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.literandltx.timer.dto.option.TimerOptionCreateRequestDto;
import com.literandltx.timer.dto.option.TimerOptionUpdateRequestDto;
import com.literandltx.timer.dto.user.UserLoginRequestDto;
import com.literandltx.timer.model.Role;
import com.literandltx.timer.model.RoleName;
import com.literandltx.timer.model.TimerOption;
import com.literandltx.timer.model.User;
import com.literandltx.timer.repository.RoleRepository;
import com.literandltx.timer.repository.TimerOptionRepository;
import com.literandltx.timer.repository.UserRepository;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

public class TimerOptionControllerIT extends BaseIntegrationTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TimerOptionRepository timerOptionRepository;

    @Autowired
    private RoleRepository roleRepository;

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

        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException("USER role not found in test DB"));

        testUser = User.builder()
                .email(userEmail)
                .password(passwordEncoder.encode(userPlainPassword))
                .roles(Set.of(userRole))
                .build();
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
        jdbcTemplate.execute("DELETE FROM refresh_tokens");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    void shouldCreateTimerOption_WhenUserIsAuthenticated() {
        // 1. Arrange
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        Long optionValue = 25L;

        TimerOptionCreateRequestDto request = TimerOptionCreateRequestDto.builder()
                .uuid(UUID.randomUUID())
                .value(optionValue)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(request)
                .when()
                .post("/api/v1/timer-options");

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.CREATED.value())
                .body("uuid", notNullValue())
                .body("value", equalTo(optionValue.intValue()))
                .body("createdAt", equalTo(now.format(FORMATTER)))
                .body("updatedAt", equalTo(now.format(FORMATTER)))
                .body("deleted", equalTo(false));
    }

    @Test
    void shouldReturnAllActiveTimerOptions_WhenNoUpdatedAfterIsProvided() {
        // 1. Arrange
        Long value1 = 15L;
        Long value2 = 45L;
        LocalDateTime now = LocalDateTime.now();

        TimerOption option1 = TimerOption.builder()
                .uuid(UUID.randomUUID())
                .value(value1)
                .user(testUser)
                .createdAt(now)
                .updatedAt(now)
                .isDeleted(false)
                .build();

        TimerOption option2 = TimerOption.builder()
                .uuid(UUID.randomUUID())
                .value(value2)
                .user(testUser)
                .createdAt(now.plusHours(1))
                .updatedAt(now.plusHours(1))
                .isDeleted(true)
                .build();

        timerOptionRepository.saveAll(List.of(option1, option2));

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/v1/timer-options");

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(1))
                .body("value", hasItem(value1.intValue()))
                .body("value", not(hasItem(value2.intValue())));
    }

    @Test
    void shouldReturnDeltaUpdates_WhenUpdatedAfterIsProvided() {
        // 1. Arrange
        Long oldValue = 10L;
        Long newValue = 60L;
        LocalDateTime past = LocalDateTime.now().minusDays(5);
        LocalDateTime future = LocalDateTime.now().plusDays(5);

        TimerOption newOption = TimerOption.builder()
                .uuid(UUID.randomUUID())
                .value(newValue)
                .user(testUser)
                .createdAt(past)
                .updatedAt(future)
                .isDeleted(false)
                .build();

        TimerOption oldOption = TimerOption.builder()
                .uuid(UUID.randomUUID())
                .value(oldValue)
                .user(testUser)
                .createdAt(past)
                .updatedAt(past)
                .isDeleted(false)
                .build();

        timerOptionRepository.saveAll(List.of(oldOption, newOption));

        // 2. Act
        String isoDate = LocalDateTime.now().format(FORMATTER);
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .queryParam("updatedAfter", isoDate)
                .when()
                .get("/api/v1/timer-options");

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(1))
                .body("value", not(hasItem(oldValue.intValue())))
                .body("value", hasItem(newValue.intValue()));
    }

    @Test
    void shouldUpdateTimerOption_WhenUserIsAuthenticated_AndOptionExists() {
        // 1. Arrange
        Long originalValue = 25L;
        Long updatedValue = 30L;
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime future = LocalDateTime.now().plusHours(1).truncatedTo(ChronoUnit.SECONDS);

        TimerOption originalOption = TimerOption.builder()
                .uuid(UUID.randomUUID())
                .value(originalValue)
                .user(testUser)
                .createdAt(now)
                .updatedAt(now)
                .isDeleted(false)
                .build();

        TimerOption savedOption = timerOptionRepository.save(originalOption);

        TimerOptionUpdateRequestDto request = TimerOptionUpdateRequestDto.builder()
                .value(updatedValue)
                .updatedAt(future)
                .build();

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(request)
                .when()
                .put("/api/v1/timer-options/{id}", savedOption.getUuid());

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("uuid", notNullValue())
                .body("value", equalTo(updatedValue.intValue()))
                .body("createdAt", equalTo(now.format(FORMATTER)))
                .body("updatedAt", equalTo(future.format(FORMATTER)))
                .body("deleted", equalTo(false));
    }

    @Test
    void shouldReturnForbidden_WhenUpdatingTimerOptionBelongingToAnotherUser() {
        // 1. Arrange
        Long victimValue = 120L;
        Long adversaryValue = 5L;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = LocalDateTime.now().plusHours(1);

        User victimUser = User.builder()
                .email("victim@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userRepository.save(victimUser);

        TimerOption victimOption = TimerOption.builder()
                .uuid(UUID.randomUUID())
                .value(victimValue)
                .user(victimUser)
                .createdAt(now)
                .updatedAt(now)
                .isDeleted(false)
                .build();
        TimerOption savedVictimOption = timerOptionRepository.save(victimOption);

        TimerOptionUpdateRequestDto request = TimerOptionUpdateRequestDto.builder()
                .value(adversaryValue)
                .updatedAt(future)
                .build();

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(request)
                .when()
                .put("/api/v1/timer-options/{id}", savedVictimOption.getUuid());

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void shouldSoftDeleteTimerOption_WhenUserIsAuthenticated_AndOwnsOption() {
        // 1. Arrange
        LocalDateTime now = LocalDateTime.now();

        TimerOption option = TimerOption.builder()
                .uuid(UUID.randomUUID())
                .value(90L)
                .user(testUser)
                .createdAt(now)
                .updatedAt(now)
                .isDeleted(false)
                .build();
        TimerOption savedOption = timerOptionRepository.save(option);

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .delete("/api/v1/timer-options/{id}", savedOption.getUuid());

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NO_CONTENT.value());

        TimerOption fetchedOption = timerOptionRepository.findById(savedOption.getUuid()).orElseThrow();
        assertTrue(fetchedOption.isDeleted(), "Timer option should be marked as deleted");
    }

    @Test
    void shouldReturnNotFound_WhenDeletingNonExistentTimerOption() {
        // 1. Arrange
        UUID nonExistentId = UUID.randomUUID();

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .delete("/api/v1/timer-options/{id}", nonExistentId);

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }
}
