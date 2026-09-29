package iteration2;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class DepositTest {
    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.baseURI = "http://localhost:4111";
        RestAssured.basePath = "/api/v1";

        RestAssured.filters(List.of(new RequestLoggingFilter(), new ResponseLoggingFilter()));
    }

    public static void createUser(String username, String password) {
        given().contentType(ContentType.JSON).accept(ContentType.JSON).header("Authorization", "Basic YWRtaW46YWRtaW4=").body("""
                {
                  "username": "%s",
                  "password": "%s",
                  "role": "USER"
                }
                """.formatted(username, password)).post("/admin/users").then().assertThat().statusCode(HttpStatus.SC_CREATED);
    }

    public static String loginUser(String username, String password) {
        return given().contentType(ContentType.JSON).accept(ContentType.JSON).body("""
                {
                "username": "%s",
                "password":"%s"
                }
                """.formatted(username, password)).post("/auth/login").header("Authorization");
    }

    public static int createAccount(String userAuthorization) {
        //создаем счет
        given().header("Authorization", userAuthorization).contentType(ContentType.JSON).accept(ContentType.JSON).post("/accounts").then().assertThat().statusCode(HttpStatus.SC_CREATED);
        //проверяем что счет создался
        return given().header("Authorization", userAuthorization).contentType(ContentType.JSON).accept(ContentType.JSON).get("/customer/accounts").then().assertThat().statusCode(HttpStatus.SC_OK).body("$", not(empty())).extract().path("[0].id");

    }

    @ParameterizedTest
    @ValueSource(doubles = {5000, 4999.99, 0.01})
    public void userCanMakeDepositWithValidAmountTest(double amount) {
// подготавливаем тестовые данные и вызываем хелперы
        String username = "Evchen_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "Irreversible!10";

        createUser(username, password);//создаем юзера
        String userAuthorization = loginUser(username, password);//получаем токен

        int accountId = createAccount(userAuthorization);//создаем счет и присваиваем результат метода в переменную

        //делаем депозит
        given().header("Authorization", userAuthorization).contentType(ContentType.JSON).accept(ContentType.JSON).body("""
                        {
                          "id": %s,
                          "balance": %s
                                           }
                        """.formatted(accountId, amount))
                .post("/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);
    }


    @Test

    public void userCannotMakeDepositWithNegativeAmountTest() {
        double amount = -200;
        String username = "Evchen_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "Irreversible!10";

        createUser(username, password);//создаем юзера
        String userAuthorization = loginUser(username, password);//получаем токен

        int accountId = createAccount(userAuthorization);//создаем счет и присваиваем результат метода в переменную

        //делаем депозит
        given().header("Authorization", userAuthorization)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "id": %s,
                          "balance": %s
                                           }
                        """.formatted(accountId, amount))
                .post("/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(equalTo("Deposit amount must be at least 0.01"));
    }


    @Test
    public void userCannotMakeDepositAboveMaximumAmountTest() {
        double amount = 5000.01;
        String username = "Evchen_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "Irreversible!10";

        createUser(username, password);//создаем юзера
        String userAuthorization = loginUser(username, password);//получаем токен

        int accountId = createAccount(userAuthorization);//создаем счет и присваиваем результат метода в переменную

        //делаем депозит
        given().header("Authorization", userAuthorization)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "id": %s,
                          "balance": %s
                                           }
                        """.formatted(accountId, amount))
                .post("/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(equalTo("Deposit amount cannot exceed 5000"));
    }

    @Test
    public void userCannotMakeDepositToAnotherUsersAccountTest() {
        String password = "Irreversible!10";
        String username1 = "Evchen_" + UUID.randomUUID().toString().substring(0, 8);
        String username2 = "Evchen_" + UUID.randomUUID().toString().substring(0, 8);

        createUser(username1, password);
        createUser(username2, password);

        String authorization1 = loginUser(username1, password);
        String authorization2 = loginUser(username2, password);

        //int accountId1 = createAccount(authorization1);  - we don't need our account number
        int accountId2 = createAccount(authorization2);

        given().header("Authorization", authorization1).contentType(ContentType.JSON).accept(ContentType.JSON).body("""
                        {
                          "id": %s,
                          "balance":100
                                           }
                        """.formatted(accountId2))
                .post("/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body(equalTo("Unauthorized access to account"));
    }

    @Test
    public void userCannotMakeDepositToNonExistingAccountTest() {
        String password = "Irreversible!10";
        String username = "Evchen_" + UUID.randomUUID().toString().substring(0, 8);

        createUser(username, password);//создаем юзера
        String userAuthorization = loginUser(username, password);//получаем токен

        int nonExistingAccountId = 999999;

        given()
                .header("Authorization", userAuthorization)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "id": %s,
                          "balance": 100
                                           }
                        """.formatted(nonExistingAccountId))
                .post("/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body(equalTo("Unauthorized access to account"));


    }


}
