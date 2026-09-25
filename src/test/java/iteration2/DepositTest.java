package iteration2;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

public class DepositTest {
    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.filters(
                List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()));
    }

public  static String loginUser(String username, String password){
   return given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("""
                        {
                        "username": "%s",
                        "password":"%s"
                        }
                        """.formatted(username, password))
            .post("http://localhost:4111/api/v1/auth/login")
            .header("Authorization");

}



}
