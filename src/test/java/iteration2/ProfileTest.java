package iteration2;

import enums.AlertMessage;
import generators.RandomData;
import generators.RandomModelGenerator;
import generators.TestDataGenerator;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import models.ChangeProfileRequest;
import models.CreateUserRequest;
import models.GetProfileResponse;
import models.UserRole;
import org.junit.jupiter.api.Test;
import requests.AdminCreateUserRequester;
import requests.ChangeProfileRequester;
import requests.GetProfileRequester;
import requests.skeleton.Endpoint;
import requests.skeleton.requesters.CrudRequester;
import requests.steps.AdminSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.Random;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ProfileTest {

    @Test
    public void userCanUpdateProfileNameTest() {

        CreateUserRequest userRequest = AdminSteps.createUser();

        GetProfileResponse profile = new CrudRequester(RequestSpecs.authAsUser(
                userRequest.getUsername(),
                userRequest.getPassword()),
                Endpoint.PROFILE,
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(GetProfileResponse.class);// profile object

        assertNull(profile.getName());

        ChangeProfileRequest changeProfileRequest =
                RandomModelGenerator.generate(ChangeProfileRequest.class);

        String newUserName = changeProfileRequest.getName();

        new CrudRequester(
                RequestSpecs.authAsUser(
                        userRequest.getUsername(),
                        userRequest.getPassword()),
                Endpoint.PROFILE,
                ResponseSpecs.requestReturnsOK())
                .update(changeProfileRequest);

        GetProfileResponse updatedProfile = new CrudRequester(
                RequestSpecs.authAsUser(
                        userRequest.getUsername(),
                        userRequest.getPassword()),
                Endpoint.PROFILE,
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(GetProfileResponse.class);

        assertEquals(newUserName, updatedProfile.getName());
    }

    @Test
    public void userCannotUpdateProfileWithInvalidNameTest() {
        CreateUserRequest userRequest = AdminSteps.createUser();

        GetProfileResponse profileBeforeUpdate = new CrudRequester(
                RequestSpecs.authAsUser(
                        userRequest.getUsername(),
                        userRequest.getPassword()),
                Endpoint.PROFILE,
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(GetProfileResponse.class);

        assertNull(profileBeforeUpdate.getName());//check the initial field state


        String invalidName = TestDataGenerator.generateInvalidProfileName();//invalid format for profile name

        //create model
        ChangeProfileRequest changeProfileRequest = ChangeProfileRequest.builder()
                .name(invalidName)
                .build();

        new CrudRequester(
                RequestSpecs.authAsUser(
                        userRequest.getUsername(),
                        userRequest.getPassword()),
                Endpoint.PROFILE,
                ResponseSpecs.requestReturnsBadRequestWithText(AlertMessage.INVALID_PROFILE_NAME.getMessage()))
                .update(changeProfileRequest);


        GetProfileResponse profileAfterUpdate = new CrudRequester(
                RequestSpecs.authAsUser(
                        userRequest.getUsername(),
                        userRequest.getPassword()),
                Endpoint.PROFILE,
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(GetProfileResponse.class);

        assertNull(profileAfterUpdate.getName());
    }
}
