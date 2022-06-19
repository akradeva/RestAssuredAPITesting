package apitests;

import com.jayway.jsonpath.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class APITests {

    static String loginToken;
    static Integer userId;
    static String username;
    static Integer latestPostId;
    static String commentId;

    @BeforeTest
    public void loginTest() throws IOException {
        LoginPOJO login = new LoginPOJO();

        FileReader reader = new FileReader("credentials.properties");
        Properties properties = new Properties();
        properties.load(reader);

        login.setUsernameOrEmail(properties.getProperty("user"));
        login.setPassword(properties.getProperty("password"));

        //DEPRECATED: update using only POJO object
//        convert the pojo object to json
//        ObjectMapper objectMapper = new ObjectMapper();
//        String convertedJson = objectMapper.writeValueAsString(login);

        baseURI = "http://training.skillo-bg.com:3100";

        Response response = given()
                .header("Content-Type", "application/json")
                .body(login)
                .when()
                .post("/users/login");

        response
                .then()
                .assertThat().statusCode(201);

        String loginResponseBody = response.body().asString();
        loginToken = JsonPath.parse(loginResponseBody).read("$.token");
        userId = JsonPath.parse(loginResponseBody).read("$.user.id");
        username = JsonPath.parse(loginResponseBody).read("$.user.username");

        //check the token
        System.out.println("The token is: ");
        System.out.println(loginToken);
        System.out.println(username);
    }

    @Test()
    public void getAllPost() {
        ValidatableResponse validatableResponse = given()
                .header("Content-Type", "application/json")
                .queryParam("take", 5)
                .queryParam("skip", 0)
                .when()
                .get("/posts")
                .then()
                .assertThat().statusCode(200)
                .log()
                .all();

        ArrayList<Integer> postIds = validatableResponse.extract().path("id");
        latestPostId = postIds.get(0);

        //check the id of the latest post
        System.out.println(latestPostId);
    }

    @Test
    public void likePost() {
        ActionsPOJO likePost = new ActionsPOJO();
        likePost.setAction("likePost");
        getAllPost();

        ValidatableResponse validatableResponse = given()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + loginToken)
                .body(likePost)
                .when()
                .patch("/posts/" + latestPostId)
                .then()
                .assertThat().body("post.id", equalTo(latestPostId))
                .assertThat().body("user.id", equalTo(userId))
                .log()
                .all()
                .statusCode(200);

        System.out.println("Extracted response: ");
        System.out.println((String) validatableResponse.extract().path("post.user.username"));
        System.out.println((String) validatableResponse.extract().path("user.username"));
    }

    @Test()
    public void commentPost() {
        ActionsPOJO commentPost = new ActionsPOJO();
        commentPost.setContent("test comment");
        getAllPost();

        Response response = given()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + loginToken)
                .body(commentPost)
                .when()
                .post("/posts/" + latestPostId + "/comment");
        response
                .then()
                .assertThat().body("user.id", equalTo(userId))
                .log()
                .all()
                .statusCode(201);

        String loginResponseBody = response.body().asString();
        commentId = JsonPath.parse(loginResponseBody).read("$.id").toString();
        System.out.println(commentId);
    }

    @Test()
    public void getCommentsForPost() {
        getAllPost();

         given()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .get("/posts/" + latestPostId + "/comments")
                .then()
                .log()
                .all()
                .statusCode(200);
    }

    @Test()
    public void deleteComment() {
        commentPost();
        given()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .delete("/posts/" + latestPostId + "/comments/" + commentId)
                .then()
                .assertThat().body("user.id", equalTo(userId))
                .log()
                .all()
                .statusCode(200);
    }
}
