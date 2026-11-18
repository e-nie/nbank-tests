package iteration2.api;

import api.enums.AlertMessage;
import api.generators.RandomModelGenerator;
import api.generators.TestDataGenerator;
import api.models.ChangeProfileRequest;
import api.models.CreateUserRequest;
import api.models.GetProfileResponse;
import api.models.comparison.ModelAssertions;
import org.junit.jupiter.api.Test;
import api.steps.AdminSteps;
import api.steps.UserSteps;
import api.specs.ResponseSpecs;

import static org.junit.jupiter.api.Assertions.assertNull;

public class ProfileTest {

    @Test
    public void userCanUpdateProfileNameTest() {

        CreateUserRequest userRequest = AdminSteps.createUser();

        GetProfileResponse profile = UserSteps.getProfile(userRequest);

        assertNull(profile.getName());

        ChangeProfileRequest changeProfileRequest = RandomModelGenerator.generate(ChangeProfileRequest.class);

        //update profile
        UserSteps.updateProfile(
                userRequest,
                changeProfileRequest,
                ResponseSpecs.requestReturnsOK());

        GetProfileResponse updatedProfile = UserSteps.getProfile(userRequest);

        ModelAssertions.assertThatModels(changeProfileRequest, updatedProfile).match();
    }

    @Test
    public void userCannotUpdateProfileWithInvalidNameTest() {
        CreateUserRequest userRequest = AdminSteps.createUser();

        GetProfileResponse profileBeforeUpdate = UserSteps.getProfile(userRequest);

        assertNull(profileBeforeUpdate.getName());//check the initial field state


        String invalidName = TestDataGenerator.generateInvalidProfileName();//invalid format for profile name

        //create model
        ChangeProfileRequest changeProfileRequest = ChangeProfileRequest.builder()
                .name(invalidName)
                .build();

        UserSteps.updateProfile(
                userRequest,
                changeProfileRequest,
                ResponseSpecs.requestReturnsBadRequestWithText(
                        AlertMessage.INVALID_PROFILE_NAME.getMessage()));

        GetProfileResponse profileAfterUpdate =
                UserSteps.getProfile(userRequest);

        assertNull(profileAfterUpdate.getName());
    }
}
