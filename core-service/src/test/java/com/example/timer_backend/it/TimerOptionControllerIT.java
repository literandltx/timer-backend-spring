package com.example.timer_backend.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import com.example.timer_backend.dto.timer.option.CreateTimerOptionRequestDto;
import com.example.timer_backend.dto.timer.option.TimerOptionRequestDto;
import com.example.timer_backend.model.TimerOption;
import com.example.timer_backend.model.User;
import com.example.timer_backend.repository.LabelRepository;
import com.example.timer_backend.repository.TimerOptionRepository;
import com.example.timer_backend.repository.UserRepository;
import io.restassured.http.ContentType;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

class TimerOptionControllerIT extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LabelRepository labelRepository;

    @Autowired
    private TimerOptionRepository timerOptionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String userEmail = "timeruser@example.com";
    private final String userPlainPassword = "password";

    @BeforeEach
    void setUpUser() {
        User testUser = User.builder()
                .email(userEmail)
                .password(passwordEncoder.encode(userPlainPassword))
                .build();
        userRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        timerOptionRepository.deleteAll();
        labelRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateTimerOption_WhenUserIsAuthenticated() {
        CreateTimerOptionRequestDto request = CreateTimerOptionRequestDto.builder()
                .value(25L)
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(request)
                .when()
                .post("/api/v1/timer-options")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.CREATED.value())
                .body("id", notNullValue())
                .body("value", equalTo(25));
    }

    @Test
    void shouldReturnBadRequest_WhenCreatingInvalidTimerOption() {
        CreateTimerOptionRequestDto request = CreateTimerOptionRequestDto.builder()
                .value(0L)
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(request)
                .when()
                .post("/api/v1/timer-options")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void shouldGetTimerOptionById_WhenUserIsAuthenticated_AndOptionExists() {
        User testUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Test user not found"));

        TimerOption option = TimerOption.builder()
                .value(50L)
                .user(testUser)
                .build();

        TimerOption savedOption = timerOptionRepository.save(option);

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .get("/api/v1/timer-options/{id}", savedOption.getId())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(savedOption.getId().intValue()))
                .body("value", equalTo(50));
    }

    @Test
    void shouldReturnNotFound_WhenTimerOptionDoesNotExist() {
        long nonExistentId = 99999L;

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .get("/api/v1/timer-options/{id}", nonExistentId)
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void shouldReturnForbidden_WhenGettingTimerOptionBelongingToAnotherUser() {
        User victimUser = User.builder()
                .email("victim.timer@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userRepository.save(victimUser);

        TimerOption victimOption = TimerOption.builder()
                .value(60L)
                .user(victimUser)
                .build();
        TimerOption savedVictimOption = timerOptionRepository.save(victimOption);

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .get("/api/v1/timer-options/{id}", savedVictimOption.getId())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void shouldUpdateTimerOption_WhenUserIsAuthenticated_AndOptionExists() {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Test user not found"));

        TimerOption originalOption = TimerOption.builder()
                .value(15L)
                .user(currentUser)
                .build();

        TimerOption savedOption = timerOptionRepository.save(originalOption);

        TimerOptionRequestDto updateRequest = TimerOptionRequestDto.builder()
                .id(savedOption.getId())
                .value(30L)
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(updateRequest)
                .when()
                .put("/api/v1/timer-options/{id}", savedOption.getId())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(savedOption.getId().intValue()))
                .body("value", equalTo(30));
    }

    @Test
    void shouldReturnForbidden_WhenUpdatingTimerOptionBelongingToAnotherUser() {
        User victimUser = User.builder()
                .email("victim.update@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userRepository.save(victimUser);

        TimerOption victimOption = TimerOption.builder()
                .value(10L)
                .user(victimUser)
                .build();
        TimerOption savedVictimOption = timerOptionRepository.save(victimOption);

        TimerOptionRequestDto updateRequest = TimerOptionRequestDto.builder()
                .id(savedVictimOption.getId())
                .value(100L)
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(updateRequest)
                .when()
                .put("/api/v1/timer-options/{id}", savedVictimOption.getId())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void shouldReturnNotFound_WhenUpdatingNonExistentTimerOption() {
        long nonExistentId = 99999L;

        TimerOptionRequestDto updateRequest = TimerOptionRequestDto.builder()
                .id(nonExistentId)
                .value(30L)
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(updateRequest)
                .when()
                .put("/api/v1/timer-options/{id}", nonExistentId)
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void shouldDeleteTimerOption_WhenUserIsAuthenticated_AndOwnsOption() {
        User testUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Test user not found"));

        TimerOption option = TimerOption.builder()
                .value(45L)
                .user(testUser)
                .build();

        TimerOption savedOption = timerOptionRepository.save(option);

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .delete("/api/v1/timer-options/{id}", savedOption.getId())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NO_CONTENT.value());

        boolean exists = timerOptionRepository.existsById(savedOption.getId());
        Assertions.assertFalse(exists, "Timer option should have been deleted from DB");
    }

    @Test
    void shouldReturnForbidden_WhenDeletingTimerOptionBelongingToAnotherUser() {
        User victimUser = User.builder()
                .email("victim.delete@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userRepository.save(victimUser);

        TimerOption victimOption = TimerOption.builder()
                .value(20L)
                .user(victimUser)
                .build();
        TimerOption savedVictimOption = timerOptionRepository.save(victimOption);

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .delete("/api/v1/timer-options/{id}", savedVictimOption.getId())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.FORBIDDEN.value());

        boolean exists = timerOptionRepository.existsById(savedVictimOption.getId());
        Assertions.assertTrue(exists, "Timer option should NOT be deleted");
    }

    @Test
    void shouldReturnAllTimerOptions_WhenUserHasOptions() {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        TimerOption option1 = TimerOption.builder().value(25L).user(user).build();
        TimerOption option2 = TimerOption.builder().value(50L).user(user).build();

        timerOptionRepository.saveAll(List.of(option1, option2));

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .get("/api/v1/timer-options")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.JSON)
                .body(".", hasSize(2))
                .body("value", hasItems(25, 50));
    }

    @Test
    void shouldReturnOnlyAuthenticatedUserOptions_WhenMultipleUsersExist() {
        User mainUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Main user not found"));

        User otherUser = User.builder()
                .email("other.user@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userRepository.save(otherUser);

        TimerOption mainOption1 = TimerOption.builder().value(10L).user(mainUser).build();
        TimerOption mainOption2 = TimerOption.builder().value(20L).user(mainUser).build();
        timerOptionRepository.saveAll(List.of(mainOption1, mainOption2));

        TimerOption otherOption = TimerOption.builder().value(99L).user(otherUser).build();
        timerOptionRepository.save(otherOption);

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .get("/api/v1/timer-options")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(2))
                .body("id", hasItems(mainOption1.getId().intValue(), mainOption2.getId().intValue()))
                .body("id", not(hasItem(otherOption.getId().intValue())));
    }
}
