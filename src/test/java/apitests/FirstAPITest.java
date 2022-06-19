package apitests;

import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;

public class FirstAPITest {
    @Test
    public void GetPosts() {
        given()
                .when()
                .get("http://training.skillo-bg.com:3100/posts?take=5&skip=0")
                .then()
                .log()
                .all();
    }
}
