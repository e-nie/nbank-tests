package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import static io.restassured.RestAssured.given;

public class GetProfileRequester extends Request{
    public GetProfileRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    public ValidatableResponse getProfile(){
        return given()
                .spec(requestSpecification)
                .get("/customer/profile")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }
}
