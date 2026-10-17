package iteration2;

import enums.AlertMessage;
import generators.RandomData;
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
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

        GetProfileResponse profile = new GetProfileRequester(userSpec, ResponseSpecs.requestReturnsOK())
                .getProfile()
                .extract()
                .as(GetProfileResponse.class);// profile object

        assertNull(profile.getName());


        String newUserName = TestDataGenerator.generateProfileName();
        ChangeProfileRequest changeProfileRequest = ChangeProfileRequest.builder()
                .name(newUserName)
                .build();

        ValidatableResponse responseAfterUpdate = new ChangeProfileRequester(userSpec, ResponseSpecs.requestReturnsOK())
                .changeProfile(changeProfileRequest);

        GetProfileResponse updatedProfile = new GetProfileRequester(userSpec, ResponseSpecs.requestReturnsOK())
                .getProfile()
                .extract()
                .as(GetProfileResponse.class);

        assertEquals(newUserName, updatedProfile.getName());
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

        GetProfileResponse profileBeforeUpdate = new GetProfileRequester(userSpec, ResponseSpecs.requestReturnsOK())
                .getProfile()
                .extract()
                .as(GetProfileResponse.class);

        assertNull(profileBeforeUpdate.getName());//check the initial field state

        String invalidName = TestDataGenerator.generateInvalidProfileName();//invalid format for profile name

        //create model
        ChangeProfileRequest changeProfileRequest = ChangeProfileRequest.builder()
                .name(invalidName)
                .build();


        new ChangeProfileRequester(userSpec, ResponseSpecs.requestReturnsBadRequestWithText(AlertMessage.INVALID_PROFILE_NAME.getMessage()))
                .changeProfile(changeProfileRequest);


        GetProfileResponse profileAfterUpdate = new GetProfileRequester(userSpec, ResponseSpecs.requestReturnsOK())
                .getProfile()
                .extract()
                .as(GetProfileResponse.class);

        assertNull(profileAfterUpdate.getName());
    }
}
