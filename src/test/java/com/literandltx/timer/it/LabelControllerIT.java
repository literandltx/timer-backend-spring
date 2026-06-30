package com.literandltx.timer.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.literandltx.timer.dto.label.LabelCreateRequestDto;
import com.literandltx.timer.dto.label.LabelUpdateRequestDto;
import com.literandltx.timer.dto.user.UserLoginRequestDto;
import com.literandltx.timer.model.Label;
import com.literandltx.timer.model.Role;
import com.literandltx.timer.model.RoleName;
import com.literandltx.timer.model.User;
import com.literandltx.timer.repository.LabelRepository;
import com.literandltx.timer.repository.RoleRepository;
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

public class LabelControllerIT extends BaseIntegrationTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Autowired
    private UserRepository userRepository;

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
        jdbcTemplate.execute("DELETE FROM labels");
        jdbcTemplate.execute("DELETE FROM users_roles");
        jdbcTemplate.execute("DELETE FROM refresh_tokens");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    void shouldCreateLabel_WhenUserIsAuthenticated() {
        // 1. Arrange
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        String labelName = "Label 1";
        String labelColor = "#FF5733";

        LabelCreateRequestDto request = LabelCreateRequestDto.builder()
                .uuid(UUID.randomUUID())
                .name(labelName)
                .color(labelColor)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(request)
                .when()
                .post("/api/v1/labels");

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.CREATED.value())
                .body("uuid", notNullValue())
                .body("name", equalTo(labelName))
                .body("color", equalTo(labelColor))
                .body("createdAt", equalTo(now.format(FORMATTER)))
                .body("updatedAt", equalTo(now.format(FORMATTER)))
                .body("deleted", equalTo(false));
    }

    @Test
    void shouldReturnAllActiveLabels_WhenNoUpdatedAfterIsProvided() {
        // 1. Arrange
        String labelName1 = "Label 1";
        String labelName2 = "Label 2";
        String labelColor1 = "#FF0000";
        String labelColor2 = "#FFFF00";
        LocalDateTime now = LocalDateTime.now();

        Label label1 = Label.builder()
                .uuid(UUID.randomUUID())
                .name(labelName1)
                .color(labelColor1)
                .user(testUser)
                .createdAt(now)
                .updatedAt(now)
                .isDeleted(false)
                .build();

        Label label2 = Label.builder()
                .uuid(UUID.randomUUID())
                .name(labelName2)
                .color(labelColor2)
                .user(testUser)
                .createdAt(now.plusHours(1))
                .updatedAt(now.plusHours(1))
                .isDeleted(true)
                .build();

        labelRepository.saveAll(List.of(label1, label2));

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/api/v1/labels");

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(1))
                .body("name", hasItem(labelName1))
                .body("name", not(hasItem(labelName2)));
    }

    @Test
    void shouldReturnDeltaUpdates_WhenUpdatedAfterIsProvided() {
        // 1. Arrange
        String labelName1 = "Label 1";
        String labelName2 = "Label 2";
        String labelColor1 = "#FF0000";
        String labelColor2 = "#FFFF00";
        LocalDateTime past = LocalDateTime.now().minusDays(1);

        Label newLabel = Label.builder()
                .uuid(UUID.randomUUID())
                .name(labelName1)
                .color(labelColor1)
                .user(testUser)
                .createdAt(past)
                .updatedAt(past)
                .isDeleted(false)
                .build();

        Label oldLabel = Label.builder()
                .uuid(UUID.randomUUID())
                .name(labelName2)
                .color(labelColor2)
                .user(testUser)
                .createdAt(past)
                .updatedAt(past.plusDays(2))
                .isDeleted(false)
                .build();

        labelRepository.saveAll(List.of(oldLabel, newLabel));

        // 2. Act
        String isoDate = LocalDateTime.now().format(FORMATTER);
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .queryParam("updatedAfter", isoDate)
                .when()
                .get("/api/v1/labels");

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(1))
                .body("name", not(hasItem(labelName1)))
                .body("name", hasItem(labelName2));
    }

    @Test
    void shouldUpdateLabel_WhenUserIsAuthenticated_AndLabelExists() {
        // 1. Arrange
        String labelName = "Label";
        String labelColor = "#FF0000";
        String updatedLabelName = "Updated Label";
        String updatedLabelColor = "#FFFF00";
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime future = now.plusHours(1);

        Label originalLabel = Label.builder()
                .uuid(UUID.randomUUID())
                .name(labelName)
                .color(labelColor)
                .user(testUser)
                .createdAt(now)
                .updatedAt(now)
                .isDeleted(false)
                .build();

        Label savedLabel = labelRepository.save(originalLabel);

        LabelUpdateRequestDto request = LabelUpdateRequestDto.builder()
                .name(updatedLabelName)
                .color(updatedLabelColor)
                .updatedAt(future)
                .build();

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(request)
                .when()
                .put("/api/v1/labels/{id}", savedLabel.getUuid());

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("uuid", notNullValue())
                .body("name", equalTo(updatedLabelName))
                .body("color", equalTo(updatedLabelColor))
                .body("createdAt", equalTo(now.format(FORMATTER)))
                .body("updatedAt", equalTo(future.format(FORMATTER)))
                .body("deleted", equalTo(false));
    }

    @Test
    void shouldReturnForbidden_WhenUpdatingLabelBelongingToAnotherUser() {
        // 1. Arrange
        String victimLabelName = "Label";
        String victimLabelColor = "#FF0000";
        String adversaryLabelName = "Adversary Label";
        String adversaryLabelColor = "#FFFF00";
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = LocalDateTime.now().plusHours(1);

        User victimUser = User.builder()
                .email("victim@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userRepository.save(victimUser);

        Label victimLabel = Label.builder()
                .uuid(UUID.randomUUID())
                .name(victimLabelName)
                .color(victimLabelColor)
                .user(victimUser)
                .createdAt(now)
                .updatedAt(now)
                .isDeleted(false)
                .build();
        Label savedVictimLabel = labelRepository.save(victimLabel);

        LabelUpdateRequestDto request = LabelUpdateRequestDto.builder()
                .name(adversaryLabelName)
                .color(adversaryLabelColor)
                .updatedAt(future)
                .build();

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(request)
                .when()
                .put("/api/v1/labels/{id}", savedVictimLabel.getUuid());

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void shouldSoftDeleteLabel_WhenUserIsAuthenticated_AndOwnsLabel() {
        // 1. Arrange
        LocalDateTime now = LocalDateTime.now();

        Label label = Label.builder()
                .uuid(UUID.randomUUID())
                .name("To Be Deleted")
                .color("#000000")
                .user(testUser)
                .createdAt(now)
                .updatedAt(now)
                .isDeleted(false)
                .build();
        Label savedLabel = labelRepository.save(label);

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .delete("/api/v1/labels/{id}", savedLabel.getUuid());

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NO_CONTENT.value());

        Label fetchedLabel = labelRepository.findById(savedLabel.getUuid()).orElseThrow();
        assertTrue(fetchedLabel.isDeleted(), "Label should be marked as deleted");
    }

    @Test
    void shouldReturnNotFound_WhenDeletingNonExistentLabel() {
        // 1. Arrange
        UUID nonExistentId = UUID.randomUUID();

        // 2. Act
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .delete("/api/v1/labels/{id}", nonExistentId);

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }
}
