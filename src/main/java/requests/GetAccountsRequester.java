package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import static io.restassured.RestAssured.given;

public class GetAccountsRequester extends Request {
    public GetAccountsRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    public ValidatableResponse getAccounts() {
        return given()
                .spec(requestSpecification)
                .get("/customer/accounts")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }
}
