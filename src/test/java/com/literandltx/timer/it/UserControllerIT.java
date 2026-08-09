package com.literandltx.timer.it;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.literandltx.timer.dto.user.UserChangeEmailRequestDto;
import com.literandltx.timer.dto.user.UserChangePasswordRequestDto;
import com.literandltx.timer.dto.user.login.UserLoginRequestDto;
import com.literandltx.timer.model.Label;
import com.literandltx.timer.model.Role;
import com.literandltx.timer.model.RoleName;
import com.literandltx.timer.model.TimerOption;
import com.literandltx.timer.model.TimerPreset;
import com.literandltx.timer.model.User;
import com.literandltx.timer.repository.LabelRepository;
import com.literandltx.timer.repository.RoleRepository;
import com.literandltx.timer.repository.TimerOptionRepository;
import com.literandltx.timer.repository.TimerPresetRepository;
import com.literandltx.timer.repository.UserRepository;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Set;
import java.util.UUID;
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
    private LabelRepository labelRepository;

    @Autowired
    private TimerOptionRepository timerOptionRepository;

    @Autowired
    private TimerPresetRepository timerPresetRepository;

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
        jdbcTemplate.execute("DELETE FROM timer_presets");
        jdbcTemplate.execute("DELETE FROM timer_options");
        jdbcTemplate.execute("DELETE FROM labels");
        jdbcTemplate.execute("DELETE FROM users_roles");
        jdbcTemplate.execute("DELETE FROM refresh_tokens");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    void shouldHardDeleteUser_WhenAuthorizedUserRequestsDeletion() {
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
        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?",
                Integer.class,
                userEmail
        );

        assertThat(userCount).isEqualTo(0);
    }

    @Test
    void shouldHardDeleteUser_WhenUserHasRelatedEntities() {
        // 1. Arrange
        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException("USER role not found"));

        User existingUser = new User();
        existingUser.setEmail("complex_delete@email.com");
        existingUser.setPassword(passwordEncoder.encode(userPlainPassword));
        existingUser.setRoles(Set.of(userRole));
        existingUser = userRepository.save(existingUser);

        Label label = new Label();
        label.setUuid(UUID.randomUUID());
        label.setName("Focus");
        label.setColor("#FF0000");
        label.setUser(existingUser);
        label = labelRepository.save(label);

        TimerOption timerOption = new TimerOption();
        timerOption.setUuid(UUID.randomUUID());
        timerOption.setValue(1500L);
        timerOption.setUser(existingUser);
        timerOption = timerOptionRepository.save(timerOption);

        TimerPreset timerPreset = new TimerPreset();
        timerPreset.setUuid(UUID.randomUUID());
        timerPreset.setLabel(label);
        timerPreset.setTimerOption(timerOption);
        timerPreset.setUser(existingUser);
        timerPreset.setLastUpdated(System.currentTimeMillis());
        timerPresetRepository.save(timerPreset);

        UserLoginRequestDto loginRequest = new UserLoginRequestDto();
        loginRequest.setUsername("complex_delete@email.com");
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
        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?",
                Integer.class,
                existingUser.getId()
        );
        assertThat(userCount).isEqualTo(0);

        Integer presetCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM timer_presets WHERE user_id = ?",
                Integer.class,
                existingUser.getId()
        );
        assertThat(presetCount).isEqualTo(0);

        Integer labelCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM labels WHERE user_id = ?",
                Integer.class,
                existingUser.getId()
        );
        assertThat(labelCount).isEqualTo(0);
        Integer timerOptionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM timer_options WHERE user_id = ?",
                Integer.class,
                existingUser.getId()
        );
        assertThat(timerOptionCount).isEqualTo(0);
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

    @Test
    void shouldChangeEmail_WhenValidRequestAndAuthorized() {
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

        UserChangeEmailRequestDto changeEmailRequest = new UserChangeEmailRequestDto();
        changeEmailRequest.setNewEmail("new_email@email.com");
        changeEmailRequest.setPassword(userPlainPassword);

        // 2. Act
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(changeEmailRequest)
                .when()
                .post("/api/v1/users/change/email")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NO_CONTENT.value());

        // 3. Assert
        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?",
                Integer.class,
                "new_email@email.com"
        );
        assertThat(userCount).isEqualTo(1);
    }

    @Test
    void shouldChangePassword_WhenValidRequestAndAuthorized() {
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

        UserChangePasswordRequestDto changePasswordRequest = new UserChangePasswordRequestDto();
        changePasswordRequest.setCurrentPassword(userPlainPassword);
        changePasswordRequest.setNewPassword("new_secure_password");
        changePasswordRequest.setConfirmationPassword("new_secure_password");

        // 2. Act
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(changePasswordRequest)
                .when()
                .post("/api/v1/users/change/password")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NO_CONTENT.value());

        // 3. Assert
        UserLoginRequestDto newLoginRequest = new UserLoginRequestDto();
        newLoginRequest.setUsername(userEmail);
        newLoginRequest.setPassword("new_secure_password");

        given()
                .contentType(ContentType.JSON)
                .body(newLoginRequest)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void shouldReturnBadRequest_WhenChangePasswordHasMismatchedConfirmation() {
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

        UserChangePasswordRequestDto changePasswordRequest = new UserChangePasswordRequestDto();
        changePasswordRequest.setCurrentPassword(userPlainPassword);
        changePasswordRequest.setNewPassword("new_secure_password");
        changePasswordRequest.setConfirmationPassword("different_password");

        // 2. Act & Assert
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(changePasswordRequest)
                .when()
                .post("/api/v1/users/change/password")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }
}
