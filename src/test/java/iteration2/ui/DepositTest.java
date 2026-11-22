package iteration2.ui;

import api.enums.AlertMessage;
import api.generators.TestDataGenerator;
import api.models.CreateAccountResponse;
import api.models.CreateUserRequest;
import api.steps.AdminSteps;
import api.steps.UserSteps;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.WebDriverRunner;
import iteration1.ui.BaseUiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ui.pages.UserDashboard;

import java.util.concurrent.atomic.AtomicInteger;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.WebDriverRunner.getSelenideProxy;
import static com.codeborne.selenide.proxy.RequestMatcher.HttpMethod.POST;
import static com.codeborne.selenide.proxy.RequestMatchers.urlEndsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DepositTest extends BaseUiTest {

    @DisplayName("TC-01: Successful deposit with valid amounts")
    @ParameterizedTest
    @ValueSource(doubles = {5000, 0.01})
    public void userCanMakeDepositViaUiWithValidAmountTest(double amount) {
        //1 . Prepare the test data via API:
        CreateUserRequest user = AdminSteps.createUser();
        CreateAccountResponse account = UserSteps.createAccount(user);
        CreateAccountResponse[] accountsBeforeDeposit = UserSteps.getAccounts(user);
        assertEquals(1, accountsBeforeDeposit.length);
        assertEquals(0.0, accountsBeforeDeposit[0].getBalance());

        authAsUser(user);

        new UserDashboard()
                .open()
                .makeDeposit(amount)
                .checkAlertMessageAndAccept(String.format(AlertMessage.SUCCESSFUL_DEPOSIT.getMessage(), amount, account.getAccountNumber()));


        CreateAccountResponse[] accountsAfterDeposit = UserSteps.getAccounts(user);

        assertEquals(1, accountsAfterDeposit.length);

        assertEquals(accountsBeforeDeposit[0].getBalance() + amount,
                accountsAfterDeposit[0].getBalance());
    }

    @DisplayName("TC-02: Deposit amount exceeds 5000")
    @ParameterizedTest
    @ValueSource(doubles = {5000.01})
    public void userCannotMakeDepositAboveMaximumAmountViaUiTest(double amount) {
        //1 . Prepare the test data via API:
        CreateUserRequest userRequest = AdminSteps.createUser();
        UserSteps.createAccount(userRequest);
        CreateAccountResponse[] accountsBeforeDeposit = UserSteps.getAccounts(userRequest);
        assertEquals(1, accountsBeforeDeposit.length);
        assertEquals(0.0, accountsBeforeDeposit[0].getBalance());

        authAsUser(userRequest);

        new UserDashboard()
                .open()
                .makeDeposit(amount)
                .checkAlertMessageAndAccept(AlertMessage.DEPOSIT_AMOUNT_TOO_HIGH_UI.getMessage());


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

        authAsUser(userRequest);

        new UserDashboard()
                .open()
                .makeDeposit(amount)
                .checkAlertMessageAndAccept(AlertMessage.INVALID_DEPOSIT_AMOUNT.getMessage());

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

        authAsUser(userRequest);

        double amount = TestDataGenerator.generateDepositAmount();

        new UserDashboard()
                .open()
                .makeDepositWithoutSelectingAccount(amount)
                .checkAlertMessageAndAccept(AlertMessage.ACCOUNT_NOT_SELECTED.getMessage());

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

        authAsUser(userRequestA);

        new UserDashboard()
                .open()
                .openDepositForm()
                .shouldSeeAccount(accountA.getAccountNumber())
                .shouldNotSeeAccount(accountB.getAccountNumber());
    }

    @DisplayName("TC-06: Error handling when deposit fails")
    @Test
    public void userSeesErrorWhenDepositFailsViaUiTest() {
        CreateUserRequest userRequest = AdminSteps.createUser();
        UserSteps.createAccount(userRequest);

        CreateAccountResponse[] accountsBeforeDeposit = UserSteps.getAccounts(userRequest);
        assertEquals(1, accountsBeforeDeposit.length);
        assertEquals(0.0, accountsBeforeDeposit[0].getBalance());

        // Authenticate and fill deposit form
        authAsUser(userRequest);

        double amount = TestDataGenerator.generateDepositAmount();

        UserDashboard dashboard = new UserDashboard().open().fillDepositForm(amount);

        //сохраняем URL до депозита
        String urlBeforeDeposit = WebDriverRunner.url();

        AtomicInteger mockedRequests = new AtomicInteger(0);

        // Mock server error
        getSelenideProxy().responseMocker().mockText(
                "deposit-error",
                urlEndsWith(POST, "/accounts/deposit"),
                500,
                () -> {
                    mockedRequests.incrementAndGet();
                    return "{\"error\":\"Internal Server Error\"}";
                }
        );
        //Submit deposit and verify error message
        dashboard.submitDeposit()
                .checkAlertMessageAndAccept(AlertMessage.DEPOSIT_UNSUCCESSFUL.getMessage());


        // Verify that Selenide Proxy intercepted the deposit request exactly once
        assertEquals(1, mockedRequests.get());

        //Verify that the user remains on the deposit page
        assertEquals(urlBeforeDeposit, WebDriverRunner.url());
        dashboard.shouldShowDepositForm();

        // Verify that account balance remains unchanged after failed deposit
        CreateAccountResponse[] accountsAfterDeposit = UserSteps.getAccounts(userRequest);

        assertEquals(1, accountsAfterDeposit.length);
        assertEquals(accountsBeforeDeposit[0].getBalance(), accountsAfterDeposit[0].getBalance());
    }
}