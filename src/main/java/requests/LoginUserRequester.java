package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.LoginUserRequest;

import static io.restassured.RestAssured.given;

public class LoginUserRequester extends Request{

    public LoginUserRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }


    public ValidatableResponse loginUser(LoginUserRequest model) {
        return given()
                .spec(requestSpecification)
                //Параметр 2: Тело запроса
                .body(model)
                .post("/auth/login")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }
}
