package iteration2.ui;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import models.CreateAccountResponse;
import models.CreateUserRequest;
import models.LoginUserRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
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
import static com.codeborne.selenide.Selenide.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DepositTest {
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

    @DisplayName("TC-01: Successful deposit with valid amounts")
    @ParameterizedTest
    @ValueSource(doubles = {5000, 0.01})
    public void userCanMakeDepositViaUiWithValidAmountTest(double amount) {
        //1 . Prepare the test data via API:
        CreateUserRequest userRequest = AdminSteps.createUser();
        CreateAccountResponse account = UserSteps.createAccount(userRequest);
        CreateAccountResponse[] accountsBeforeDeposit = UserSteps.getAccounts(userRequest);
        assertEquals(1, accountsBeforeDeposit.length);
        assertEquals(0.0, accountsBeforeDeposit[0].getBalance());

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

        //save the token in localstorage
        executeJavaScript("localStorage.setItem('authToken', arguments[0])", userAuthHeader);//saved in local storage
        Selenide.open("/dashboard");

        //2. Test steps:
        $$("button").findBy(exactText("💰 Deposit Money")).click();

        $(Selectors.byText("💰 Deposit Money")).shouldBe(Condition.visible);
        $$("select").findBy(exactText("-- Choose an account --")).click();
        $("select").selectOption(1);
        $("[placeholder='Enter amount']").setValue(String.valueOf(amount));
        $$("button").findBy(exactText("\uD83D\uDCB5 Deposit")).click();

        //click alerts between deposits
        String alertText = switchTo().alert().getText();

        assertEquals(
                "✅ Successfully deposited $" + amount
                        + " to account " + account.getAccountNumber() + "!",
                alertText
        );

        switchTo().alert().accept();

        CreateAccountResponse[] accountsAfterDeposit = UserSteps.getAccounts(userRequest);

        assertEquals(1, accountsAfterDeposit.length);

        assertEquals(accountsBeforeDeposit[0].getBalance() + amount,
                accountsAfterDeposit[0].getBalance());

    }
}