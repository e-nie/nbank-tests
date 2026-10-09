package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.CreateUserRequest;

import static io.restassured.RestAssured.given;

public class AdminCreateUserRequester extends Request{
    public AdminCreateUserRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }


    public ValidatableResponse createUser(CreateUserRequest model) {
        return given()
                .spec(requestSpecification)
                .body(model)
                .post("/admin/users")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }
}

