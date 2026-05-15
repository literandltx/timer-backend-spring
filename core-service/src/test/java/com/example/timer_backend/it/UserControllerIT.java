package com.example.timer_backend.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.example.timer_backend.dto.user.ChangeEmailRequestDto;
import com.example.timer_backend.dto.user.ChangePasswordRequestDto;
import com.example.timer_backend.dto.user.UserUpdateRequestDto;
import com.example.timer_backend.model.User;
import com.example.timer_backend.repository.LabelRepository;
import com.example.timer_backend.repository.UserRepository;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserControllerIT extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LabelRepository labelRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String userEmail = "testuser@example.com";
    private final String userPlainPassword = "password1234";
    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .email(userEmail)
                .password(passwordEncoder.encode(userPlainPassword))
                .build();
        userRepository.save(currentUser);
    }

    @AfterEach
    void tearDown() {
        labelRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldReturnCurrentUser_WhenAuthenticated() {
        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .get("/api/v1/users/me")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("email", equalTo(userEmail))
                .body("id", equalTo(currentUser.getId().intValue()));
    }

    @Test
    void shouldReturnUnauthorized_WhenGettingUserWithoutAuth() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/v1/users/me")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void shouldUpdateAccount_WhenRequestIsValid() {
        UserUpdateRequestDto request = new UserUpdateRequestDto();
        request.setEmail("updated@example.com");
        request.setCurrentPassword(userPlainPassword);
        request.setNewPassword("newPassword123");
        request.setConfirmationPassword("newPassword123");

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(request)
                .when()
                .put("/api/v1/users/me")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("email", equalTo("updated@example.com"));

        User updatedUser = userRepository.findById(currentUser.getId()).orElseThrow();
        Assertions.assertEquals("updated@example.com", updatedUser.getEmail());
    }

    @Test
    void shouldReturnBadRequest_WhenUpdateAccountHasPasswordMismatch() {
        UserUpdateRequestDto request = new UserUpdateRequestDto();
        request.setEmail("updated@example.com");
        request.setCurrentPassword(userPlainPassword);
        request.setNewPassword("newPassword123");
        request.setConfirmationPassword("mismatchPassword123");

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(request)
                .when()
                .put("/api/v1/users/me")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void shouldChangePassword_WhenCurrentPasswordIsCorrect() {
        String newPassword = "newSecretPassword123";
        ChangePasswordRequestDto request = new ChangePasswordRequestDto();
        request.setCurrentPassword(userPlainPassword);
        request.setNewPassword(newPassword);
        request.setConfirmationPassword(newPassword);

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(request)
                .when()
                .patch("/api/v1/users/me/password")
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, newPassword)
                .when()
                .get("/api/v1/users/me")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void shouldReturnUnauthorized_WhenChangingPasswordWithWrongCurrent() {
        ChangePasswordRequestDto request = new ChangePasswordRequestDto();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("newSecretPassword123");
        request.setConfirmationPassword("newSecretPassword123");

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(request)
                .when()
                .patch("/api/v1/users/me/password")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void shouldChangeEmail_WhenEmailIsAvailable() {
        ChangeEmailRequestDto request = new ChangeEmailRequestDto();
        request.setNewEmail("newemail@example.com");

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(request)
                .when()
                .patch("/api/v1/users/me/email")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("email", equalTo("newemail@example.com"));

        User updatedUser = userRepository.findById(currentUser.getId()).orElseThrow();
        Assertions.assertEquals("newemail@example.com", updatedUser.getEmail());
    }

    @Test
    void shouldReturnConflict_WhenChangingEmailToExistingOne() {
        User otherUser = User.builder()
                .email("other@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userRepository.save(otherUser);

        ChangeEmailRequestDto request = new ChangeEmailRequestDto();
        request.setNewEmail("other@example.com");

        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .body(request)
                .when()
                .patch("/api/v1/users/me/email")
                .then()
                .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    void shouldDeleteAccount_WhenAuthenticated() {
        given()
                .contentType(ContentType.JSON)
                .auth().basic(userEmail, userPlainPassword)
                .when()
                .delete("/api/v1/users/me")
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        boolean exists = userRepository.existsById(currentUser.getId());
        Assertions.assertFalse(exists);
    }
}
