package com.literandltx.timer;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.literandltx.timer.dto.user.UserLoginRequestDto;
import com.literandltx.timer.model.Role;
import com.literandltx.timer.model.RoleName;
import com.literandltx.timer.model.User;
import com.literandltx.timer.repository.RoleRepository;
import com.literandltx.timer.repository.UserRepository;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UserControllerIT extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String userEmail = "delete_me@email.com";
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
    void shouldSoftDeleteUser_WhenAuthorizedUserRequestsDeletion() {
        // 1. Arrange
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

        // 2. Act
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/api/v1/users/me")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NO_CONTENT.value());

        // 3. Assert
        Boolean isDeleted = jdbcTemplate.queryForObject(
                "SELECT is_deleted FROM users WHERE email = ?",
                Boolean.class,
                userEmail
        );

        assertThat(isDeleted).isTrue();
    }

    @Test
    void shouldReturnUnauthorized_WhenUnauthenticatedUserAttemptsDeletion() {
        // 1. Arrange

        // 2. Act
        Response response = given()
                .when()
                .delete("/api/v1/users/me");

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

}
