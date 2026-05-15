package com.example.timer_backend.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import com.example.timer_backend.dto.timer.entry.CreateTimerEntryRequestDto;
import com.example.timer_backend.dto.timer.entry.TimerEntryRequestDto;
import com.example.timer_backend.model.Label;
import com.example.timer_backend.model.TimerEntry;
import com.example.timer_backend.model.User;
import com.example.timer_backend.repository.LabelRepository;
import com.example.timer_backend.repository.TimerEntryRepository;
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

class TimerEntryControllerIT extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LabelRepository labelRepository;

    @Autowired
    private TimerEntryRepository timerEntryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String userEmail = "entryuser@example.com";
    private final String userPlainPassword = "password";
    private User testUser;
    private Label testLabel;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email(userEmail)
                .password(passwordEncoder.encode(userPlainPassword))
                .build();
        userRepository.save(testUser);

        testLabel = Label.builder()
                .name("Work")
                .color("#FF0000")
                .user(testUser)
                .build();
        labelRepository.save(testLabel);
    }

    @AfterEach
    void tearDown() {
        timerEntryRepository.deleteAll();
        labelRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateTimerEntry_WhenUserIsAuthenticated_AndLabelExists() {
        long now = Instant.now().getEpochSecond();
        CreateTimerEntryRequestDto request = CreateTimerEntryRequestDto.builder()
                .labelId(testLabel.getId())
                .durationSeconds(60L)
                .startTime(now)
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(request)
                .when()
                .post("/api/v1/timer-entries")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.CREATED.value())
                .body("id", notNullValue())
                .body("labelId", equalTo(testLabel.getId().intValue()))
                .body("durationSeconds", equalTo(60))
                .body("startTime", equalTo((int) now)); // Cast to int for Hamcrest matching if JSON returns number
    }

    @Test
    void shouldReturnBadRequest_WhenCreatingEntry_WithNullLabelId() {
        CreateTimerEntryRequestDto request = CreateTimerEntryRequestDto.builder()
                .labelId(null)
                .durationSeconds(60L)
                .startTime(Instant.now().getEpochSecond())
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(request)
                .when()
                .post("/api/v1/timer-entries")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void shouldReturnNotFound_WhenCreatingEntry_WithNonExistentLabelId() {
        CreateTimerEntryRequestDto request = CreateTimerEntryRequestDto.builder()
                .labelId(99999L)
                .durationSeconds(60L)
                .startTime(Instant.now().getEpochSecond())
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(request)
                .when()
                .post("/api/v1/timer-entries")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void shouldReturnForbidden_WhenCreatingEntry_WithLabelBelongingToAnotherUser() {
        User otherUser = User.builder().email("other@example.com").password("pass").build();
        userRepository.save(otherUser);

        Label otherLabel = Label.builder()
                .name("Other")
                .user(otherUser)
                .color("#0000FF")
                .build();
        labelRepository.save(otherLabel);

        CreateTimerEntryRequestDto request = CreateTimerEntryRequestDto.builder()
                .labelId(otherLabel.getId())
                .durationSeconds(60L)
                .startTime(Instant.now().getEpochSecond())
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(request)
                .when()
                .post("/api/v1/timer-entries")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void shouldGetTimerEntryById_WhenUserIsAuthenticated() {
        TimerEntry entry = TimerEntry.builder()
                .user(testUser)
                .label(testLabel)
                .durationSeconds(120L)
                .startTime(Instant.now().getEpochSecond())
                .build();
        TimerEntry savedEntry = timerEntryRepository.save(entry);

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .get("/api/v1/timer-entries/{id}", savedEntry.getId())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(savedEntry.getId().intValue()))
                .body("labelId", equalTo(testLabel.getId().intValue()));
    }

    @Test
    void shouldReturnForbidden_WhenGettingEntryBelongingToAnotherUser() {
        User otherUser = User.builder().email("victim@example.com").password("pass").build();
        userRepository.save(otherUser);

        Label otherLabel = Label.builder()
                .name("Other")
                .user(otherUser)
                .color("#0000FF")
                .build();
        labelRepository.save(otherLabel);

        TimerEntry otherEntry = TimerEntry.builder()
                .user(otherUser)
                .label(otherLabel)
                .durationSeconds(10L)
                .startTime(Instant.now().getEpochSecond())
                .build();
        TimerEntry savedOtherEntry = timerEntryRepository.save(otherEntry);

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .get("/api/v1/timer-entries/{id}", savedOtherEntry.getId())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void shouldReturnNotFound_WhenGettingNonExistentTimerEntry() {
        long nonExistentId = 99999L;

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .get("/api/v1/timer-entries/{id}", nonExistentId)
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void shouldUpdateTimerEntry_WhenUserIsAuthenticated() {
        TimerEntry entry = TimerEntry.builder()
                .user(testUser)
                .label(testLabel)
                .durationSeconds(100L)
                .startTime(Instant.now().getEpochSecond())
                .build();
        TimerEntry savedEntry = timerEntryRepository.save(entry);

        TimerEntryRequestDto updateRequest = new TimerEntryRequestDto();
        updateRequest.setLabelId(testLabel.getId());
        updateRequest.setDurationSeconds(200L);
        updateRequest.setStartTime(savedEntry.getStartTime());

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(updateRequest)
                .when()
                .put("/api/v1/timer-entries/{id}", savedEntry.getId())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(savedEntry.getId().intValue()))
                .body("durationSeconds", equalTo(200));
    }

    @Test
    void shouldReturnNotFound_WhenUpdatingNonExistentTimerEntry() {
        long nonExistentId = 99999L;
        TimerEntryRequestDto updateRequest = new TimerEntryRequestDto();
        updateRequest.setLabelId(testLabel.getId());
        updateRequest.setDurationSeconds(200L);
        updateRequest.setStartTime(Instant.now().getEpochSecond());

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(updateRequest)
                .when()
                .put("/api/v1/timer-entries/{id}", nonExistentId)
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void shouldReturnForbidden_WhenUpdatingEntryWithLabelBelongingToAnotherUser() {
        TimerEntry entry = TimerEntry.builder()
                .user(testUser)
                .label(testLabel)
                .durationSeconds(100L)
                .startTime(Instant.now().getEpochSecond())
                .build();
        TimerEntry savedEntry = timerEntryRepository.save(entry);

        User otherUser = User.builder().email("other2@example.com").password("pass").build();
        userRepository.save(otherUser);
        Label otherLabel = Label.builder()
                .name("Other")
                .user(otherUser)
                .color("#0000FF")
                .build();
        labelRepository.save(otherLabel);

        TimerEntryRequestDto updateRequest = new TimerEntryRequestDto();
        updateRequest.setLabelId(otherLabel.getId());
        updateRequest.setDurationSeconds(200L);
        updateRequest.setStartTime(savedEntry.getStartTime());

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(updateRequest)
                .when()
                .put("/api/v1/timer-entries/{id}", savedEntry.getId())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void shouldDeleteTimerEntry_WhenUserIsAuthenticated_AndOwnsEntry() {
        TimerEntry entry = TimerEntry.builder()
                .user(testUser)
                .label(testLabel)
                .durationSeconds(100L)
                .startTime(Instant.now().getEpochSecond())
                .build();
        TimerEntry savedEntry = timerEntryRepository.save(entry);

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .delete("/api/v1/timer-entries/{id}", savedEntry.getId())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NO_CONTENT.value());

        boolean exists = timerEntryRepository.existsById(savedEntry.getId());
        Assertions.assertFalse(exists, "Timer entry should have been deleted");
    }

    @Test
    void shouldReturnForbidden_WhenDeletingEntryBelongingToAnotherUser() {
        User otherUser = User.builder().email("victim2@example.com").password("pass").build();
        userRepository.save(otherUser);

        Label otherLabel = Label.builder()
                .name("Other")
                .user(otherUser)
                .color("#0000FF")
                .build();
        labelRepository.save(otherLabel);

        TimerEntry otherEntry = TimerEntry.builder()
                .user(otherUser)
                .label(otherLabel)
                .durationSeconds(10L)
                .startTime(Instant.now().getEpochSecond())
                .build();
        TimerEntry savedOtherEntry = timerEntryRepository.save(otherEntry);

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .delete("/api/v1/timer-entries/{id}", savedOtherEntry.getId())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.FORBIDDEN.value());

        Assertions.assertTrue(timerEntryRepository.existsById(savedOtherEntry.getId()));
    }

    @Test
    void shouldReturnAllEntries_WhenUserHasEntries() {
        TimerEntry entry1 = TimerEntry.builder().user(testUser).label(testLabel).durationSeconds(10L).startTime(1L).build();
        TimerEntry entry2 = TimerEntry.builder().user(testUser).label(testLabel).durationSeconds(20L).startTime(2L).build();
        timerEntryRepository.save(entry1);
        timerEntryRepository.save(entry2);

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .get("/api/v1/timer-entries")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(2));
    }

    @Test
    void shouldReturnEmptyList_WhenUserHasNoEntries() {
        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .get("/api/v1/timer-entries")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(0));
    }
}
