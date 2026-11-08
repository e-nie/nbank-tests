package iteration2.ui;

import com.codeborne.selenide.*;
import models.CreateAccountResponse;
import models.CreateUserRequest;
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
import java.util.concurrent.atomic.AtomicInteger;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverRunner.getSelenideProxy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.codeborne.selenide.proxy.RequestMatcher.HttpMethod.POST;
import static com.codeborne.selenide.proxy.RequestMatchers.urlEndsWith;

import enums.AlertMessage;

public class DepositTest {
    @BeforeAll
    public static void setupSelenoid() {
        Configuration.remote = "http://localhost:4444/wd/hub";
        Configuration.baseUrl = "http://192.168.178.20:3000";
        Configuration.browser = "chrome";
        Configuration.browserSize = "1920x1080";

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true));

        Configuration.proxyEnabled = true;
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

        assertEquals(String.format(AlertMessage.SUCCESSFUL_DEPOSIT.getMessage(), amount, account.getAccountNumber()), alertText);


        switchTo().alert().accept();

        CreateAccountResponse[] accountsAfterDeposit = UserSteps.getAccounts(userRequest);

        assertEquals(1, accountsAfterDeposit.length);

        assertEquals(accountsBeforeDeposit[0].getBalance() + amount,
                accountsAfterDeposit[0].getBalance());
    }

    @DisplayName("TC-02: Deposit amount exceeds 5000")
    @Test
    public void userCannotMakeDepositAboveMaximumAmountViaUiTest() {
        //1 . Prepare the test data via API:
        CreateUserRequest userRequest = AdminSteps.createUser();
        UserSteps.createAccount(userRequest);
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

        double amount = 5001;

        $("[placeholder='Enter amount']").setValue(String.valueOf(amount));
        $$("button").findBy(exactText("\uD83D\uDCB5 Deposit")).click();

        //get alert text
        String alertText = switchTo().alert().getText();

        assertEquals(
                AlertMessage.DEPOSIT_AMOUNT_TOO_HIGH_UI.getMessage(),
                alertText
        );
        switchTo().alert().accept();

        CreateAccountResponse[] accountsAfterDeposit = UserSteps.getAccounts(userRequest);

        assertEquals(1, accountsAfterDeposit.length);

        assertEquals(accountsBeforeDeposit[0].getBalance(),
                accountsAfterDeposit[0].getBalance());
    }

    @DisplayName("TC-03: Deposit with invalid amount")
    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", ""})
    public void userCannotMakeDepositWithInvalidAmountViaUiTest(String amount) {
        //1 . Prepare the test data via API:
        CreateUserRequest userRequest = AdminSteps.createUser();
        UserSteps.createAccount(userRequest);
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
        $("[placeholder='Enter amount']").setValue(amount);
        $$("button").findBy(exactText("\uD83D\uDCB5 Deposit")).click();

        //click alerts between deposits
        String alertText = switchTo().alert().getText();

        assertEquals(AlertMessage.INVALID_DEPOSIT_AMOUNT.getMessage(), alertText);

        switchTo().alert().accept();

        CreateAccountResponse[] accountsAfterDeposit = UserSteps.getAccounts(userRequest);

        assertEquals(1, accountsAfterDeposit.length);

        assertEquals(accountsBeforeDeposit[0].getBalance(), accountsAfterDeposit[0].getBalance());
    }

    @DisplayName("TC-04: Deposit without selecting an account")
    @Test
    public void userCannotMakeDepositWithoutSelectingAccountViaUiTest() {
        //1 . Prepare the test data via API:
        CreateUserRequest userRequest = AdminSteps.createUser();
        UserSteps.createAccount(userRequest);

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

        $("[placeholder='Enter amount']").setValue("100");
        $$("button").findBy(exactText("\uD83D\uDCB5 Deposit")).click();

        //get alert text
        String alertText = switchTo().alert().getText();

        assertEquals(AlertMessage.ACCOUNT_NOT_SELECTED.getMessage(), alertText);
        switchTo().alert().accept();

        CreateAccountResponse[] accountsAfterDeposit = UserSteps.getAccounts(userRequest);

        assertEquals(1, accountsAfterDeposit.length);

        assertEquals(accountsBeforeDeposit[0].getBalance(),
                accountsAfterDeposit[0].getBalance());
    }

    @DisplayName("TC-05: User can see only the owned accounts")
    @Test
    public void userCanSeeOnlyOwnedAccountsViaUiTest() {
        CreateUserRequest userRequestA = AdminSteps.createUser();
        CreateAccountResponse accountA = UserSteps.createAccount(userRequestA);

        CreateUserRequest userRequestB = AdminSteps.createUser();
        CreateAccountResponse accountB = UserSteps.createAccount(userRequestB);


        //get userA token and save in local Storage
        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK())
                .post(LoginUserRequest.builder()
                        .username(userRequestA.getUsername())
                        .password(userRequestA.getPassword())
                        .build())
                .extract()
                .header("Authorization");

        Selenide.open("/");

        //save the token in localstorage
        executeJavaScript("localStorage.setItem('authToken', arguments[0])", userAuthHeader);//saved in local storage

        Selenide.open("/dashboard");

        $$("button").findBy(exactText("💰 Deposit Money")).click();

        $(Selectors.byText("💰 Deposit Money")).shouldBe(Condition.visible);
        $$("select").findBy(exactText("-- Choose an account --")).click();

        //Selenide assertions
        $("select").$$("option").findBy(Condition.text(accountA.getAccountNumber())).should(Condition.exist);

        $("select").$$("option").findBy(Condition.text(accountB.getAccountNumber())).shouldNot(Condition.exist);
    }

    @DisplayName("TC-06: Error handling when deposit fails")
    @Test
    public void userSeesErrorWhenDepositFailsViaUiTest() {
        CreateUserRequest userRequest = AdminSteps.createUser();
        UserSteps.createAccount(userRequest);

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

        double amount = 100;
        $("[placeholder='Enter amount']").setValue(String.valueOf(amount));


        //сохраняем URL
        String urlBeforeDeposit = WebDriverRunner.url();

        AtomicInteger mockedRequests = new AtomicInteger(0);

        getSelenideProxy().responseMocker().mockText(
                "deposit-error",
                urlEndsWith(POST, "/accounts/deposit"),
                500,
                () -> {
                    mockedRequests.incrementAndGet();
                    return "{\"error\":\"Internal Server Error\"}";
                }
        );
        $$("button").findBy(exactText("\uD83D\uDCB5 Deposit")).click();

        String alertText = switchTo().alert().getText();

        assertEquals(AlertMessage.DEPOSIT_UNSUCCESSFUL.getMessage(), alertText);

        switchTo().alert().accept();

        // Verify that Selenide Proxy intercepted the deposit request exactly once
        assertEquals(1, mockedRequests.get());

        //user remains on the deposit money page
        assertEquals(urlBeforeDeposit, WebDriverRunner.url());
        $(Selectors.byText("💰 Deposit Money")).shouldBe(Condition.visible);
    }


}