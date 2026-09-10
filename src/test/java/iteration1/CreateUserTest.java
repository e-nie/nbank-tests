package iteration1;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class CreateUserTest {
    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.filters(
                List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()));
    }

    @Test
    public void adminCanCreateUserWithValidData() {
        //Создание пользователя
        String username = "Evchen_" + UUID.randomUUID()
                .toString()
                .substring(0, 8);
        String password = "Irreversible!10";

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                          "username": "%s",
                          "password": "%s",
                          "role": "USER"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .body("username", equalTo(username))
                .body("password", not(equalTo(password)))
                .body("role", equalTo("USER"))
                .body("id", notNullValue());

    }


    public static Stream<Arguments> userInvalidData() {
        return Stream.of(
                //username field validation

                Arguments.of(" ", "Password!01", "USER", "username", "Username cannot be blank"),
                Arguments.of("yo", "Password!01", "USER", "username", "Username must be between 3 and 15 characters"),
                Arguments.of("yo9-_.$", "Password!01", "USER", "username", "Username must contain only letters, digits, dashes, underscores, and dots")





        );
    }

    @MethodSource("userInvalidData")
    @ParameterizedTest
    public void adminCannotCreateUserWithInvalidData(String username, String password, String role, String errorKey,  String errorValue) {
        String requestBody = String.format(
                """
                         {
                               "username": "%s",
                               "password": "%s",
                               "role": "%s"
                             }
                        """, username, password, role
        );

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body(requestBody)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(errorKey, equalTo(errorValue));

    }

}
