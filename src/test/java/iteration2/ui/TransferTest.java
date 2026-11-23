package iteration2.ui;

import api.enums.AlertMessage;
import api.generators.TestDataGenerator;
import api.models.*;
import api.specs.ResponseSpecs;
import api.steps.AdminSteps;
import api.steps.UserSteps;
import iteration1.ui.BaseUiTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ui.pages.TransferPage;
import ui.pages.UserDashboardPage;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransferTest extends BaseUiTest {

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

        authAsUser(user);

        TransferPage transferPage = new UserDashboardPage()
                .open()
                .openTransfer();

        transferPage.selectSenderAccount(senderAccount.getAccountNumber())
                .fillTransferForm(profileName, recipientAccount.getAccountNumber(), amount)
                .confirmTransfer()
                .submitTransfer()
                .checkAlertMessageAndAccept(String.format(AlertMessage.TRANSFER_SUCCESSFUL_UI.getMessage(), amount, recipientAccount.getAccountNumber()));

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

        authAsUser(user);

        TransferPage transferPage = new UserDashboardPage()
                .open()
                .openTransfer()
                .shouldShowTransferPage();


        if (!emptyField.equals("senderAccount")) {
            transferPage.selectSenderAccount(senderAccount.getAccountNumber());
        }

        if (!emptyField.equals("recipientName")) {
            transferPage.enterRecipientName(profileName);
        }

        if (!emptyField.equals("recipientAccountNumber")) {
            transferPage.enterRecipientAccountNumber(recipientAccount.getAccountNumber());
        }

        if (!emptyField.equals("amount")) {
            transferPage.enterAmount(transferAmount);
        }
        String expectedAlertMessage = emptyField.equals("recipientName")
                ? AlertMessage.RECIPIENT_NAME_MISMATCH.getMessage()
                : AlertMessage.REQUIRED_FIELDS_NOT_FILLED.getMessage();


        transferPage.confirmTransfer()
                .submitTransfer()
                .checkAlertMessageAndAccept(expectedAlertMessage);


// Get account balances after transfer
        CreateAccountResponse[] accountsAfterTransfer = UserSteps.getAccounts(user);

        CreateAccountResponse senderAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getAccountNumber().equals(senderAccount.getAccountNumber()))
                .findFirst()
                .orElseThrow();

        CreateAccountResponse recipientAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getAccountNumber().equals(recipientAccount.getAccountNumber()))
                .findFirst()
                .orElseThrow();

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


        authAsUser(user);

        TransferPage transferPage = new UserDashboardPage()
                .open()
                .openTransfer()
                .shouldShowTransferPage();

        transferPage.selectSenderAccount(senderAccount.getAccountNumber())
                .fillTransferForm(profileName, recipientAccount.getAccountNumber(), transferAmount)
                .submitTransfer()
                .checkAlertMessageAndAccept(AlertMessage.REQUIRED_FIELDS_NOT_FILLED.getMessage());


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

        authAsUser(user);

        TransferPage transferPage = new UserDashboardPage()
                .open()
                .openTransfer()
                .shouldShowTransferPage();

        transferPage.selectSenderAccount(userAccount.getAccountNumber())
                .shouldHaveSelectedSenderAccount(userAccount.getId())
                .fillTransferForm(profileName, userAccount.getAccountNumber(), transferAmount)
                .confirmTransfer()
                .submitTransfer()
                .checkAlertMessageAndAccept(AlertMessage.INVALID_TRANSFER.getMessage());

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


        authAsUser(user);

        TransferPage transferPage = new UserDashboardPage()
                .open()
                .openTransfer()
                .shouldShowTransferPage();

        transferPage.selectSenderAccount(senderAccount.getAccountNumber())
                .fillTransferForm(TestDataGenerator.generateProfileName(), recipientAccount.getAccountNumber(), transferAmount)
                .confirmTransfer()
                .submitTransfer()
                .checkAlertMessageAndAccept(AlertMessage.INCORRECT_TRANSFER_NAME.getMessage());

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

        authAsUser(user);

        TransferPage transferPage = new UserDashboardPage()
                .open()
                .openTransfer()
                .shouldShowTransferPage();

        transferPage.selectSenderAccount(senderAccount.getAccountNumber())
                .fillTransferForm(profileName, recipientAccount.getAccountNumber(), transferAmount)
                .confirmTransfer()
                .submitTransfer()
                .checkAlertMessageAndAccept(AlertMessage.TRANSFER_AMOUNT_EXCEEDS_LIMIT_UI.getMessage());

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

        authAsUser(user);

        TransferPage transferPage = new UserDashboardPage()
                .open()
                .openTransfer()
                .shouldShowTransferPage();

        transferPage.selectSenderAccount(senderAccount.getAccountNumber())
                .fillTransferForm(profileName, recipientAccount.getAccountNumber(), transferAmount)
                .confirmTransfer()
                .submitTransfer()
                .checkAlertMessageAndAccept(AlertMessage.INVALID_TRANSFER.getMessage());

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

        String nonExistingAccountNumber = TestDataGenerator.generateNonExistingAccountNumber();

        authAsUser(user);

        TransferPage transferPage = new UserDashboardPage()
                .open()
                .openTransfer()
                .shouldShowTransferPage();

        transferPage.selectSenderAccount(senderAccount.getAccountNumber())
                .fillTransferForm(profileName, nonExistingAccountNumber, transferAmount)
                .confirmTransfer()
                .submitTransfer()
                .checkAlertMessageAndAccept(AlertMessage.RECIPIENT_ACCOUNT_NOT_FOUND.getMessage());

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

        authAsUser(user);

        TransferPage transferPage = new UserDashboardPage()
                .open()
                .openTransfer()
                .shouldShowTransferPage();


        transferPage.openTransferAgain()
                .searchTransactionByName(profileName)
                .searchTransactions()
                .repeatTransaction(firstTransferAmount)
                .shouldShowRepeatTransferModal()
                .selectModalSenderAccount(senderAccount.getId())
                .shouldHaveRepeatAmount(firstTransferAmount)
                .confirmTransfer()
                .submitRepeatTransfer()
                .checkAlertMessageAndAccept(String.format(AlertMessage.REPEAT_TRANSFER_SUCCESSFUL_UI.getMessage(), firstTransferAmount, senderAccount.getId(), recipientAccount.getId()));

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

        authAsUser(user);

        TransferPage transferPage = new UserDashboardPage()
                .open()
                .openTransfer()
                .shouldShowTransferPage();

        transferPage.openTransferAgain()
                .searchTransactionByName(profileName)
                .searchTransactions()
                .repeatTransaction(firstTransferAmount)
                .shouldShowRepeatTransferModal()
                .selectModalSenderAccount(senderAccount.getId())
                .enterRepeatTransferAmount(invalidAmount)
                .confirmTransfer()
                .submitRepeatTransfer()
                .checkAlertMessageAndAccept(AlertMessage.TRANSFER_FAILED_UI.getMessage());

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