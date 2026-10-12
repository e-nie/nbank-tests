package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.BaseModel;

import static io.restassured.RestAssured.given;

public class TransferRequester extends Request{
    public TransferRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    public ValidatableResponse makeTransfer(BaseModel model) {
        return given()
                .spec(requestSpecification)
                .body(model)
                .post("/accounts/transfer")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }
}
