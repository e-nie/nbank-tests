package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.BaseModel;

import static io.restassured.RestAssured.given;

public class ChangeProfileRequester extends Request{
    public ChangeProfileRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    public ValidatableResponse changeProfile(BaseModel model){
        return given()
                .spec(requestSpecification)
                .body(model)
                .put("/customer/profile")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }
}
