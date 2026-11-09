package iteration2.ui;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import enums.AlertMessage;
import generators.TestDataGenerator;
import models.ChangeProfileRequest;
import models.CreateUserRequest;
import models.GetProfileResponse;
import models.LoginUserRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import requests.skeleton.Endpoint;
import requests.skeleton.requesters.CrudRequester;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.Map;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exactValue;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverConditions.urlContaining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ProfileTest {
    @BeforeAll
    public static void setupSelenoid() {
        Configuration.remote = "http://localhost:4444/wd/hub";
        Configuration.baseUrl = "http://192.168.178.20:3000";
        Configuration.browser = "chrome";
        Configuration.browserSize = "1920x1080";

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true));
    }

    @AfterEach
    void tearDown() {
        closeWebDriver();
    }

    @DisplayName("TC-07: Successful profile name update")
    @Test
    public void userCanUpdateProfileNameViaUiTest() {
        CreateUserRequest userRequest = AdminSteps.createUser();
        GetProfileResponse userProfileBeforeUpdate = UserSteps.getProfile(userRequest);

        assertNull(userProfileBeforeUpdate.getName());

        //get user token and save in local Storage
        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK())
                .post(LoginUserRequest.builder()
                        .username(userRequest.getUsername())
                        .password(userRequest.getPassword())
                        .build())
                .extract()
                .header("Authorization");

        Selenide.open("/");

        executeJavaScript("localStorage.setItem('authToken', arguments[0])", userAuthHeader);//saved in local storage
        Selenide.open("/dashboard");

        //Test steps
        $("span.user-name").shouldHave(exactText("Noname"));
        $(".profile-header").click();
        webdriver().shouldHave(urlContaining("/edit-profile"));

        String newProfileName = TestDataGenerator.generateProfileName();
        $("input[placeholder='Enter new name']").setValue(newProfileName);
        $$("button").findBy(exactText("\uD83D\uDCBE Save Changes")).click();

        String alertText = switchTo().alert().getText();

        assertEquals(AlertMessage.PROFILE_NAME_UPDATED_SUCCESSFULLY.getMessage(), alertText);

        switchTo().alert().accept();

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

        //get user token and save in local Storage
        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK())
                .post(LoginUserRequest.builder()
                        .username(userRequest.getUsername())
                        .password(userRequest.getPassword())
                        .build())
                .extract()
                .header("Authorization");

        Selenide.open("/");

        executeJavaScript("localStorage.setItem('authToken', arguments[0])", userAuthHeader);//saved in local storage
        Selenide.open("/dashboard");

        //Test Steps
        $("span.user-name").shouldHave(exactText("Noname"));
        $(".profile-header").click();
        webdriver().shouldHave(urlContaining("/edit-profile"));

        $("input[placeholder='Enter new name']").setValue(invalidName);
        $$("button").findBy(exactText("\uD83D\uDCBE Save Changes")).click();

        String alertText = switchTo().alert().getText();

        assertEquals(AlertMessage.INVALID_PROFILE_NAME.getMessage(), alertText);

        switchTo().alert().accept();

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

        //get user token and save in local Storage
        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK())
                .post(LoginUserRequest.builder()
                        .username(userRequest.getUsername())
                        .password(userRequest.getPassword())
                        .build())
                .extract()
                .header("Authorization");

        Selenide.open("/");

        executeJavaScript("localStorage.setItem('authToken', arguments[0])", userAuthHeader);//saved in local storage
        Selenide.open("/dashboard");

        //Test steps
        $("span.user-name").shouldHave(exactText("Noname"));
        $(".profile-header").click();
        webdriver().shouldHave(urlContaining("/edit-profile"));

        String newEmptyProfileName = "";
        $("input[placeholder='Enter new name']").setValue(newEmptyProfileName);
        $$("button").findBy(exactText("\uD83D\uDCBE Save Changes")).click();

        String alertText = switchTo().alert().getText();

        assertEquals(AlertMessage.INVALID_PROFILE_NAME_INPUT.getMessage(), alertText);

        switchTo().alert().accept();

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

        UserSteps.updateProfile(user, changeProfileRequest, ResponseSpecs.requestReturnsOK());

        //comparing with api
        GetProfileResponse userProfileBeforeSecondUpdate = UserSteps.getProfile(user);
        assertEquals(profileName, userProfileBeforeSecondUpdate.getName());

        // Get user token and save it in localStorage
        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK())
                .post(LoginUserRequest.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .build())
                .extract()
                .header("Authorization");

        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0])", userAuthHeader);
        Selenide.open("/dashboard");

        // Test steps
        $("span.user-name").shouldHave(exactText(profileName));
        $(".profile-header").click();
        webdriver().shouldHave(urlContaining("/edit-profile"));

        $("input[placeholder='Enter new name']").shouldHave(exactValue(profileName));

        $$("button").findBy(exactText("\uD83D\uDCBE Save Changes")).click();

        String alertText = switchTo().alert().getText();
        assertEquals(AlertMessage.SAME_PROFILE_NAME.getMessage(), alertText);
        switchTo().alert().accept();

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

        //get user token and save in local Storage
        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK())
                .post(LoginUserRequest.builder()
                        .username(userRequest.getUsername())
                        .password(userRequest.getPassword())
                        .build())
                .extract()
                .header("Authorization");

        Selenide.open("/");

        executeJavaScript("localStorage.setItem('authToken', arguments[0])", userAuthHeader);//saved in local storage
        Selenide.open("/dashboard");

        //Test Steps
        $("span.user-name").shouldHave(exactText("Noname"));
        $(".profile-header").click();
        webdriver().shouldHave(urlContaining("/edit-profile"));

        $("input[placeholder='Enter new name']").setValue(invalidName);
        $$("button").findBy(exactText("\uD83D\uDCBE Save Changes")).click();

        String alertText = switchTo().alert().getText();

        assertEquals(AlertMessage.INVALID_PROFILE_NAME.getMessage(), alertText);

        switchTo().alert().accept();

        //verify the name was NOT updated in API
        GetProfileResponse userProfileAfterUpdate = UserSteps.getProfile(userRequest);

        assertNull(userProfileAfterUpdate.getName());
    }

}



