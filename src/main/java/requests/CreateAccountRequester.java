package requests;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.BaseModel;
import org.apache.http.HttpStatus;

import static io.restassured.RestAssured.given;

public class CreateAccountRequester extends Request {

    public CreateAccountRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }


    public ValidatableResponse createAccount() {
        return given()
                .spec(requestSpecification)
                .post("/accounts")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }
}
