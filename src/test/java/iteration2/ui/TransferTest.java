package iteration2.ui;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import enums.AlertMessage;
import generators.TestDataGenerator;
import models.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import requests.skeleton.Endpoint;
import requests.skeleton.requesters.CrudRequester;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.Selenide.switchTo;
import static com.codeborne.selenide.WebDriverConditions.urlContaining;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransferTest {
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

    @DisplayName("TC-12: Successful transfer between accounts")
    @ParameterizedTest
    @ValueSource(doubles = {0.01, 10000})
    public void userCanTransferMoneyBetweenAccountsViaUiTest(double amount) {
        //prepare test data
        CreateUserRequest user = AdminSteps.createUser();
        CreateAccountResponse senderAccount = UserSteps.createAccount(user);
        CreateAccountResponse recipientAccount = UserSteps.createAccount(user);

        CreateAccountResponse[] accountsBeforeDeposit = UserSteps.getAccounts(user);
        assertEquals(2, accountsBeforeDeposit.length);
        assertEquals(0.0, accountsBeforeDeposit[0].getBalance());
        assertEquals(0.0, accountsBeforeDeposit[1].getBalance());

        //set profile name:
        String profileName = TestDataGenerator.generateProfileName();

        ChangeProfileRequest changeProfileRequest = ChangeProfileRequest.builder()
                .name(profileName)
                .build();
        //update name
        UserSteps.updateProfile(user, changeProfileRequest, ResponseSpecs.requestReturnsOK());

        //make 2 deposits
        double depositAmount = 5000.0;
        DepositRequest depositRequest = DepositRequest.builder()
                .id(senderAccount.getId())
                .balance(depositAmount)
                .build();

        for (int i = 0; i < 2; i++) {
            UserSteps.makeDeposit(
                    user,
                    depositRequest,
                    ResponseSpecs.requestReturnsOK());
        }

        // Get account balances before transfer
        CreateAccountResponse[] accountsBeforeTransfer = UserSteps.getAccounts(user);

        //get user token and save in local Storage
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

        //save the token in localstorage
        executeJavaScript("localStorage.setItem('authToken', arguments[0])", userAuthHeader);//saved in local storage
        Selenide.open("/dashboard");

        // UI. Open transfer form
        $$("button").findBy(exactText("\uD83D\uDD04 Make a Transfer")).click();
        webdriver().shouldHave(urlContaining("/transfer"));
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(Condition.visible);

        //fill in the transfer form
        $("select.account-selector").selectOptionContainingText(senderAccount.getAccountNumber());
        $("input[placeholder='Enter recipient name']").setValue(profileName);
        $("input[placeholder='Enter recipient account number']").setValue(recipientAccount.getAccountNumber());
        $("input[placeholder='Enter amount']").setValue(String.valueOf(amount));
        $("#confirmCheck").setSelected(true);
        $$("button").findBy(exactText("\uD83D\uDE80 Send Transfer")).click();

        String alertText = switchTo().alert().getText();

        assertEquals(String.format(AlertMessage.TRANSFER_SUCCESSFUL_UI.getMessage(), amount, recipientAccount.getAccountNumber()), alertText);

        switchTo().alert().accept();

        CreateAccountResponse[] accountsAfterTransfer = UserSteps.getAccounts(user);

        CreateAccountResponse senderAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getAccountNumber().equals(senderAccount.getAccountNumber()))
                .findFirst()
                .orElseThrow();

        CreateAccountResponse recipientAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getAccountNumber().equals(recipientAccount.getAccountNumber()))
                .findFirst()
                .orElseThrow();

        assertEquals(10000.0 - amount, senderAfterTransfer.getBalance());
        assertEquals(amount, recipientAfterTransfer.getBalance());
    }


    @DisplayName("TC-13: Transfer with empty required fields")
    @ParameterizedTest
    @ValueSource(strings = {
            "senderAccount",
            "recipientName",
            "recipientAccountNumber",
            "amount"
    })
    public void userCannotTransferMoneyWithEmptyRequiredFieldsViaUiTest(String emptyField) {
        //prepare test data
        CreateUserRequest user = AdminSteps.createUser();
        CreateAccountResponse senderAccount = UserSteps.createAccount(user);
        CreateAccountResponse recipientAccount = UserSteps.createAccount(user);

        CreateAccountResponse[] accountsBeforeDeposit = UserSteps.getAccounts(user);
        assertEquals(2, accountsBeforeDeposit.length);
        assertEquals(0.0, accountsBeforeDeposit[0].getBalance());
        assertEquals(0.0, accountsBeforeDeposit[1].getBalance());

        // Set profile name via API
        String profileName = TestDataGenerator.generateProfileName();

        ChangeProfileRequest changeProfileRequest = ChangeProfileRequest.builder()
                .name(profileName)
                .build();

        UserSteps.updateProfile(user, changeProfileRequest, ResponseSpecs.requestReturnsOK());

// Make deposit via API
        double depositAmount = TestDataGenerator.generateDepositAmount();

        DepositRequest depositRequest = DepositRequest.builder()
                .id(senderAccount.getId())
                .balance(depositAmount)
                .build();

        UserSteps.makeDeposit(user, depositRequest, ResponseSpecs.requestReturnsOK());

// Generate transfer amount
        double transferAmount = TestDataGenerator.generateTransferAmount(depositAmount);

// Get balances before transfer
        CreateAccountResponse[] accountsBeforeTransfer = UserSteps.getAccounts(user);

        //sender acc before transfer
        CreateAccountResponse senderBeforeTransfer = Arrays.stream(accountsBeforeTransfer)
                .filter(account -> account.getAccountNumber().equals(senderAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        //recipient acc before transfer
        CreateAccountResponse recipientBeforeTransfer = Arrays.stream(accountsBeforeTransfer)
                .filter(account -> account.getAccountNumber().equals(recipientAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        //get user token and save in local Storage
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

        //save the token in localstorage
        executeJavaScript("localStorage.setItem('authToken', arguments[0])", userAuthHeader);//saved in local storage
        Selenide.open("/dashboard");

        // Open transfer form
        $$("button").findBy(exactText("🔄 Make a Transfer")).click();
        webdriver().shouldHave(urlContaining("/transfer"));
        $(Selectors.byText("🔄 Make a Transfer")).shouldBe(Condition.visible);

        //test steps
        if (!emptyField.equals("senderAccount")) {
            $("select.account-selector")
                    .selectOptionContainingText(senderAccount.getAccountNumber());
        }

        if (!emptyField.equals("recipientName")) {
            $("input[placeholder='Enter recipient name']").setValue(profileName);
        }

        if (!emptyField.equals("recipientAccountNumber")) {
            $("input[placeholder='Enter recipient account number']").setValue(recipientAccount.getAccountNumber());
        }

        if (!emptyField.equals("amount")) {
            $("input[placeholder='Enter amount']").setValue(String.valueOf(transferAmount));
        }

        $("#confirmCheck").setSelected(true);
        $$("button").findBy(exactText("\uD83D\uDE80 Send Transfer")).click();

        String alertText = switchTo().alert().getText();

        if (emptyField.equals("recipientName")) {
            assertEquals(AlertMessage.RECIPIENT_NAME_MISMATCH.getMessage(), alertText);
        } else {
            assertEquals(AlertMessage.REQUIRED_FIELDS_NOT_FILLED.getMessage(), alertText);
        }

        switchTo().alert().accept();
        //all user's accounts after transfer
        CreateAccountResponse[] accountsAfterTransfer = UserSteps.getAccounts(user);


        //sender account after
        CreateAccountResponse senderAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getAccountNumber().equals(senderAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        //recipient account after
        CreateAccountResponse recipientAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getAccountNumber().equals(recipientAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        assertEquals(senderBeforeTransfer.getBalance(), senderAfterTransfer.getBalance());
        assertEquals(recipientBeforeTransfer.getBalance(), recipientAfterTransfer.getBalance());

    }

    @DisplayName("TC-14: Transfer without confirmation")
    @Test
    public void userCannotTransferMoneyWithoutConfirmationViaUiTest() {
        //prepare test data
        CreateUserRequest user = AdminSteps.createUser();
        CreateAccountResponse senderAccount = UserSteps.createAccount(user);
        CreateAccountResponse recipientAccount = UserSteps.createAccount(user);

        CreateAccountResponse[] accountsBeforeDeposit = UserSteps.getAccounts(user);
        assertEquals(2, accountsBeforeDeposit.length);
        assertEquals(0.0, accountsBeforeDeposit[0].getBalance());
        assertEquals(0.0, accountsBeforeDeposit[1].getBalance());

        // Set profile name via API
        String profileName = TestDataGenerator.generateProfileName();

        ChangeProfileRequest changeProfileRequest = ChangeProfileRequest.builder()
                .name(profileName)
                .build();

        UserSteps.updateProfile(user, changeProfileRequest, ResponseSpecs.requestReturnsOK());

        // Make deposit via API
        double depositAmount = TestDataGenerator.generateDepositAmount();

        DepositRequest depositRequest = DepositRequest.builder()
                .id(senderAccount.getId())
                .balance(depositAmount)
                .build();

        UserSteps.makeDeposit(user, depositRequest, ResponseSpecs.requestReturnsOK());

        // Generate transfer amount
        double transferAmount = TestDataGenerator.generateTransferAmount(depositAmount);

        // Get balances before transfer
        CreateAccountResponse[] accountsBeforeTransfer = UserSteps.getAccounts(user);

        //sender acc before transfer
        CreateAccountResponse senderBeforeTransfer = Arrays.stream(accountsBeforeTransfer)
                .filter(account -> account.getAccountNumber().equals(senderAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        //recipient acc before transfer
        CreateAccountResponse recipientBeforeTransfer = Arrays.stream(accountsBeforeTransfer)
                .filter(account -> account.getAccountNumber().equals(recipientAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        //get user token and save in local Storage
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

        //save the token in localstorage
        executeJavaScript("localStorage.setItem('authToken', arguments[0])", userAuthHeader);//saved in local storage
        Selenide.open("/dashboard");

        // Open transfer form
        $$("button").findBy(exactText("🔄 Make a Transfer")).click();
        webdriver().shouldHave(urlContaining("/transfer"));
        $(Selectors.byText("🔄 Make a Transfer")).shouldBe(Condition.visible);

        //test steps
        //fill in the transfer form
        $("select.account-selector").selectOptionContainingText(senderAccount.getAccountNumber());
        $("input[placeholder='Enter recipient name']").setValue(profileName);
        $("input[placeholder='Enter recipient account number']").setValue(recipientAccount.getAccountNumber());
        $("input[placeholder='Enter amount']").setValue(String.valueOf(transferAmount));
        //-omit the checkbox-
        $$("button").findBy(exactText("\uD83D\uDE80 Send Transfer")).click();

        String alertText = switchTo().alert().getText();
        assertEquals(AlertMessage.REQUIRED_FIELDS_NOT_FILLED.getMessage(), alertText);
        switchTo().alert().accept();

        //all user's accounts after transfer
        CreateAccountResponse[] accountsAfterTransfer = UserSteps.getAccounts(user);

        //sender account after
        CreateAccountResponse senderAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getAccountNumber().equals(senderAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        //recipient account after
        CreateAccountResponse recipientAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getAccountNumber().equals(recipientAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        assertEquals(senderBeforeTransfer.getBalance(), senderAfterTransfer.getBalance());
        assertEquals(recipientBeforeTransfer.getBalance(), recipientAfterTransfer.getBalance());
    }

    @Disabled("TC-15: Expected behavior for transfer to the same account is not specified in requirements")
    @DisplayName("TC-15: Transfer to the same account")
    @Test
    public void userCannotTransferMoneyToSameAccountViaUiTest() {
        //prepare test data
        CreateUserRequest user = AdminSteps.createUser();
        CreateAccountResponse userAccount = UserSteps.createAccount(user);


        CreateAccountResponse[] accountsBeforeDeposit = UserSteps.getAccounts(user);
        assertEquals(1, accountsBeforeDeposit.length);
        assertEquals(0.0, accountsBeforeDeposit[0].getBalance());


        // Set profile name via API
        String profileName = TestDataGenerator.generateProfileName();

        ChangeProfileRequest changeProfileRequest = ChangeProfileRequest.builder()
                .name(profileName)
                .build();

        UserSteps.updateProfile(user, changeProfileRequest, ResponseSpecs.requestReturnsOK());

        // Make deposit via API
        double depositAmount = TestDataGenerator.generateDepositAmount();

        DepositRequest depositRequest = DepositRequest.builder()
                .id(userAccount.getId())
                .balance(depositAmount)
                .build();

        UserSteps.makeDeposit(user, depositRequest, ResponseSpecs.requestReturnsOK());

        // Generate transfer amount
        double transferAmount = TestDataGenerator.generateTransferAmount(depositAmount);

        // Get balances before transfer
        CreateAccountResponse[] accountsBeforeTransfer = UserSteps.getAccounts(user);

        //user acc before transfer
        CreateAccountResponse userAccountBeforeTransfer = Arrays.stream(accountsBeforeTransfer)
                .filter(account -> account.getAccountNumber().equals(userAccount.getAccountNumber()))
                .findFirst().orElseThrow();


        //get user token and save in local Storage
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

        //save the token in localstorage
        executeJavaScript("localStorage.setItem('authToken', arguments[0])", userAuthHeader);//saved in local storage
        Selenide.open("/dashboard");

        // Open transfer form
        $$("button").findBy(exactText("🔄 Make a Transfer")).click();
        webdriver().shouldHave(urlContaining("/transfer"));
        $(Selectors.byText("🔄 Make a Transfer")).shouldBe(Condition.visible);

        //test steps
        //fill in the transfer form
        $("select.account-selector").selectOptionContainingText(userAccount.getAccountNumber());
        $("select.account-selector").shouldHave(Condition.value(String.valueOf(userAccount.getId())));        $("input[placeholder='Enter recipient name']").setValue(profileName);
        $("input[placeholder='Enter recipient account number']").setValue(userAccount.getAccountNumber());
        $("input[placeholder='Enter amount']").setValue(String.valueOf(transferAmount));
        $("#confirmCheck").setSelected(true);
        $$("button").findBy(exactText("\uD83D\uDE80 Send Transfer")).click();

        String alertText = switchTo().alert().getText();
        assertEquals(AlertMessage.INVALID_TRANSFER.getMessage(), alertText);
        switchTo().alert().accept();

        //user's accounts after transfer
        CreateAccountResponse[] accountsAfterTransfer = UserSteps.getAccounts(user);
        assertEquals(userAccountBeforeTransfer.getBalance(), accountsAfterTransfer[0].getBalance());
    }

    @DisplayName("TC-16: Transfer with incorrect recipient name")
    @Test
    public void userCannotTransferMoneyWithIncorrectRecipientNameViaUiTest() {
//prepare test data
        CreateUserRequest user = AdminSteps.createUser();
        CreateAccountResponse senderAccount = UserSteps.createAccount(user);
        CreateAccountResponse recipientAccount = UserSteps.createAccount(user);

        CreateAccountResponse[] accountsBeforeDeposit = UserSteps.getAccounts(user);
        assertEquals(2, accountsBeforeDeposit.length);
        assertEquals(0.0, accountsBeforeDeposit[0].getBalance());
        assertEquals(0.0, accountsBeforeDeposit[1].getBalance());

        // Set profile name via API
        String profileName = TestDataGenerator.generateProfileName();

        ChangeProfileRequest changeProfileRequest = ChangeProfileRequest.builder()
                .name(profileName)
                .build();

        UserSteps.updateProfile(user, changeProfileRequest, ResponseSpecs.requestReturnsOK());

        // Make deposit via API
        double depositAmount = TestDataGenerator.generateDepositAmount();

        DepositRequest depositRequest = DepositRequest.builder()
                .id(senderAccount.getId())
                .balance(depositAmount)
                .build();

        UserSteps.makeDeposit(user, depositRequest, ResponseSpecs.requestReturnsOK());

        // Generate transfer amount
        double transferAmount = TestDataGenerator.generateTransferAmount(depositAmount);

        // Get balances before transfer
        CreateAccountResponse[] accountsBeforeTransfer = UserSteps.getAccounts(user);

        //sender acc before transfer
        CreateAccountResponse senderBeforeTransfer = Arrays.stream(accountsBeforeTransfer)
                .filter(account -> account.getAccountNumber().equals(senderAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        //recipient acc before transfer
        CreateAccountResponse recipientBeforeTransfer = Arrays.stream(accountsBeforeTransfer)
                .filter(account -> account.getAccountNumber().equals(recipientAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        //get user token and save in local Storage
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

        //save the token in localstorage
        executeJavaScript("localStorage.setItem('authToken', arguments[0])", userAuthHeader);//saved in local storage
        Selenide.open("/dashboard");

        // Open transfer form
        $$("button").findBy(exactText("🔄 Make a Transfer")).click();
        webdriver().shouldHave(urlContaining("/transfer"));
        $(Selectors.byText("🔄 Make a Transfer")).shouldBe(Condition.visible);

        //test steps
        //fill in the transfer form
        $("select.account-selector").selectOptionContainingText(senderAccount.getAccountNumber());
        $("input[placeholder='Enter recipient name']").setValue(TestDataGenerator.generateProfileName());
        $("input[placeholder='Enter recipient account number']").setValue(recipientAccount.getAccountNumber());
        $("input[placeholder='Enter amount']").setValue(String.valueOf(transferAmount));
        $("#confirmCheck").setSelected(true);

        $$("button").findBy(exactText("\uD83D\uDE80 Send Transfer")).click();

        String alertText = switchTo().alert().getText();
        assertEquals(AlertMessage.INCORRECT_TRANSFER_NAME.getMessage(), alertText);
        switchTo().alert().accept();

        //all user's accounts after transfer
        CreateAccountResponse[] accountsAfterTransfer = UserSteps.getAccounts(user);

        //sender account after
        CreateAccountResponse senderAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getAccountNumber().equals(senderAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        //recipient account after
        CreateAccountResponse recipientAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getAccountNumber().equals(recipientAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        assertEquals(senderBeforeTransfer.getBalance(), senderAfterTransfer.getBalance());
        assertEquals(recipientBeforeTransfer.getBalance(), recipientAfterTransfer.getBalance());
    }

    @DisplayName("TC-17: Transfer amount exceeds maximum limit")
    @ParameterizedTest
    @ValueSource(doubles = {10000.01})
    public void userCannotTransferMoneyAboveMaximumLimitViaUiTest(double transferAmount) {
//prepare test data
        CreateUserRequest user = AdminSteps.createUser();
        CreateAccountResponse senderAccount = UserSteps.createAccount(user);
        CreateAccountResponse recipientAccount = UserSteps.createAccount(user);

        CreateAccountResponse[] accountsBeforeDeposit = UserSteps.getAccounts(user);
        assertEquals(2, accountsBeforeDeposit.length);
        assertEquals(0.0, accountsBeforeDeposit[0].getBalance());
        assertEquals(0.0, accountsBeforeDeposit[1].getBalance());

        // Set profile name via API
        String profileName = TestDataGenerator.generateProfileName();

        ChangeProfileRequest changeProfileRequest = ChangeProfileRequest.builder()
                .name(profileName)
                .build();

        UserSteps.updateProfile(user, changeProfileRequest, ResponseSpecs.requestReturnsOK());

        // Make deposit via API

        double depositAmount = TestDataGenerator.generateDepositForTransfer();

        DepositRequest depositRequest = DepositRequest.builder()
                .id(senderAccount.getId())
                .balance(depositAmount)
                .build();

        //make 3 deposits
        for (int i = 0; i < 3; i++) {
            UserSteps.makeDeposit(user, depositRequest, ResponseSpecs.requestReturnsOK());
        }

        // Get balances before transfer
        CreateAccountResponse[] accountsBeforeTransfer = UserSteps.getAccounts(user);

        //sender acc before transfer
        CreateAccountResponse senderBeforeTransfer = Arrays.stream(accountsBeforeTransfer)
                .filter(account -> account.getAccountNumber().equals(senderAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        //recipient acc before transfer
        CreateAccountResponse recipientBeforeTransfer = Arrays.stream(accountsBeforeTransfer)
                .filter(account -> account.getAccountNumber().equals(recipientAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        //get user token and save in local Storage
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

        //save the token in localstorage
        executeJavaScript("localStorage.setItem('authToken', arguments[0])", userAuthHeader);//saved in local storage
        Selenide.open("/dashboard");

        // Open transfer form
        $$("button").findBy(exactText("🔄 Make a Transfer")).click();
        webdriver().shouldHave(urlContaining("/transfer"));
        $(Selectors.byText("🔄 Make a Transfer")).shouldBe(Condition.visible);

        //test steps
        //fill in the transfer form
        $("select.account-selector").selectOptionContainingText(senderAccount.getAccountNumber());
        $("input[placeholder='Enter recipient name']").setValue(profileName);
        $("input[placeholder='Enter recipient account number']").setValue(recipientAccount.getAccountNumber());
        $("input[placeholder='Enter amount']").setValue(String.valueOf(transferAmount));
        $("#confirmCheck").setSelected(true);
        $$("button").findBy(exactText("\uD83D\uDE80 Send Transfer")).click();

        String alertText = switchTo().alert().getText();
        assertEquals(AlertMessage.TRANSFER_AMOUNT_EXCEEDS_LIMIT_UI.getMessage(), alertText);
        switchTo().alert().accept();

        //all user's accounts after transfer
        CreateAccountResponse[] accountsAfterTransfer = UserSteps.getAccounts(user);

        //sender account after
        CreateAccountResponse senderAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getAccountNumber().equals(senderAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        //recipient account after
        CreateAccountResponse recipientAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getAccountNumber().equals(recipientAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        assertEquals(senderBeforeTransfer.getBalance(), senderAfterTransfer.getBalance());
        assertEquals(recipientBeforeTransfer.getBalance(), recipientAfterTransfer.getBalance());
    }

    @DisplayName("TC-18: Transfer with insufficient funds")
    @Test
    public void userCannotTransferMoneyWithInsufficientFundsViaUiTest() {
//prepare test data
        CreateUserRequest user = AdminSteps.createUser();
        CreateAccountResponse senderAccount = UserSteps.createAccount(user);
        CreateAccountResponse recipientAccount = UserSteps.createAccount(user);

        CreateAccountResponse[] accountsBeforeDeposit = UserSteps.getAccounts(user);
        assertEquals(2, accountsBeforeDeposit.length);
        assertEquals(0.0, accountsBeforeDeposit[0].getBalance());
        assertEquals(0.0, accountsBeforeDeposit[1].getBalance());

        // Set profile name via API
        String profileName = TestDataGenerator.generateProfileName();

        ChangeProfileRequest changeProfileRequest = ChangeProfileRequest.builder()
                .name(profileName)
                .build();

        UserSteps.updateProfile(user, changeProfileRequest, ResponseSpecs.requestReturnsOK());

        // Make deposit via API
        double depositAmount = TestDataGenerator.generateDepositForTransfer();

        double transferAmount = depositAmount + 1.0;

        DepositRequest depositRequest = DepositRequest.builder()
                .id(senderAccount.getId())
                .balance(depositAmount)
                .build();

        UserSteps.makeDeposit(user, depositRequest, ResponseSpecs.requestReturnsOK());
        // Get balances before transfer
        CreateAccountResponse[] accountsBeforeTransfer = UserSteps.getAccounts(user);

        //sender acc before transfer
        CreateAccountResponse senderBeforeTransfer = Arrays.stream(accountsBeforeTransfer)
                .filter(account -> account.getAccountNumber().equals(senderAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        //recipient acc before transfer
        CreateAccountResponse recipientBeforeTransfer = Arrays.stream(accountsBeforeTransfer)
                .filter(account -> account.getAccountNumber().equals(recipientAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        //get user token and save in local Storage
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

        //save the token in localstorage
        executeJavaScript("localStorage.setItem('authToken', arguments[0])", userAuthHeader);//saved in local storage
        Selenide.open("/dashboard");

        // Open transfer form
        $$("button").findBy(exactText("🔄 Make a Transfer")).click();
        webdriver().shouldHave(urlContaining("/transfer"));
        $(Selectors.byText("🔄 Make a Transfer")).shouldBe(Condition.visible);

        //test steps
        //fill in the transfer form
        $("select.account-selector").selectOptionContainingText(senderAccount.getAccountNumber());
        $("input[placeholder='Enter recipient name']").setValue(profileName);
        $("input[placeholder='Enter recipient account number']").setValue(recipientAccount.getAccountNumber());

        $("input[placeholder='Enter amount']").setValue(String.valueOf(transferAmount));
        $("#confirmCheck").setSelected(true);
        $$("button").findBy(exactText("\uD83D\uDE80 Send Transfer")).click();

        String alertText = switchTo().alert().getText();
        assertEquals(AlertMessage.INVALID_TRANSFER.getMessage(), alertText);
        switchTo().alert().accept();

        //all user's accounts after transfer
        CreateAccountResponse[] accountsAfterTransfer = UserSteps.getAccounts(user);

        //sender account after
        CreateAccountResponse senderAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getAccountNumber().equals(senderAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        //recipient account after
        CreateAccountResponse recipientAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getAccountNumber().equals(recipientAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        assertEquals(senderBeforeTransfer.getBalance(), senderAfterTransfer.getBalance());
        assertEquals(recipientBeforeTransfer.getBalance(), recipientAfterTransfer.getBalance());
    }

    @DisplayName("TC-19: Transfer to non-existing recipient account")
    @Test
    public void userCannotTransferMoneyToNonExistingRecipientAccountViaUiTest() {
        //prepare test data
        CreateUserRequest user = AdminSteps.createUser();
        CreateAccountResponse senderAccount = UserSteps.createAccount(user);

        CreateAccountResponse[] accountsBeforeDeposit = UserSteps.getAccounts(user);
        assertEquals(1, accountsBeforeDeposit.length);
        assertEquals(0.0, accountsBeforeDeposit[0].getBalance());

        // Set profile name via API
        String profileName = TestDataGenerator.generateProfileName();

        ChangeProfileRequest changeProfileRequest = ChangeProfileRequest.builder()
                .name(profileName)
                .build();

        UserSteps.updateProfile(user, changeProfileRequest, ResponseSpecs.requestReturnsOK());

        // Make deposit via API
        double depositAmount = TestDataGenerator.generateDepositForTransfer();

        double transferAmount = TestDataGenerator.generateTransferAmount(depositAmount);

        DepositRequest depositRequest = DepositRequest.builder()
                .id(senderAccount.getId())
                .balance(depositAmount)
                .build();

        UserSteps.makeDeposit(user, depositRequest, ResponseSpecs.requestReturnsOK());
        // Get balances before transfer
        CreateAccountResponse[] accountsBeforeTransfer = UserSteps.getAccounts(user);

        //sender acc before transfer
        CreateAccountResponse senderBeforeTransfer = Arrays.stream(accountsBeforeTransfer)
                .filter(account -> account.getAccountNumber().equals(senderAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        //get user token and save in local Storage
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

        //save the token in localstorage
        executeJavaScript("localStorage.setItem('authToken', arguments[0])", userAuthHeader);//saved in local storage
        Selenide.open("/dashboard");

        // Open transfer form
        $$("button").findBy(exactText("🔄 Make a Transfer")).click();
        webdriver().shouldHave(urlContaining("/transfer"));
        $(Selectors.byText("🔄 Make a Transfer")).shouldBe(Condition.visible);

        //test steps
        //fill in the transfer form
        $("select.account-selector").selectOptionContainingText(senderAccount.getAccountNumber());
        $("input[placeholder='Enter recipient name']").setValue(profileName);
        String nonExistingAccountNumber = TestDataGenerator.generateNonExistingAccountNumber();
        $("input[placeholder='Enter recipient account number']").setValue(nonExistingAccountNumber);

        $("input[placeholder='Enter amount']").setValue(String.valueOf(transferAmount));
        $("#confirmCheck").setSelected(true);
        $$("button").findBy(exactText("\uD83D\uDE80 Send Transfer")).click();

        String alertText = switchTo().alert().getText();
        assertEquals(AlertMessage.RECIPIENT_ACCOUNT_NOT_FOUND.getMessage(), alertText);
        switchTo().alert().accept();

        //all user's accounts after transfer
        CreateAccountResponse[] accountsAfterTransfer = UserSteps.getAccounts(user);
        CreateAccountResponse senderAfterTransfer = accountsAfterTransfer[0];

        assertEquals(senderBeforeTransfer.getBalance(), senderAfterTransfer.getBalance());
    }

    @Disabled("BUG: Repeat Transfer sends incorrect receiverAccountId (sender instead of recipient)")
    /*Expected :✅ Transfer of $1478.42 successful from Account 5 to 6!
    Actual   :✅ Transfer of $1478.42 successful from Account 5 to 5!
    */

    @DisplayName("TC-20: Repeat of successful transfer")
    @Test
    public void userCanRepeatSuccessfulTransferViaUiTest() {
        //prepare test data
        CreateUserRequest user = AdminSteps.createUser();
        CreateAccountResponse senderAccount = UserSteps.createAccount(user);
        CreateAccountResponse recipientAccount = UserSteps.createAccount(user);

        CreateAccountResponse[] accountsBeforeDeposit = UserSteps.getAccounts(user);
        assertEquals(2, accountsBeforeDeposit.length);
        assertEquals(0.0, accountsBeforeDeposit[0].getBalance());
        assertEquals(0.0, accountsBeforeDeposit[1].getBalance());

        //set profile name:
        String profileName = TestDataGenerator.generateProfileName();

        ChangeProfileRequest changeProfileRequest = ChangeProfileRequest.builder()
                .name(profileName)
                .build();
        //update name
        UserSteps.updateProfile(user, changeProfileRequest, ResponseSpecs.requestReturnsOK());

        //make deposit
        double depositAmount = TestDataGenerator.generateDepositForTransfer();
        DepositRequest depositRequest = DepositRequest.builder()
                .id(senderAccount.getId())
                .balance(depositAmount)
                .build();

        UserSteps.makeDeposit(user, depositRequest, ResponseSpecs.requestReturnsOK());

        // Get accounts after deposit
        CreateAccountResponse[] accountsAfterDeposit = UserSteps.getAccounts(user);

        //verify the deposit on sender's account is successful
        CreateAccountResponse senderAfterDeposit = Arrays.stream(accountsAfterDeposit)
                .filter(account -> account.getAccountNumber().equals(senderAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        assertEquals(depositAmount, senderAfterDeposit.getBalance());

        CreateAccountResponse recipientAfterDeposit = Arrays.stream(accountsAfterDeposit)
                .filter(account -> account.getAccountNumber().equals(recipientAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        assertEquals(0.00, recipientAfterDeposit.getBalance());

        double firstTransferAmount = TestDataGenerator.generateTransferAmount(depositAmount / 2);

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(senderAccount.getId())
                .receiverAccountId(recipientAccount.getId())
                .amount(firstTransferAmount)
                .build();

        UserSteps.makeTransfer(user, transferRequest, ResponseSpecs.requestReturnsOK());

        // Get balances after first transfer
        CreateAccountResponse[] accountsAfterFirstTransfer = UserSteps.getAccounts(user);

        CreateAccountResponse senderAfterFirstTransfer = Arrays.stream(accountsAfterFirstTransfer)
                .filter(account -> account.getAccountNumber().equals(senderAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        CreateAccountResponse recipientAfterFirstTransfer = Arrays.stream(accountsAfterFirstTransfer)
                .filter(account -> account.getAccountNumber().equals(recipientAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        //verify if the transfer was made via API
        assertEquals(senderAfterDeposit.getBalance() - firstTransferAmount, senderAfterFirstTransfer.getBalance());
        assertEquals(recipientAfterDeposit.getBalance() + firstTransferAmount, recipientAfterFirstTransfer.getBalance());

        //get user token and save in local Storage
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

        //save the token in localstorage
        executeJavaScript("localStorage.setItem('authToken', arguments[0])", userAuthHeader);//saved in local storage
        Selenide.open("/dashboard");

        //Test steps
        $$("button").findBy(exactText("\uD83D\uDD04 Make a Transfer")).click();
        webdriver().shouldHave(urlContaining("/transfer"));
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(Condition.visible);

        $$("button").findBy(exactText("\uD83D\uDD01 Transfer Again")).click();
        $("input[placeholder='Enter name to find transactions']").setValue(profileName);
        $$("button").findBy(exactText("\uD83D\uDD0D Search Transactions")).click();

        String transferText = String.format(Locale.US, "TRANSFER_OUT - $%.2f", firstTransferAmount);

        $("ul.list-group").$$("li")
                .findBy(Condition.text(transferText))
                .$$("button").findBy(exactText("🔁 Repeat")).click();

        //open modal
        $("div.modal-title.h4").shouldBe(Condition.visible).shouldHave(exactText("🔁 Repeat Transfer"));
        //choose sender's account by id
        $(".modal.show select.form-control").selectOptionByValue(String.valueOf(senderAccount.getId()));
        //the last transfer amount is already in the input
        $(".modal.show input[type='number']").shouldHave(Condition.value(String.valueOf(firstTransferAmount)));
        $("#confirmCheck").setSelected(true);
        $(".modal.show").$$("button").findBy(exactText("🚀 Send Transfer")).click();

        String alertText = switchTo().alert().getText();

        assertEquals(String.format(AlertMessage.REPEAT_TRANSFER_SUCCESSFUL_UI.getMessage(), firstTransferAmount,
                senderAccount.getId(), recipientAccount.getId()), alertText);

        switchTo().alert().accept();

        CreateAccountResponse[] accountsAfterTransfer = UserSteps.getAccounts(user);

        CreateAccountResponse senderAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getAccountNumber().equals(senderAccount.getAccountNumber()))
                .findFirst()
                .orElseThrow();

        CreateAccountResponse recipientAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getAccountNumber().equals(recipientAccount.getAccountNumber()))
                .findFirst()
                .orElseThrow();

        assertEquals(senderAfterFirstTransfer.getBalance() - firstTransferAmount, senderAfterTransfer.getBalance());
        assertEquals(recipientAfterFirstTransfer.getBalance() + firstTransferAmount, recipientAfterTransfer.getBalance());
    }

    @DisplayName("TC-21: Repeat transfer with invalid amount")
    @ParameterizedTest
    @ValueSource(doubles = {0.0, -1.0, 10000.01})
    public void userCannotRepeatTransferWithInvalidAmountViaUiTest(double invalidAmount) {
        //prepare test data
        CreateUserRequest user = AdminSteps.createUser();
        CreateAccountResponse senderAccount = UserSteps.createAccount(user);
        CreateAccountResponse recipientAccount = UserSteps.createAccount(user);

        CreateAccountResponse[] accountsBeforeDeposit = UserSteps.getAccounts(user);
        assertEquals(2, accountsBeforeDeposit.length);
        assertEquals(0.0, accountsBeforeDeposit[0].getBalance());
        assertEquals(0.0, accountsBeforeDeposit[1].getBalance());

        //set profile name
        String profileName = TestDataGenerator.generateProfileName();

        ChangeProfileRequest changeProfileRequest = ChangeProfileRequest.builder()
                .name(profileName)
                .build();

        UserSteps.updateProfile(user, changeProfileRequest, ResponseSpecs.requestReturnsOK());

        //make three deposits
        double depositAmount = 5000.00;

        DepositRequest depositRequest = DepositRequest.builder()
                .id(senderAccount.getId())
                .balance(depositAmount)
                .build();

        UserSteps.makeDeposit(user, depositRequest, ResponseSpecs.requestReturnsOK());
        UserSteps.makeDeposit(user, depositRequest, ResponseSpecs.requestReturnsOK());
        UserSteps.makeDeposit(user, depositRequest, ResponseSpecs.requestReturnsOK());

        //get accounts after deposit
        CreateAccountResponse[] accountsAfterDeposit = UserSteps.getAccounts(user);

        CreateAccountResponse senderAfterDeposit = Arrays.stream(accountsAfterDeposit)
                .filter(account -> account.getAccountNumber().equals(senderAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        CreateAccountResponse recipientAfterDeposit = Arrays.stream(accountsAfterDeposit)
                .filter(account -> account.getAccountNumber().equals(recipientAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        assertEquals(depositAmount * 3, senderAfterDeposit.getBalance());
        assertEquals(0.00, recipientAfterDeposit.getBalance());

        //make first successful transfer via API
        double firstTransferAmount = TestDataGenerator.generateTransferAmount(1000.00);

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(senderAccount.getId())
                .receiverAccountId(recipientAccount.getId())
                .amount(firstTransferAmount)
                .build();

        UserSteps.makeTransfer(user, transferRequest, ResponseSpecs.requestReturnsOK());

        //get balances after first transfer
        CreateAccountResponse[] accountsAfterFirstTransfer = UserSteps.getAccounts(user);

        CreateAccountResponse senderAfterFirstTransfer = Arrays.stream(accountsAfterFirstTransfer)
                .filter(account -> account.getAccountNumber().equals(senderAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        CreateAccountResponse recipientAfterFirstTransfer = Arrays.stream(accountsAfterFirstTransfer)
                .filter(account -> account.getAccountNumber().equals(recipientAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        assertEquals(senderAfterDeposit.getBalance() - firstTransferAmount, senderAfterFirstTransfer.getBalance());
        assertEquals(recipientAfterDeposit.getBalance() + firstTransferAmount, recipientAfterFirstTransfer.getBalance());

        //get user token and save in local storage
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

        //test steps
        $$("button").findBy(exactText("🔄 Make a Transfer")).click();
        webdriver().shouldHave(urlContaining("/transfer"));
        $(Selectors.byText("🔄 Make a Transfer")).shouldBe(Condition.visible);

        $$("button").findBy(exactText("🔁 Transfer Again")).click();
        $("input[placeholder='Enter name to find transactions']").setValue(profileName);
        $$("button").findBy(exactText("🔍 Search Transactions")).click();

        String transferText = String.format(Locale.US, "TRANSFER_OUT - $%.2f", firstTransferAmount);

        $("ul.list-group").$$("li")
                .findBy(Condition.text(transferText))
                .$$("button").findBy(exactText("🔁 Repeat")).click();

        //open modal
        $("div.modal-title.h4").shouldBe(Condition.visible).shouldHave(exactText("🔁 Repeat Transfer"));

        //choose sender's account
        $(".modal.show select.form-control").selectOptionByValue(String.valueOf(senderAccount.getId()));

        //replace the original transfer amount with an invalid amount
        $(".modal.show input[type='number']").setValue(String.valueOf(invalidAmount));

        $("#confirmCheck").setSelected(true);
        $(".modal.show").$$("button").findBy(exactText("🚀 Send Transfer")).click();

        //verify error message
        String alertText = switchTo().alert().getText();
        assertEquals(AlertMessage.TRANSFER_FAILED_UI.getMessage(), alertText);
        switchTo().alert().accept();

        //verify account balances have not changed
        CreateAccountResponse[] accountsAfterFailedTransfer = UserSteps.getAccounts(user);

        CreateAccountResponse senderAfterFailedTransfer = Arrays.stream(accountsAfterFailedTransfer)
                .filter(account -> account.getAccountNumber().equals(senderAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        CreateAccountResponse recipientAfterFailedTransfer = Arrays.stream(accountsAfterFailedTransfer)
                .filter(account -> account.getAccountNumber().equals(recipientAccount.getAccountNumber()))
                .findFirst().orElseThrow();

        assertEquals(senderAfterFirstTransfer.getBalance(), senderAfterFailedTransfer.getBalance());
        assertEquals(recipientAfterFirstTransfer.getBalance(), recipientAfterFailedTransfer.getBalance());
    }
}