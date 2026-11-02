package iteration1.ui;

import com.codeborne.selenide.*;
import generators.RandomModelGenerator;
import models.CreateUserRequest;
import models.CreateUserResponse;
import models.comparison.ModelAssertions;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import specs.RequestSpecs;

import java.util.Arrays;
import java.util.Map;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.switchTo;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CreateUserTest {
    @BeforeAll
    public static void setupSelenoid() {
        Configuration.remote = "http://localhost:4444/wd/hub";
        Configuration.baseUrl = "http://192.168.178.20:3000";
        Configuration.browser = "chrome";
        Configuration.browserSize = "1920x1080";

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true));
    }

    @Test
    public void adminCanCreateUserTest() {
        //STEP 1: Admin logged in
        CreateUserRequest admin = CreateUserRequest.builder().username("admin").password("admin").build();
        Selenide.open("/login");

        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(admin.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(admin.getPassword());
        $("button").click();

        $(Selectors.byText("Admin Panel")).shouldBe(Condition.visible);

        //STEP 2: Admin creates user
        CreateUserRequest newUser = RandomModelGenerator.generate(CreateUserRequest.class);
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(newUser.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(newUser.getPassword());
        $(Selectors.byText("Add User")).click();

        //STEP 3: Verify that we have an alert "✅ User created successfully!"
        Alert alert = switchTo().alert();

        assertEquals("✅ User created successfully!", alert.getText());
        alert.accept();

        //STEP 4: Verify that the user is displayed in the UI
        ElementsCollection allUsersFromDashboard = $(Selectors.byText("All Users")).parent().findAll("li");
        allUsersFromDashboard.findBy(Condition.exactText(newUser.getUsername() + "\nUSER")).shouldBe(Condition.visible);


        //STEP 5: Verify that the user was also successfully created via the API
        CreateUserResponse[] users = given()
                .spec(RequestSpecs.adminSpec())
                .get("http://localhost:4111/api/v1/admin/users")
                .then().assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract().as(CreateUserResponse[].class);

        CreateUserResponse createdUser = Arrays.stream(users)
                .filter(user -> user.getUsername().equals(newUser.getUsername()))
                .findFirst().get();

        ModelAssertions.assertThatModels(newUser, createdUser).match();
    }

    @Test
    public void adminCannotCreateUserWithInvalidDataTest() {
        //STEP 1: Admin logged in
        CreateUserRequest admin = CreateUserRequest.builder().username("admin").password("admin").build();
        Selenide.open("/login");

        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(admin.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(admin.getPassword());
        $("button").click();

        $(Selectors.byText("Admin Panel")).shouldBe(Condition.visible);

        //STEP 2: Admin creates user
        CreateUserRequest newUser = RandomModelGenerator.generate(CreateUserRequest.class);

        //create invalid userName
        newUser.setUsername("a");

        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(newUser.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(newUser.getPassword());
        $(Selectors.byText("Add User")).click();

        //STEP 3: Verify that we have an alert "✅ User created successfully!"
        Alert alert = switchTo().alert();

        assertThat(alert.getText().contains("Username must be between 3 and 15 characters"));

        alert.accept();

        //STEP 4: Verify that the user is NOT displayed in the UI
        ElementsCollection allUsersFromDashboard = $(Selectors.byText("All Users")).parent().findAll("li");
        allUsersFromDashboard.findBy(Condition.exactText(newUser.getUsername() + "\nUSER")).shouldNotBe(Condition.exist);


        //STEP 5: Verify that the user was also NOT created via the API
        CreateUserResponse[] users = given()
                .spec(RequestSpecs.adminSpec())
                .get("http://localhost:4111/api/v1/admin/users")
                .then().assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract().as(CreateUserResponse[].class);

        long usersWithSameUsernameAsNewUser = Arrays.stream(users)
                .filter(user -> user.getUsername().equals(newUser.getUsername()))
                .count();
        assertThat(usersWithSameUsernameAsNewUser).isZero();

    }
}
