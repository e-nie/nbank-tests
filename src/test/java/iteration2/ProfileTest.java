package iteration2;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class ProfileTest {
    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.baseURI = "http://localhost:4111";
        RestAssured.basePath = "/api/v1";

        RestAssured.filters(List.of(new RequestLoggingFilter(), new ResponseLoggingFilter()));
    }

    public static void createUser(String username, String password) {
        given().contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                          "username": "%s",
                          "password": "%s",
                          "role": "USER"
                        }
                        """.formatted(username, password))
                .post("/admin/users")
                .then().assertThat()
                .statusCode(HttpStatus.SC_CREATED);
    }

    //return auth token
    public static String loginUser(String username, String password) {
        return given().contentType(ContentType.JSON).accept(ContentType.JSON).body("""
                {
                "username": "%s",
                "password":"%s"
                }
                """.formatted(username, password)).post("/auth/login").header("Authorization");
    }

    @Test
    public void userCanUpdateProfilenameTest() {
        String username = "Evchen_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "Irreversible!10";

        createUser(username, password);
        String userAuthorization = loginUser(username, password);

        given()
                .header("Authorization", userAuthorization)
                .accept(ContentType.JSON)
                .get("/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("name", nullValue());

        String newUserName = "Mdova Korneeva";

        given()
                .header("Authorization", userAuthorization)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                                {
                                 "name": "%s"
                                }
                        """.formatted(newUserName))
                .put("/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("message", equalTo("Profile updated successfully"))
                .body("customer.name", equalTo(newUserName));
    }


    @Test
    public void userCannotUpdateProfileWithInvalidNameTest() {
        String username = "Evchen_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "Irreversible!10";

        createUser(username, password);
        String userAuthorization = loginUser(username, password);

        given()
                .header("Authorization", userAuthorization)
                .accept(ContentType.JSON)
                .get("/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("name", nullValue());


        String newUserName = "Evchen_" + UUID.randomUUID().toString().substring(0, 8);//invalid format for profile name

        given()
                .header("Authorization", userAuthorization)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                                {
                                 "name": "%s"
                                }
                        """.formatted(newUserName))
                .put("/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(equalTo("Name must contain two words with letters only"));
    }
}
