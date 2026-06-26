package com.literandltx.timer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.oneOf;

import com.literandltx.timer.dto.user.UserLoginRequestDto;
import com.literandltx.timer.dto.user.UserRegistrationRequestDto;
import com.literandltx.timer.model.Role;
import com.literandltx.timer.model.RoleName;
import com.literandltx.timer.model.User;
import com.literandltx.timer.repository.RoleRepository;
import com.literandltx.timer.repository.UserRepository;
import io.restassured.http.ContentType;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

public class AuthenticationControllerIT extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String userEmail = "example@email.com";
    private final String userPlainPassword = "password";

    @BeforeEach
    void setUp() {
        super.setUp();
    }

    @AfterEach
    void tearDown() {
        super.tearDown();
        jdbcTemplate.execute("DELETE FROM users_roles");
        jdbcTemplate.execute("DELETE FROM refresh_tokens");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    void shouldRegisterUser_WhenRequestIsValid() {
        UserRegistrationRequestDto request = UserRegistrationRequestDto.builder()
                .email(userEmail)
                .password(userPlainPassword)
                .repeatPassword(userPlainPassword)
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.CREATED.value())
                .body("id", notNullValue())
                .body("email", equalTo(userEmail));
    }

    @Test
    void shouldLoginAndReturnToken_WhenCredentialsAreValid() {
        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException("USER role not found"));

        User existingUser = new User();
        existingUser.setEmail(userEmail);
        existingUser.setPassword(passwordEncoder.encode(userPlainPassword));
        existingUser.setRoles(Set.of(userRole));
        userRepository.save(existingUser);

        UserLoginRequestDto loginRequest = new UserLoginRequestDto();
        loginRequest.setUsername(userEmail);
        loginRequest.setPassword(userPlainPassword);

        given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .body("token", notNullValue())
                .body("token", not(emptyString()));
    }

    @Test
    void shouldReturnForbidden_WhenLoginFails() {
        User existingUser = new User();
        existingUser.setEmail(userEmail);
        existingUser.setPassword(passwordEncoder.encode(userPlainPassword));
        userRepository.save(existingUser);

        UserLoginRequestDto loginRequest = new UserLoginRequestDto();
        loginRequest.setUsername(userEmail);
        loginRequest.setPassword("WrongPassword123");

        given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .log().ifValidationFails()
                .statusCode(oneOf(HttpStatus.UNAUTHORIZED.value()));
    }

    @Test
    void shouldAccessProtectedResource_WhenTokenIsValid() {
        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException("USER role not found"));

        User existingUser = new User();
        existingUser.setEmail(userEmail);
        existingUser.setPassword(passwordEncoder.encode(userPlainPassword));
        existingUser.setRoles(Set.of(userRole));
        userRepository.save(existingUser);

        UserLoginRequestDto loginRequest = new UserLoginRequestDto();
        loginRequest.setUsername(userEmail);
        loginRequest.setPassword(userPlainPassword);

        String token = given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .path("token");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/labels")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void shouldReturnBadRequest_WhenPasswordsDoNotMatch() {
        UserRegistrationRequestDto request = UserRegistrationRequestDto.builder()
                .email(userEmail)
                .password("password123")
                .repeatPassword("password999")
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", containsString("Password and repeat password shouldn't be empty and should be equal"));
    }

    @Test
    void shouldReturnConflict_WhenUserAlreadyExists() {
        User existingUser = new User();
        existingUser.setEmail(userEmail);
        existingUser.setPassword(passwordEncoder.encode(userPlainPassword));
        userRepository.save(existingUser);

        UserRegistrationRequestDto request = UserRegistrationRequestDto.builder()
                .email(userEmail)
                .password("newpassword")
                .repeatPassword("newpassword")
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("message", equalTo("The email address '" + userEmail + "' is already in use."));
    }

    @Test
    void shouldReturnBadRequest_WhenEmailIsInvalid() {
        UserRegistrationRequestDto request = UserRegistrationRequestDto.builder()
                .email("not-a-valid-email-format")
                .password(userPlainPassword)
                .repeatPassword(userPlainPassword)
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", containsString("email"));
    }

    @Test
    void shouldReturnBadRequest_WhenPasswordIsTooShort() {
        UserRegistrationRequestDto request = UserRegistrationRequestDto.builder()
                .email(userEmail)
                .password("pass")
                .repeatPassword("pass")
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", containsString("password"))
                .body("message", containsString("size"));
    }

    @Test
    void shouldReturnUnauthorized_WhenUserDoesNotExist() {
        String nonExistentEmail = "ghost@example.com";
        String anyPassword = "password";

        given()
                .contentType(ContentType.JSON)
                .auth().basic(nonExistentEmail, anyPassword)
                .when()
                .get("/api/v1/labels")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
