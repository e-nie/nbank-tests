package iteration2.ui;

import api.enums.AlertMessage;
import api.generators.TestDataGenerator;
import api.models.ChangeProfileRequest;
import api.models.CreateUserRequest;
import api.models.GetProfileResponse;
import api.specs.ResponseSpecs;
import api.steps.AdminSteps;
import api.steps.UserSteps;
import iteration1.ui.BaseUiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ui.components.HeaderComponent;
import ui.pages.ProfilePage;
import ui.pages.UserDashboardPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;



public class ProfileTest extends BaseUiTest {

    private static final String DEFAULT_USER_NAME = "Noname";

    @DisplayName("TC-07: Successful profile name update")
    @Test
    public void userCanUpdateProfileNameViaUiTest() {
        CreateUserRequest userRequest = AdminSteps.createUser();
        GetProfileResponse userProfileBeforeUpdate = UserSteps.getProfile(userRequest);

        assertNull(userProfileBeforeUpdate.getName());

        authAsUser(userRequest);
        new UserDashboardPage().open();

        String newProfileName = TestDataGenerator.generateProfileName();
        System.out.println("Generated profile name: " + newProfileName);

        ProfilePage profilePage = new HeaderComponent()
                .shouldSeeUserName(DEFAULT_USER_NAME)
                .openProfile();

        profilePage.enterName(newProfileName)
                .shouldHaveName(newProfileName)
                .saveChanges()
                .checkAlertMessageAndAccept(AlertMessage.PROFILE_NAME_UPDATED_SUCCESSFULLY.getMessage());


        //verify the name is also updated in API
        GetProfileResponse userProfileAfterUpdate = UserSteps.getProfile(userRequest);

        assertEquals(newProfileName, userProfileAfterUpdate.getName());
    }

    @DisplayName("TC-08: Profile name with invalid number of words")
    @ParameterizedTest
    @ValueSource(strings = {"Anna", "Anna Maria Schmidt"})
    public void userCannotUpdateProfileNameWithInvalidNumberOfWordsViaUiTest(String invalidName) {
        CreateUserRequest userRequest = AdminSteps.createUser();
        GetProfileResponse userProfileBeforeUpdate = UserSteps.getProfile(userRequest);

        assertNull(userProfileBeforeUpdate.getName());

        authAsUser(userRequest);
        new UserDashboardPage().open();

        ProfilePage profilePage = new HeaderComponent()
                .shouldSeeUserName(DEFAULT_USER_NAME)
                .openProfile();

        profilePage.enterName(invalidName)
                .saveChanges()
                .checkAlertMessageAndAccept(AlertMessage.INVALID_PROFILE_NAME.getMessage());

        //verify the name was NOT updated in API
        GetProfileResponse userProfileAfterUpdate = UserSteps.getProfile(userRequest);

        assertNull(userProfileAfterUpdate.getName());
    }

    @DisplayName("TC-09: Profile name is empty")
    @Test
    public void userCannotUpdateProfileNameWithEmptyNameViaUiTest() {
        CreateUserRequest userRequest = AdminSteps.createUser();
        GetProfileResponse userProfileBeforeUpdate = UserSteps.getProfile(userRequest);

        assertNull(userProfileBeforeUpdate.getName());

        authAsUser(userRequest);
        new UserDashboardPage().open();

        ProfilePage profilePage = new HeaderComponent()
                .shouldSeeUserName(DEFAULT_USER_NAME)
                .openProfile();

        String newEmptyProfileName = "";

        profilePage.enterName(newEmptyProfileName)
                .saveChanges()
                .checkAlertMessageAndAccept(AlertMessage.INVALID_PROFILE_NAME_INPUT.getMessage());

        //verify the name was not updated in API
        GetProfileResponse userProfileAfterUpdate = UserSteps.getProfile(userRequest);

        assertNull(userProfileAfterUpdate.getName());
    }

    @DisplayName("TC-10: New profile name is the same as the current name")
    @Test
    public void userCannotUpdateProfileWithSameNameViaUiTest() {
        CreateUserRequest user = AdminSteps.createUser();

        String profileName = TestDataGenerator.generateProfileName();
        ChangeProfileRequest changeProfileRequest = ChangeProfileRequest.builder()
                .name(profileName)
                .build();

        //set new name via API
        UserSteps.updateProfile(user, changeProfileRequest, ResponseSpecs.requestReturnsOK());

        //comparing with api
        GetProfileResponse userProfileBeforeSecondUpdate = UserSteps.getProfile(user);
        assertEquals(profileName, userProfileBeforeSecondUpdate.getName());

        authAsUser(user);
        new UserDashboardPage().open();

        ProfilePage profilePage = new HeaderComponent()
                .shouldSeeUserName(profileName)
                .openProfile();

        profilePage.shouldHaveName(profileName)
                .saveChanges()
                .checkAlertMessageAndAccept(AlertMessage.SAME_PROFILE_NAME.getMessage());

        // Verify that the profile name has not changed
        GetProfileResponse userProfileAfterUpdate = UserSteps.getProfile(user);
        assertEquals(profileName, userProfileAfterUpdate.getName());
    }

    @DisplayName("TC-12: Profile name contains invalid characters")
    @ParameterizedTest
    @ValueSource(strings = {"Anna123 Schmidt", "Anna@ Schmidt", "$nn% Schm_dt"})
    public void userCannotUpdateProfileNameWithInvalidCharactersViaUiTest(String invalidName) {
        CreateUserRequest userRequest = AdminSteps.createUser();
        GetProfileResponse userProfileBeforeUpdate = UserSteps.getProfile(userRequest);

        assertNull(userProfileBeforeUpdate.getName());

        authAsUser(userRequest);
        new UserDashboardPage().open();

        ProfilePage profilePage = new HeaderComponent()
                .shouldSeeUserName(DEFAULT_USER_NAME)
                .openProfile();

        profilePage.enterName(invalidName)
                .saveChanges()
                .checkAlertMessageAndAccept(AlertMessage.INVALID_PROFILE_NAME.getMessage());

        //verify the name was NOT updated in API
        GetProfileResponse userProfileAfterUpdate = UserSteps.getProfile(userRequest);

        assertNull(userProfileAfterUpdate.getName());
    }

}



