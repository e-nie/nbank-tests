package iteration2;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class TransferTest {
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

    //returns account id
    public static int createAccount(String userAuthorization) {
        //создаем счет
        return given().header("Authorization", userAuthorization)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");
    }

    public static void makeDeposit(String userAuthorization, int accountId, double amount) {

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
                .statusCode(HttpStatus.SC_OK);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.01, 9999.99, 10000.00})
    public void userCanTransferBetweenOwnAccountsTest(double amount) {
        String username = "Evchen_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "Irreversible!10";

        createUser(username, password);
        String userAuthorization = loginUser(username, password);

        int sourceAccountId = createAccount(userAuthorization);
        int destinationAccountId = createAccount(userAuthorization);

        makeDeposit(userAuthorization, sourceAccountId, 5000);
        makeDeposit(userAuthorization, sourceAccountId, 5000);
        makeDeposit(userAuthorization, sourceAccountId, 100);

        given()
                .header("Authorization", userAuthorization)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                                {
                                  "senderAccountId": %s,
                                  "receiverAccountId":%s,
                                  "amount": %s
                                }
                        """.formatted(sourceAccountId, destinationAccountId, amount))
                .post("/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("message", equalTo("Transfer successful"))
                .body("senderAccountId", equalTo(sourceAccountId))
                .body("receiverAccountId", equalTo(destinationAccountId))
                .body("amount", equalTo((float) amount));
    }

    @Test
    public void userCanMakeTransferToAnotherUsersAccountTest() {
        String username1 = "Evchen_" + UUID.randomUUID().toString().substring(0, 8);
        String username2 = "Adam" + UUID.randomUUID().toString().substring(0, 8);
        String password = "Irreversible!10";

        double amount = 1000;

        createUser(username1, password);
        createUser(username2, password);

        String userAuthorization1 = loginUser(username1, password);
        String userAuthorization2 = loginUser(username2, password);

        int senderAccountId = createAccount(userAuthorization1);
        int receiverAccountId = createAccount(userAuthorization2);

        makeDeposit(userAuthorization1, senderAccountId, amount);

        given()
                .header("Authorization", userAuthorization1)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                                {
                                  "senderAccountId": %s,
                                  "receiverAccountId":%s,
                                  "amount": %s
                                }
                        """.formatted(senderAccountId, receiverAccountId, amount))
                .post("/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("message", equalTo("Transfer successful"))
                .body("senderAccountId", equalTo(senderAccountId))
                .body("receiverAccountId", equalTo(receiverAccountId))
                .body("amount", equalTo((float) amount));
    }

    @Test
    public void userCannotTransferAboveMaximumAmountTest() {
        String username = "Evchen_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "Irreversible!10";

        createUser(username, password);
        String userAuthorization = loginUser(username, password);

        int sourceAccountId = createAccount(userAuthorization);
        int destinationAccountId = createAccount(userAuthorization);

        makeDeposit(userAuthorization, sourceAccountId, 5000);
        makeDeposit(userAuthorization, sourceAccountId, 5000);
        makeDeposit(userAuthorization, sourceAccountId, 100);

        double transferAmount = 10000.01;

        given()
                .header("Authorization", userAuthorization)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                                {
                                  "senderAccountId": %s,
                                  "receiverAccountId":%s,
                                  "amount": %s
                                }
                        """.formatted(sourceAccountId, destinationAccountId, transferAmount))
                .post("/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(equalTo("Transfer amount cannot exceed 10000"));
    }

    @Test
    public void userCannotTransferNegativeAmountTest() {
        String username = "Evchen_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "Irreversible!10";

        createUser(username, password);
        String userAuthorization = loginUser(username, password);

        int sourceAccountId = createAccount(userAuthorization);
        int destinationAccountId = createAccount(userAuthorization);

        makeDeposit(userAuthorization, sourceAccountId, 100);

        double transferAmount = -100;

        given()
                .header("Authorization", userAuthorization)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                                {
                                  "senderAccountId": %s,
                                  "receiverAccountId":%s,
                                  "amount": %s
                                }
                        """.formatted(sourceAccountId, destinationAccountId, transferAmount))
                .post("/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(equalTo("Transfer amount must be at least 0.01"));
    }


    @Test
    public void userCannotTransferMoreThanAvailableBalanceTest() {
        String username = "Evchen_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "Irreversible!10";

        createUser(username, password);
        String userAuthorization = loginUser(username, password);

        int sourceAccountId = createAccount(userAuthorization);
        int destinationAccountId = createAccount(userAuthorization);


        makeDeposit(userAuthorization, sourceAccountId, 100);
        double transferAmount = 200;
        given()
                .header("Authorization", userAuthorization)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                                {
                                  "senderAccountId": %s,
                                  "receiverAccountId":%s,
                                  "amount": %s
                                }
                        """.formatted(sourceAccountId, destinationAccountId, transferAmount))
                .post("/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(equalTo("Invalid transfer: insufficient funds or invalid accounts"));
    }
}



