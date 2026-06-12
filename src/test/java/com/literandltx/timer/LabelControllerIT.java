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
import com.literandltx.timer.model.User;
import com.literandltx.timer.repository.LabelRepository;
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

public class LabelControllerIT extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

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
        jdbcTemplate.execute("DELETE FROM labels");
        jdbcTemplate.execute("DELETE FROM users_roles");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    void shouldCreateLabel_WhenUserIsAuthenticated() {
        UUID newLabelId = UUID.randomUUID();

        Map<String, Object> request = Map.of(
                "uuid", newLabelId,
                "name", "Work",
                "color", "#FF5733"
        );

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(request)
                .when()
                .post("/api/v1/labels")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.CREATED.value())
                .body("uuid", notNullValue())
                .body("name", equalTo("Work"))
                .body("color", equalTo("#FF5733"));
    }

    @Test
    void shouldReturnAllActiveLabels_WhenNoUpdatedAfterIsProvided() {
        Label label1 = new Label();
        label1.setUuid(UUID.randomUUID());
        label1.setName("Active Work");
        label1.setColor("#FF0000");
        label1.setUser(testUser);
        label1.setDeleted(false);

        Label label2 = new Label();
        label2.setUuid(UUID.randomUUID());
        label2.setName("Deleted Work");
        label2.setColor("#000000");
        label2.setUser(testUser);
        label2.setDeleted(true);

        labelRepository.saveAll(List.of(label1, label2));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/v1/labels")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(1))
                .body("name", hasItem("Active Work"))
                .body("name", not(hasItem("Deleted Work")));
    }

    @Test
    void shouldReturnDeltaUpdates_WhenUpdatedAfterIsProvided() {
        LocalDateTime past = LocalDateTime.now().minusDays(5);
        LocalDateTime future = LocalDateTime.now().plusDays(5);

        Label oldLabel = new Label();
        oldLabel.setUuid(UUID.randomUUID());
        oldLabel.setColor("red");
        oldLabel.setName("Old Task");
        oldLabel.setUpdatedAt(past);
        oldLabel.setUser(testUser);

        Label newLabel = new Label();
        newLabel.setUuid(UUID.randomUUID());
        newLabel.setColor("blue");
        newLabel.setName("New Task");
        newLabel.setUpdatedAt(future);
        newLabel.setUser(testUser);

        labelRepository.saveAll(List.of(oldLabel, newLabel));

        String isoDate = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .queryParam("updatedAfter", isoDate)
                .when()
                .get("/api/v1/labels")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(1))
                .body("name", hasItem("New Task"));
    }

    @Test
    void shouldUpdateLabel_WhenUserIsAuthenticated_AndLabelExists() {
        Label originalLabel = new Label();
        originalLabel.setUuid(UUID.randomUUID());
        originalLabel.setName("Original Name");
        originalLabel.setColor("#000000");
        originalLabel.setUser(testUser);
        Label savedLabel = labelRepository.save(originalLabel);

        Map<String, Object> updateRequest = Map.of(
                "name", "Updated Name",
                "color", "#FFFFFF"
        );

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(updateRequest)
                .when()
                .put("/api/v1/labels/{id}", savedLabel.getUuid())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("uuid", equalTo(savedLabel.getUuid().toString()))
                .body("name", equalTo("Updated Name"))
                .body("color", equalTo("#FFFFFF"));
    }

    @Test
    void shouldReturnForbidden_WhenUpdatingLabelBelongingToAnotherUser() {
        User victimUser = new User();
        victimUser.setEmail("victim@example.com");
        victimUser.setPassword(passwordEncoder.encode("password"));
        userRepository.save(victimUser);

        Label victimLabel = new Label();
        victimLabel.setUuid(UUID.randomUUID());
        victimLabel.setName("Victim's Secret");
        victimLabel.setColor("#000000");
        victimLabel.setUser(victimUser);
        Label savedVictimLabel = labelRepository.save(victimLabel);

        Map<String, Object> updateRequest = Map.of(
                "name", "Hacked Name",
                "color", "#FF0000"
        );

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(updateRequest)
                .when()
                .put("/api/v1/labels/{id}", savedVictimLabel.getUuid())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void shouldSoftDeleteLabel_WhenUserIsAuthenticated_AndOwnsLabel() {
        Label label = new Label();
        label.setUuid(UUID.randomUUID());
        label.setName("To Be Deleted");
        label.setColor("#000000");
        label.setUser(testUser);
        label.setDeleted(false);
        Label savedLabel = labelRepository.save(label);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .delete("/api/v1/labels/{id}", savedLabel.getUuid())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NO_CONTENT.value());

        Label fetchedLabel = labelRepository.findById(savedLabel.getUuid()).orElseThrow();
        assertTrue(fetchedLabel.isDeleted(), "Label should be marked as deleted");
    }

    @Test
    void shouldReturnNotFound_WhenDeletingNonExistentLabel() {
        UUID nonExistentId = UUID.randomUUID();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .delete("/api/v1/labels/{id}", nonExistentId)
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }
}
