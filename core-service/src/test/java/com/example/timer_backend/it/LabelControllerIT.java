package com.example.timer_backend.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import com.example.timer_backend.dto.label.CreateLabelRequestDto;
import com.example.timer_backend.model.Label;
import com.example.timer_backend.model.User;
import com.example.timer_backend.repository.LabelRepository;
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

class LabelControllerIT extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LabelRepository labelRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String userEmail = "testuser@example.com";
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
        labelRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateLabel_WhenUserIsAuthenticated() {
        CreateLabelRequestDto request = CreateLabelRequestDto.builder()
                .name("Work Projects")
                .color("#FF5733")
                .build();

        given()
                .contentType(ContentType.JSON)
                //.header("Authorization", "Bearer " + token)
                .auth().basic(userEmail, userPlainPassword)
                .body(request)
                .when()
                .post("/api/v1/labels")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.CREATED.value())
                .body("id", notNullValue())
                .body("name", equalTo("Work Projects"))
                .body("color", equalTo("#FF5733"));
    }

    @Test
    void shouldGetLabelById_WhenUserIsAuthenticated_AndLabelExists() {
        User testUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Test user not found"));

        Label label = Label.builder()
                .name("Gym Routine")
                .color("#0000FF")
                .user(testUser)
                .build();

        Label savedLabel = labelRepository.save(label);

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .get("/api/v1/labels/{id}", savedLabel.getId())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(savedLabel.getId().intValue()))
                .body("name", equalTo("Gym Routine"))
                .body("color", equalTo("#0000FF"));
    }

    @Test
    void shouldReturnNotFound_WhenLabelDoesNotExist() {
        long nonExistentId = 99999L;

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .get("/api/v1/labels/{id}", nonExistentId)
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void shouldReturnForbidden_WhenGettingLabelBelongingToAnotherUser() {
        User victimUser = User.builder()
                .email("victim.get@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userRepository.save(victimUser);

        Label victimLabel = Label.builder()
                .name("Victim's Private Label")
                .color("#000000")
                .user(victimUser)
                .build();
        Label savedVictimLabel = labelRepository.save(victimLabel);

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .get("/api/v1/labels/{id}", savedVictimLabel.getId())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void shouldUpdateLabel_WhenUserIsAuthenticated_AndLabelExists() {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Test user not found"));

        Label originalLabel = Label.builder()
                .name("Original Name")
                .color("#000000")
                .user(currentUser)
                .build();

        Label savedLabel = labelRepository.save(originalLabel);

        CreateLabelRequestDto updateRequest = CreateLabelRequestDto.builder()
                .name("Updated Name")
                .color("#FFFFFF")
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(updateRequest)
                .when()
                .put("/api/v1/labels/{id}", savedLabel.getId())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(savedLabel.getId().intValue()))
                .body("name", equalTo("Updated Name"))
                .body("color", equalTo("#FFFFFF"));
    }

    @Test
    void shouldReturnNotFound_WhenUpdatingNonExistentLabel() {
        CreateLabelRequestDto request = CreateLabelRequestDto.builder()
                .name("Ghost Label")
                .color("#000000")
                .build();

        long nonExistentId = 999999L;
        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(request)
                .when()
                .put("/api/v1/labels/{id}", nonExistentId)
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void shouldReturnForbidden_WhenUpdatingLabelBelongingToAnotherUser() {
        User victimUser = User.builder()
                .email("victim@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userRepository.save(victimUser);

        Label victimLabel = Label.builder()
                .name("Victim's Secret")
                .color("#000000")
                .user(victimUser)
                .build();
        Label savedVictimLabel = labelRepository.save(victimLabel);

        CreateLabelRequestDto updateRequest = CreateLabelRequestDto.builder()
                .name("Hacked Name")
                .color("#FF0000")
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(updateRequest)
                .when()
                .put("/api/v1/labels/{id}", savedVictimLabel.getId())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void shouldDeleteLabel_WhenUserIsAuthenticated_AndOwnsLabel() {
        User testUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Test user not found"));

        Label label = Label.builder()
                .name("To Be Deleted")
                .color("#000000")
                .user(testUser)
                .build();

        Label savedLabel = labelRepository.save(label);

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .delete("/api/v1/labels/{id}", savedLabel.getId())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NO_CONTENT.value());

        boolean exists = labelRepository.existsById(savedLabel.getId());

        Assertions.assertFalse(exists, "Label should have been deleted from DB");
    }

    @Test
    void shouldReturnNotFound_WhenDeletingNonExistentLabel() {
        long nonExistentId = 99999L;

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .delete("/api/v1/labels/{id}", nonExistentId)
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void shouldReturnForbidden_WhenDeletingLabelBelongingToAnotherUser() {
        User victimUser = User.builder()
                .email("victim@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userRepository.save(victimUser);

        Label victimLabel = Label.builder()
                .name("Victim's Label")
                .color("#999999")
                .user(victimUser)
                .build();
        Label savedVictimLabel = labelRepository.save(victimLabel);

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .delete("/api/v1/labels/{id}", savedVictimLabel.getId())
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.FORBIDDEN.value());

        boolean exists = labelRepository.existsById(savedVictimLabel.getId());
        Assertions.assertTrue(exists, "Label should NOT be deleted");
    }

    @Test
    void shouldReturnAllLabels_WhenUserHasLabels() {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Label label1 = Label.builder()
                .name("Work")
                .color("#FF0000")
                .user(user)
                .build();

        Label label2 = Label.builder()
                .name("Personal")
                .color("#00FF00")
                .user(user)
                .build();

        labelRepository.saveAll(List.of(label1, label2));

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .get("/api/v1/labels")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.JSON)
                .body(".", hasSize(2))
                .body("name", hasItems("Work", "Personal"))
                .body("color", hasItems("#FF0000", "#00FF00"));
    }

    @Test
    void shouldReturnOnlyAuthenticatedUserLabels_WhenMultipleUsersExist() {
        User mainUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Main user not found"));

        User otherUser = User.builder()
                .email("other@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userRepository.save(otherUser);

        Label mainLabel1 = Label.builder().name("My Task 1").color("#000").user(mainUser).build();
        Label mainLabel2 = Label.builder().name("My Task 2").color("#000").user(mainUser).build();
        labelRepository.saveAll(List.of(mainLabel1, mainLabel2));

        Label otherLabel = Label.builder().name("Secret Task").color("#FFF").user(otherUser).build();
        labelRepository.save(otherLabel);

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .get("/api/v1/labels")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(2))
                .body("id", hasItems(mainLabel1.getId().intValue(), mainLabel2.getId().intValue()))
                .body("id", not(hasItem(otherLabel.getId().intValue())));
    }

    @Test
    void shouldReturnEmptyList_WhenUserHasNoLabels() {
        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .get("/api/v1/labels")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(0));
    }
}
