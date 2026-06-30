package com.literandltx.timer.it;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.literandltx.timer.dto.user.UserLoginRequestDto;
import com.literandltx.timer.model.Role;
import com.literandltx.timer.model.RoleName;
import com.literandltx.timer.model.User;
import com.literandltx.timer.repository.RoleRepository;
import com.literandltx.timer.repository.UserRepository;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

public class AdminControllerIT extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminAuthToken;
    private User adminUser;
    private Role userRole;

    @BeforeEach
    void setUpAdminUser() {
        super.setUp();

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role not found in test DB"));
        
        userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException("USER role not found in test DB"));

        adminUser = User.builder()
                .email("admin@example.com")
                .password(passwordEncoder.encode("adminpass"))
                .roles(Set.of(adminRole))
                .isEnabled(true)
                .build();
        
        userRepository.save(adminUser);

        UserLoginRequestDto loginRequest = new UserLoginRequestDto();
        loginRequest.setUsername("admin@example.com");
        loginRequest.setPassword("adminpass");

        adminAuthToken = given()
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
        jdbcTemplate.execute("DELETE FROM users_roles");
        jdbcTemplate.execute("DELETE FROM refresh_tokens");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    void shouldEnableSpecificUser_WhenAdminIsAuthenticated() {
        // 1. Arrange
        User targetUser = User.builder()
                .email("target@example.com")
                .password(passwordEncoder.encode("password"))
                .roles(Set.of(userRole))
                .isEnabled(false)
                .build();
        targetUser = userRepository.save(targetUser);

        // 2. Act
        Response response = given()
                .header("Authorization", "Bearer " + adminAuthToken)
                .when()
                .patch("/api/v1/admin/users/{id}/enable", targetUser.getId());

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NO_CONTENT.value());

        User updatedUser = userRepository.findById(targetUser.getId()).orElseThrow();
        assertTrue(updatedUser.isEnabled(), "User should be enabled");
    }

    @Test
    void shouldDisableSpecificUser_WhenAdminIsAuthenticated() {
        // 1. Arrange
        User targetUser = User.builder()
                .email("target@example.com")
                .password(passwordEncoder.encode("password"))
                .roles(Set.of(userRole))
                .isEnabled(true)
                .build();
        targetUser = userRepository.save(targetUser);

        // 2. Act
        Response response = given()
                .header("Authorization", "Bearer " + adminAuthToken)
                .when()
                .patch("/api/v1/admin/users/{id}/disable", targetUser.getId());

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NO_CONTENT.value());

        User updatedUser = userRepository.findById(targetUser.getId()).orElseThrow();
        assertFalse(updatedUser.isEnabled(), "User should be disabled");
    }

    @Test
    void shouldEnableAllUsers_ExceptAdmins() {
        // 1. Arrange
        User normalUser1 = User.builder()
                .email("user1@example.com")
                .password("pass")
                .roles(Set.of(userRole))
                .isEnabled(false)
                .build();
                
        User normalUser2 = User.builder()
                .email("user2@example.com")
                .password("pass")
                .roles(Set.of(userRole))
                .isEnabled(false)
                .build();
                
        userRepository.saveAll(List.of(normalUser1, normalUser2));

        // 2. Act
        Response response = given()
                .header("Authorization", "Bearer " + adminAuthToken)
                .when()
                .patch("/api/v1/admin/users/enable");

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NO_CONTENT.value());

        User updatedUser1 = userRepository.findById(normalUser1.getId()).orElseThrow();
        User updatedUser2 = userRepository.findById(normalUser2.getId()).orElseThrow();
        
        assertTrue(updatedUser1.isEnabled(), "User 1 should be enabled");
        assertTrue(updatedUser2.isEnabled(), "User 2 should be enabled");
    }

    @Test
    void shouldDisableAllUsers_ExceptAdmins() {
        // 1. Arrange
        User normalUser1 = User.builder()
                .email("user1@example.com")
                .password("pass")
                .roles(Set.of(userRole))
                .isEnabled(true)
                .build();
                
        User normalUser2 = User.builder()
                .email("user2@example.com")
                .password("pass")
                .roles(Set.of(userRole))
                .isEnabled(true)
                .build();
                
        userRepository.saveAll(List.of(normalUser1, normalUser2));

        // 2. Act
        Response response = given()
                .header("Authorization", "Bearer " + adminAuthToken)
                .when()
                .patch("/api/v1/admin/users/disable");

        // 3. Assert
        response.then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NO_CONTENT.value());

        User updatedUser1 = userRepository.findById(normalUser1.getId()).orElseThrow();
        User updatedUser2 = userRepository.findById(normalUser2.getId()).orElseThrow();
        User checkAdmin = userRepository.findById(adminUser.getId()).orElseThrow();
        
        assertFalse(updatedUser1.isEnabled(), "User 1 should be disabled");
        assertFalse(updatedUser2.isEnabled(), "User 2 should be disabled");
        assertTrue(checkAdmin.isEnabled(), "Admin should remain enabled despite global disable");
    }
}
