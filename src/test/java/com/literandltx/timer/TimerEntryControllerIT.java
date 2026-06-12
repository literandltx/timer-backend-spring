package com.literandltx.timer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.literandltx.timer.dto.user.UserLoginRequestDto;
import com.literandltx.timer.model.Label;
import com.literandltx.timer.model.TimerEntry;
import com.literandltx.timer.model.User;
import com.literandltx.timer.repository.LabelRepository;
import com.literandltx.timer.repository.TimerEntryRepository;
import com.literandltx.timer.repository.UserRepository;
import io.restassured.http.ContentType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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

public class TimerEntryControllerIT extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TimerEntryRepository timerEntryRepository;

    @Autowired
    private LabelRepository labelRepository;

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

        testUser = new User();
        testUser.setEmail(userEmail);
        testUser.setPassword(passwordEncoder.encode(userPlainPassword));
        userRepository.save(testUser);

        defaultLabel = new Label();
        defaultLabel.setUuid(UUID.randomUUID());
        defaultLabel.setName("Default Label");
        defaultLabel.setColor("#000000");
        defaultLabel.setUser(testUser);
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
        UUID newEntryId = UUID.randomUUID();
        Long startTimeMs = System.currentTimeMillis();

        Map<String, Object> request = Map.of(
                "uuid", newEntryId,
                "labelId", defaultLabel.getUuid(),
                "startTime", startTimeMs,
                "durationSeconds", 3600
        );

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(request)
                .when()
                .post("/api/v1/timer-entries")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.CREATED.value())
                .body("uuid", notNullValue())
                .body("durationSeconds", equalTo(3600));
    }

    @Test
    void shouldReturnAllActiveTimerEntries_WhenNoUpdatedAfterIsProvided() {
        TimerEntry entry1 = new TimerEntry();
        entry1.setUuid(UUID.randomUUID());
        entry1.setDurationSeconds(1500L);
        entry1.setStartTime(System.currentTimeMillis());
        entry1.setLabel(defaultLabel);
        entry1.setUser(testUser);
        entry1.setDeleted(false);

        TimerEntry entry2 = new TimerEntry();
        entry2.setUuid(UUID.randomUUID());
        entry2.setDurationSeconds(3000L);
        entry2.setStartTime(System.currentTimeMillis() + 1);
        entry2.setLabel(defaultLabel);
        entry2.setUser(testUser);
        entry2.setDeleted(true);

        timerEntryRepository.saveAll(List.of(entry1, entry2));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/v1/timer-entries")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(1))
                .body("durationSeconds", hasItem(1500))
                .body("durationSeconds", not(hasItem(3000)));
    }

    @Test
    void shouldReturnDeltaUpdates_WhenUpdatedAfterIsProvided() {
        LocalDateTime past = LocalDateTime.now().minusDays(5);
        LocalDateTime future = LocalDateTime.now().plusDays(5);

        TimerEntry oldEntry = new TimerEntry();
        oldEntry.setUuid(UUID.randomUUID());
        oldEntry.setDurationSeconds(60L);
        oldEntry.setStartTime(System.currentTimeMillis());
        oldEntry.setLabel(defaultLabel);
        oldEntry.setUpdatedAt(past);
        oldEntry.setUser(testUser);

        TimerEntry newEntry = new TimerEntry();
        newEntry.setUuid(UUID.randomUUID());
        newEntry.setDurationSeconds(120L);
        newEntry.setStartTime(System.currentTimeMillis() + 1);
        newEntry.setLabel(defaultLabel);
        newEntry.setUpdatedAt(future);
        newEntry.setUser(testUser);

        timerEntryRepository.saveAll(List.of(oldEntry, newEntry));

        String isoDate = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .queryParam("updatedAfter", isoDate)
                .when()
                .get("/api/v1/timer-entries")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(1))
                .body("durationSeconds", hasItem(120));
    }

    @Test
    void shouldUpdateTimerEntry_WhenUserIsAuthenticated_AndEntryExists() {
        TimerEntry originalEntry = new TimerEntry();
        originalEntry.setUuid(UUID.randomUUID());
        originalEntry.setDurationSeconds(100L);
        originalEntry.setStartTime(System.currentTimeMillis());
        originalEntry.setLabel(defaultLabel);
        originalEntry.setUser(testUser);
        TimerEntry savedEntry = timerEntryRepository.save(originalEntry);

        Map<String, Object> updateRequest = Map.of(
                "durationSeconds", 200
        );

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(updateRequest)
                .when()
                .put("/api/v1/timer-entries/{id}", savedEntry.getUuid())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("uuid", equalTo(savedEntry.getUuid().toString()))
                .body("durationSeconds", equalTo(200));
    }

    @Test
    void shouldAssignLabelToTimerEntry_WhenUserOwnsBoth() {
        Label newLabel = new Label();
        newLabel.setUuid(UUID.randomUUID());
        newLabel.setName("Focus Work");
        newLabel.setColor("#FFF");
        newLabel.setUser(testUser);
        Label savedLabel = labelRepository.save(newLabel);

        TimerEntry entry = new TimerEntry();
        entry.setUuid(UUID.randomUUID());
        entry.setDurationSeconds(500L);
        entry.setStartTime(System.currentTimeMillis());
        entry.setLabel(defaultLabel);
        entry.setUser(testUser);
        TimerEntry savedEntry = timerEntryRepository.save(entry);

        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("labelId", savedLabel.getUuid());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(updateRequest)
                .when()
                .put("/api/v1/timer-entries/{id}", savedEntry.getUuid())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("labelId", equalTo(savedLabel.getUuid().toString()));
    }

    @Test
    void shouldReturnForbidden_WhenUpdatingTimerEntryWithAnotherUsersLabel() {
        User victimUser = new User();
        victimUser.setEmail("victim@example.com");
        victimUser.setPassword(passwordEncoder.encode("password"));
        userRepository.save(victimUser);

        Label victimLabel = new Label();
        victimLabel.setUuid(UUID.randomUUID());
        victimLabel.setName("Victim's Label");
        victimLabel.setColor("#000");
        victimLabel.setUser(victimUser);
        Label savedVictimLabel = labelRepository.save(victimLabel);

        TimerEntry entry = new TimerEntry();
        entry.setUuid(UUID.randomUUID());
        entry.setDurationSeconds(500L);
        entry.setStartTime(System.currentTimeMillis());
        entry.setLabel(defaultLabel);
        entry.setUser(testUser);
        TimerEntry savedEntry = timerEntryRepository.save(entry);

        Map<String, Object> updateRequest = Map.of(
                "labelId", savedVictimLabel.getUuid()
        );

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(updateRequest)
                .when()
                .put("/api/v1/timer-entries/{id}", savedEntry.getUuid())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void shouldReturnForbidden_WhenUpdatingTimerEntryBelongingToAnotherUser() {
        User victimUser = new User();
        victimUser.setEmail("victim2@example.com");
        victimUser.setPassword(passwordEncoder.encode("password"));
        userRepository.save(victimUser);

        Label victimLabel = new Label();
        victimLabel.setUuid(UUID.randomUUID());
        victimLabel.setName("Victim Label 2");
        victimLabel.setColor("#000");
        victimLabel.setUser(victimUser);
        labelRepository.save(victimLabel);

        TimerEntry victimEntry = new TimerEntry();
        victimEntry.setUuid(UUID.randomUUID());
        victimEntry.setDurationSeconds(999L);
        victimEntry.setStartTime(System.currentTimeMillis());
        victimEntry.setLabel(victimLabel);
        victimEntry.setUser(victimUser);
        TimerEntry savedVictimEntry = timerEntryRepository.save(victimEntry);

        Map<String, Object> updateRequest = Map.of(
                "durationSeconds", 10
        );

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(updateRequest)
                .when()
                .put("/api/v1/timer-entries/{id}", savedVictimEntry.getUuid())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void shouldSoftDeleteTimerEntry_WhenUserIsAuthenticated_AndOwnsEntry() {
        TimerEntry entry = new TimerEntry();
        entry.setUuid(UUID.randomUUID());
        entry.setDurationSeconds(55L);
        entry.setStartTime(System.currentTimeMillis());
        entry.setLabel(defaultLabel);
        entry.setUser(testUser);
        entry.setDeleted(false);
        TimerEntry savedEntry = timerEntryRepository.save(entry);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .delete("/api/v1/timer-entries/{id}", savedEntry.getUuid())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NO_CONTENT.value());

        TimerEntry fetchedEntry = timerEntryRepository.findById(savedEntry.getUuid()).orElseThrow();
        assertTrue(fetchedEntry.isDeleted(), "Timer entry should be marked as deleted");
    }

    @Test
    void shouldReturnNotFound_WhenDeletingNonExistentTimerEntry() {
        UUID nonExistentId = UUID.randomUUID();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .delete("/api/v1/timer-entries/{id}", nonExistentId)
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }
}
