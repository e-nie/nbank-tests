package iteration2;

import generators.RandomData;
import io.restassured.specification.RequestSpecification;
import models.ChangeProfileRequest;
import models.CreateUserRequest;
import models.UserRole;
import org.junit.jupiter.api.Test;
import requests.AdminCreateUserRequester;
import requests.ChangeProfileRequester;
import requests.GetProfileRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class ProfileTest {

    @Test
    public void userCanUpdateProfileNameTest() {

        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())//
                .createUser(userRequest);


        RequestSpecification userSpec = RequestSpecs.authAsUser(
                userRequest.getUsername(),
                userRequest.getPassword()
        );

        new GetProfileRequester(userSpec, ResponseSpecs.requestReturnsOK())
                .getProfile()
                .body("name", nullValue());//check the initial field state

        String newUserName = "Mdova Korneeva";

        ChangeProfileRequest changeProfileRequest = ChangeProfileRequest.builder()
                .name(newUserName)
                .build();

        new ChangeProfileRequester(userSpec, ResponseSpecs.requestReturnsOK())
                .changeProfile(changeProfileRequest);

        new GetProfileRequester(userSpec, ResponseSpecs.requestReturnsOK())
                .getProfile()
                .body("name", equalTo(newUserName));
    }


    @Test
    public void userCannotUpdateProfileWithInvalidNameTest() {
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .createUser(userRequest);


        RequestSpecification userSpec = RequestSpecs.authAsUser(
                userRequest.getUsername(),
                userRequest.getPassword()
        );

        new GetProfileRequester(userSpec, ResponseSpecs.requestReturnsOK())
                .getProfile()
                .body("name", nullValue());//check the initial field state


        String invalidName = "Evchen_" + UUID.randomUUID().toString().substring(0, 8);//invalid format for profile name

        //create model
        ChangeProfileRequest changeProfileRequest = ChangeProfileRequest.builder()
                .name(invalidName)
                .build();


        new ChangeProfileRequester(userSpec, ResponseSpecs.requestReturnsBadRequestWithText("Name must contain two words with letters only"))
                .changeProfile(changeProfileRequest);


        new GetProfileRequester(userSpec, ResponseSpecs.requestReturnsOK())
                .getProfile()
                .body("name", nullValue());
    }
}
