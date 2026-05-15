package com.example.timer_backend.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.rate-limit.enabled=true"
)
@Disabled("Temporary disabled to fix context pollution")
class RateLimitingFilterIT extends BaseIntegrationTest {

    @Test
    void shouldReturnTooManyRequests_WhenRateLimitIsExceeded() throws InterruptedException {
        String protectedUrl = "/api/v1/labels";
        int capacity = 10;

        for (int i = 0; i < capacity; i++) {
            given()
                    .get(protectedUrl)
                    .then()
                    .statusCode(not(HttpStatus.TOO_MANY_REQUESTS.value()));
        }

        given()
                .get(protectedUrl)
                .then()
                .statusCode(HttpStatus.TOO_MANY_REQUESTS.value())
                .body(equalTo("Too Many Requests - Rate limit exceeded"));
    }

    @Test
    void shouldRecover_AfterWaitingForRefill() throws InterruptedException {
        String url = "/api/v1/labels";

        for (int i = 0; i < 10; i++) {
            given().get(url);
        }

        given().get(url).then().statusCode(HttpStatus.TOO_MANY_REQUESTS.value());

        Thread.sleep(2000);

        given()
                .contentType(ContentType.JSON)
                .when()
                .get(url)
                .then()
                .statusCode(not(HttpStatus.TOO_MANY_REQUESTS.value()));
    }

    private org.hamcrest.Matcher<Integer> not1(int code) {
        return org.hamcrest.Matchers.not(code);
    }
}
