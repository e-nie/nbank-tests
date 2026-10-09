package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.BaseModel;
import static io.restassured.RestAssured.given;

public class MakeDepositRequester extends Request {
    public MakeDepositRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    //делаем депозит
    public ValidatableResponse makeDeposit(BaseModel model) {
        return given()
                .spec(requestSpecification)
                .body(model)
                .post("/accounts/deposit")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }
}
