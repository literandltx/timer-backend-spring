package com.literandltx.timer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.literandltx.timer.dto.entry.TimerEntryCreateRequestDto;
import com.literandltx.timer.dto.entry.TimerEntryUpdateRequestDto;
import com.literandltx.timer.dto.user.UserLoginRequestDto;
import com.literandltx.timer.model.Label;
import com.literandltx.timer.model.Role;
import com.literandltx.timer.model.RoleName;
import com.literandltx.timer.model.TimerEntry;
import com.literandltx.timer.model.User;
import com.literandltx.timer.repository.LabelRepository;
import com.literandltx.timer.repository.RoleRepository;
import com.literandltx.timer.repository.TimerEntryRepository;
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

public class TimerEntryControllerIT extends BaseIntegrationTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TimerEntryRepository timerEntryRepository;

    @Autowired
    private LabelRepository labelRepository;

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

        defaultLabel = Label.builder()
                .uuid(UUID.randomUUID())
                .name("Default Label")
                .color("#000000")
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();
        labelRepository.save(defaultLabel);

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
        jdbcTemplate.execute("DELETE FROM timer_entries");
        jdbcTemplate.execute("DELETE FROM labels");
        jdbcTemplate.execute("DELETE FROM users_roles");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    void shouldCreateTimerEntry_WhenUserIsAuthenticated() {
        // 1. Arrange
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        UUID newEntryId = UUID.randomUUID();
        Long startTimeMs = System.currentTimeMillis();
        Long durationSeconds = 3600L;

        TimerEntryCreateRequestDto request = TimerEntryCreateRequestDto.builder()
                .uuid(newEntryId)
                .labelId(defaultLabel.getUuid())
                .startTime(startTimeMs)
                .durationSeconds(durationSeconds)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(request)
                .when()
                .post("/api/v1/timer-entries");

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.CREATED.value())
                .body("uuid", notNullValue())
                .body("labelId", equalTo(defaultLabel.getUuid().toString()))
                .body("startTime", equalTo(startTimeMs))
                .body("durationSeconds", equalTo(durationSeconds.intValue()))
                .body("createdAt", equalTo(now.format(FORMATTER)))
                .body("updatedAt", equalTo(now.format(FORMATTER)))
                .body("deleted", equalTo(false));
    }

    @Test
    void shouldReturnAllActiveTimerEntries_WhenNoUpdatedAfterIsProvided() {
        // 1. Arrange
        Long duration1 = 1500L;
        Long duration2 = 3000L;
        LocalDateTime now = LocalDateTime.now();

        TimerEntry entry1 = TimerEntry.builder()
                .uuid(UUID.randomUUID())
                .durationSeconds(duration1)
                .startTime(System.currentTimeMillis())
                .label(defaultLabel)
                .user(testUser)
                .createdAt(now)
                .updatedAt(now)
                .isDeleted(false)
                .build();

        TimerEntry entry2 = TimerEntry.builder()
                .uuid(UUID.randomUUID())
                .durationSeconds(duration2)
                .startTime(System.currentTimeMillis() + 1)
                .label(defaultLabel)
                .user(testUser)
                .createdAt(now.plusHours(1))
                .updatedAt(now.plusHours(1))
                .isDeleted(true)
                .build();

        timerEntryRepository.saveAll(List.of(entry1, entry2));

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/v1/timer-entries");

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(1))
                .body("durationSeconds", hasItem(duration1.intValue()))
                .body("durationSeconds", not(hasItem(duration2.intValue())));
    }

    @Test
    void shouldReturnDeltaUpdates_WhenUpdatedAfterIsProvided() {
        // 1. Arrange
        Long oldDuration = 60L;
        Long newDuration = 120L;
        LocalDateTime past = LocalDateTime.now().minusDays(5);
        LocalDateTime future = LocalDateTime.now().plusDays(5);

        TimerEntry oldEntry = TimerEntry.builder()
                .uuid(UUID.randomUUID())
                .durationSeconds(oldDuration)
                .startTime(System.currentTimeMillis())
                .label(defaultLabel)
                .user(testUser)
                .createdAt(past)
                .updatedAt(past)
                .isDeleted(false)
                .build();

        TimerEntry newEntry = TimerEntry.builder()
                .uuid(UUID.randomUUID())
                .durationSeconds(newDuration)
                .startTime(System.currentTimeMillis() + 1)
                .label(defaultLabel)
                .user(testUser)
                .createdAt(past)
                .updatedAt(future)
                .isDeleted(false)
                .build();

        timerEntryRepository.saveAll(List.of(oldEntry, newEntry));

        // 2. Act
        String isoDate = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .queryParam("updatedAfter", isoDate)
                .when()
                .get("/api/v1/timer-entries");

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(1))
                .body("durationSeconds", not(hasItem(oldDuration.intValue())))
                .body("durationSeconds", hasItem(newDuration.intValue()));
    }

    @Test
    void shouldUpdateTimerEntry_WhenUserIsAuthenticated_AndEntryExists() {
        // 1. Arrange
        Long originalDuration = 100L;
        Long updatedDuration = 200L;
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime future = LocalDateTime.now().plusHours(1).truncatedTo(ChronoUnit.SECONDS);

        TimerEntry originalEntry = TimerEntry.builder()
                .uuid(UUID.randomUUID())
                .durationSeconds(originalDuration)
                .startTime(System.currentTimeMillis())
                .label(defaultLabel)
                .user(testUser)
                .createdAt(now)
                .updatedAt(now)
                .isDeleted(false)
                .build();

        TimerEntry savedEntry = timerEntryRepository.save(originalEntry);

        TimerEntryUpdateRequestDto request = TimerEntryUpdateRequestDto.builder()
                .durationSeconds(updatedDuration)
                .updatedAt(future)
                .build();

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(request)
                .when()
                .put("/api/v1/timer-entries/{id}", savedEntry.getUuid());

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("uuid", equalTo(savedEntry.getUuid().toString()))
                .body("durationSeconds", equalTo(updatedDuration.intValue()))
                .body("createdAt", equalTo(now.format(FORMATTER)))
                .body("updatedAt", equalTo(future.format(FORMATTER)))
                .body("deleted", equalTo(false));
    }

    @Test
    void shouldAssignLabelToTimerEntry_WhenUserOwnsBoth() {
        // 1. Arrange
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime future = LocalDateTime.now().plusHours(1).truncatedTo(ChronoUnit.SECONDS);

        Label newLabel = Label.builder()
                .uuid(UUID.randomUUID())
                .name("Focus Work")
                .color("#FFFFFF")
                .user(testUser)
                .createdAt(now)
                .updatedAt(now)
                .isDeleted(false)
                .build();
        Label savedLabel = labelRepository.save(newLabel);

        TimerEntry entry = TimerEntry.builder()
                .uuid(UUID.randomUUID())
                .durationSeconds(500L)
                .startTime(System.currentTimeMillis())
                .label(defaultLabel)
                .user(testUser)
                .createdAt(now)
                .updatedAt(now)
                .isDeleted(false)
                .build();
        TimerEntry savedEntry = timerEntryRepository.save(entry);

        TimerEntryUpdateRequestDto request = TimerEntryUpdateRequestDto.builder()
                .labelId(savedLabel.getUuid())
                .updatedAt(future)
                .build();

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(request)
                .when()
                .put("/api/v1/timer-entries/{id}", savedEntry.getUuid());

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("labelId", equalTo(savedLabel.getUuid().toString()))
                .body("updatedAt", equalTo(future.format(FORMATTER)));
    }

    @Test
    void shouldReturnForbidden_WhenUpdatingTimerEntryWithAnotherUsersLabel() {
        // 1. Arrange
        LocalDateTime now = LocalDateTime.now();

        User victimUser = User.builder()
                .email("victim@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userRepository.save(victimUser);

        Label victimLabel = Label.builder()
                .uuid(UUID.randomUUID())
                .name("Victim's Label")
                .color("#000000")
                .user(victimUser)
                .createdAt(now)
                .updatedAt(now)
                .isDeleted(false)
                .build();
        Label savedVictimLabel = labelRepository.save(victimLabel);

        TimerEntry entry = TimerEntry.builder()
                .uuid(UUID.randomUUID())
                .durationSeconds(500L)
                .startTime(System.currentTimeMillis())
                .label(defaultLabel)
                .user(testUser)
                .createdAt(now)
                .updatedAt(now)
                .isDeleted(false)
                .build();
        TimerEntry savedEntry = timerEntryRepository.save(entry);

        TimerEntryUpdateRequestDto request = TimerEntryUpdateRequestDto.builder()
                .labelId(savedVictimLabel.getUuid())
                .updatedAt(LocalDateTime.now().plusHours(1))
                .build();

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(request)
                .when()
                .put("/api/v1/timer-entries/{id}", savedEntry.getUuid());

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void shouldReturnForbidden_WhenUpdatingTimerEntryBelongingToAnotherUser() {
        // 1. Arrange
        LocalDateTime now = LocalDateTime.now();

        User victimUser = User.builder()
                .email("victim2@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userRepository.save(victimUser);

        Label victimLabel = Label.builder()
                .uuid(UUID.randomUUID())
                .name("Victim Label 2")
                .color("#000000")
                .user(victimUser)
                .createdAt(now)
                .updatedAt(now)
                .isDeleted(false)
                .build();
        labelRepository.save(victimLabel);

        TimerEntry victimEntry = TimerEntry.builder()
                .uuid(UUID.randomUUID())
                .durationSeconds(999L)
                .startTime(System.currentTimeMillis())
                .label(victimLabel)
                .user(victimUser)
                .createdAt(now)
                .updatedAt(now)
                .isDeleted(false)
                .build();
        TimerEntry savedVictimEntry = timerEntryRepository.save(victimEntry);

        TimerEntryUpdateRequestDto request = TimerEntryUpdateRequestDto.builder()
                .durationSeconds(10L)
                .updatedAt(LocalDateTime.now().plusHours(1))
                .build();

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(request)
                .when()
                .put("/api/v1/timer-entries/{id}", savedVictimEntry.getUuid());

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void shouldSoftDeleteTimerEntry_WhenUserIsAuthenticated_AndOwnsEntry() {
        // 1. Arrange
        LocalDateTime now = LocalDateTime.now();

        TimerEntry entry = TimerEntry.builder()
                .uuid(UUID.randomUUID())
                .durationSeconds(55L)
                .startTime(System.currentTimeMillis())
                .label(defaultLabel)
                .user(testUser)
                .createdAt(now)
                .updatedAt(now)
                .isDeleted(false)
                .build();
        TimerEntry savedEntry = timerEntryRepository.save(entry);

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .delete("/api/v1/timer-entries/{id}", savedEntry.getUuid());

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NO_CONTENT.value());

        TimerEntry fetchedEntry = timerEntryRepository.findById(savedEntry.getUuid()).orElseThrow();
        assertTrue(fetchedEntry.isDeleted(), "Timer entry should be marked as deleted");
    }

    @Test
    void shouldReturnNotFound_WhenDeletingNonExistentTimerEntry() {
        // 1. Arrange
        UUID nonExistentId = UUID.randomUUID();

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .delete("/api/v1/timer-entries/{id}", nonExistentId);

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }
}
