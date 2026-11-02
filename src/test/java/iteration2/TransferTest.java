package iteration2;

import enums.AlertMessage;
import generators.TestDataGenerator;
import models.*;
import models.comparison.ModelAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;
import specs.ResponseSpecs;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransferTest {

    @ParameterizedTest
    @ValueSource(doubles = {0.01, 9999.99, 10000.00})
    public void userCanTransferBetweenOwnAccountsTest(double amount) {
        CreateUserRequest userRequest = AdminSteps.createUser();

        // Create source account
        CreateAccountResponse sourceAccount = UserSteps.createAccount(userRequest);

        long sourceAccountId = sourceAccount.getId();

        //create destination account
        CreateAccountResponse destinationAccount = UserSteps.createAccount(userRequest);

        long destinationAccountId = destinationAccount.getId();

        double deposit1 = TestDataGenerator.generateDepositForTransfer();
        double deposit2 = TestDataGenerator.generateDepositForTransfer();
        double deposit3 = TestDataGenerator.generateDepositForTransfer();

        for (double depositAmount : new double[]{deposit1, deposit2, deposit3}) {
            DepositRequest depositRequest = DepositRequest.builder()
                    .id(sourceAccountId)
                    .balance(depositAmount)
                    .build();

            UserSteps.makeDeposit(
                    userRequest,
                    depositRequest,
                    ResponseSpecs.requestReturnsOK());
        }

        double totalDepositAmount = deposit1 + deposit2 + deposit3;
        double expectedSourceBalance = totalDepositAmount - amount;
        double expectedDestinationBalance = amount;

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(sourceAccountId)
                .receiverAccountId(destinationAccountId)
                .amount(amount)
                .build();


        TransferResponse transferResponse = UserSteps.makeTransfer(
                        userRequest,
                        transferRequest,
                        ResponseSpecs.requestReturnsOK())
                .extract()
                .as(TransferResponse.class);

        assertEquals(AlertMessage.TRANSFER_SUCCESSFUL.getMessage(), transferResponse.getMessage());
        ModelAssertions.assertThatModels(transferRequest, transferResponse).match();

        CreateAccountResponse[] accountsAfterTransfer = UserSteps.getAccounts(userRequest);

        CreateAccountResponse sourceAccountAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getId() == sourceAccountId)
                .findFirst()
                .orElseThrow();

        CreateAccountResponse destinationAccountAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getId() == destinationAccountId)
                .findFirst()
                .orElseThrow();

        assertEquals(expectedSourceBalance, sourceAccountAfterTransfer.getBalance());
        assertEquals(expectedDestinationBalance, destinationAccountAfterTransfer.getBalance());
    }

    @Test
    public void userCanMakeTransferToAnotherUsersAccountTest() {
        //create userA und B
        CreateUserRequest userARequest = AdminSteps.createUser();
        CreateUserRequest userBRequest = AdminSteps.createUser();

        //create source account
        CreateAccountResponse sourceAccount = UserSteps.createAccount(userARequest);

        long sourceAccountId = sourceAccount.getId();

        double depositAmount = TestDataGenerator.generateDepositAmount();

        DepositRequest depositRequest = DepositRequest.builder()
                .id(sourceAccountId)
                .balance(depositAmount)
                .build();

        UserSteps.makeDeposit(
                userARequest,
                depositRequest,
                ResponseSpecs.requestReturnsOK());

        // Create destination account for user B
        CreateAccountResponse destinationAccount = UserSteps.createAccount(userBRequest);

        long destinationAccountId = destinationAccount.getId();

        double transferAmount = depositAmount;//we transfer all deposited money

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(sourceAccountId)
                .receiverAccountId(destinationAccountId)
                .amount(transferAmount)
                .build();

        TransferResponse transferResponse = UserSteps.makeTransfer(
                        userARequest,
                        transferRequest,
                        ResponseSpecs.requestReturnsOK())
                .extract()
                .as(TransferResponse.class);

        assertEquals(AlertMessage.TRANSFER_SUCCESSFUL.getMessage(), transferResponse.getMessage());
        ModelAssertions.assertThatModels(transferRequest, transferResponse).match();

        //check user A balance
        double expectedUserABalance = depositAmount - transferAmount;

        CreateAccountResponse[] sourceAccountsAfterTransfer =
                UserSteps.getAccounts(userARequest);

        CreateAccountResponse sourceAccountAfterTransfer = sourceAccountsAfterTransfer[0];

        assertEquals(expectedUserABalance, sourceAccountAfterTransfer.getBalance());//expect 0

        //Check user B balance
        double expectedUserBBalance = transferAmount;

        CreateAccountResponse[] destinationAccountsAfterTransfer =
                UserSteps.getAccounts(userBRequest);

        CreateAccountResponse destinationAccountAfterTransfer = destinationAccountsAfterTransfer[0];
        assertEquals(expectedUserBBalance, destinationAccountAfterTransfer.getBalance());
    }

    @ParameterizedTest
    @ValueSource(doubles = {10000.01})
    public void userCannotTransferAboveMaximumAmountTest(double transferAmount) {

        CreateUserRequest userRequest = AdminSteps.createUser();

        //create source account and retrieve id
        CreateAccountResponse sourceAccount = UserSteps.createAccount(userRequest);

        long sourceAccountId = sourceAccount.getId();

        //create destination account and retrieve id
        CreateAccountResponse destinationAccount = UserSteps.createAccount(userRequest);
        long destinationAccountId = destinationAccount.getId();


        double deposit1 = TestDataGenerator.generateDepositForTransfer();
        double deposit2 = TestDataGenerator.generateDepositForTransfer();
        double deposit3 = TestDataGenerator.generateDepositForTransfer();

        //making deposits
        for (double depositAmount : new double[]{deposit1, deposit2, deposit3}) {
            DepositRequest depositRequest = DepositRequest.builder()
                    .id(sourceAccountId)
                    .balance(depositAmount)
                    .build();

            UserSteps.makeDeposit(
                    userRequest,
                    depositRequest,
                    ResponseSpecs.requestReturnsOK());
        }

        double totalDepositAmount = deposit1 + deposit2 + deposit3;
        double expectedSourceBalance = totalDepositAmount;
        double expectedDestinationBalance = 0;//🐝 Expected balance according to the test scenario, not arbitrary test data.

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(sourceAccountId)
                .receiverAccountId(destinationAccountId)
                .amount(transferAmount)
                .build();

        UserSteps.makeTransfer(
                userRequest,
                transferRequest,
                ResponseSpecs.requestReturnsBadRequestWithText(
                        AlertMessage.TRANSFER_AMOUNT_EXCEEDS_LIMIT.getMessage()));

        CreateAccountResponse[] accountsAfterTransfer =
                UserSteps.getAccounts(userRequest);

        CreateAccountResponse sourceAccountAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getId() == sourceAccountId)
                .findFirst()
                .orElseThrow();

        CreateAccountResponse destinationAccountAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getId() == destinationAccountId)
                .findFirst()
                .orElseThrow();

        assertEquals(expectedSourceBalance, sourceAccountAfterTransfer.getBalance());
        assertEquals(expectedDestinationBalance, destinationAccountAfterTransfer.getBalance());
    }

    @Test
    public void userCannotTransferNegativeAmountTest() {
        CreateUserRequest userRequest = AdminSteps.createUser();
        // Create source account
        CreateAccountResponse sourceAccount = UserSteps.createAccount(userRequest);
        long sourceAccountId = sourceAccount.getId();

        // Create destination account
        CreateAccountResponse destinationAccount = UserSteps.createAccount(userRequest);
        long destinationAccountId = destinationAccount.getId();


        double depositAmount = TestDataGenerator.generateDepositAmount();
        double transferAmount = TestDataGenerator.generateNegativeTransferAmount(depositAmount);

        DepositRequest depositRequest = DepositRequest.builder()
                .id(sourceAccountId)
                .balance(depositAmount)
                .build();

        UserSteps.makeDeposit(
                userRequest,
                depositRequest,
                ResponseSpecs.requestReturnsOK());

        double expectedSourceBalance = depositAmount;
        double expectedDestinationBalance = 0;//🐝 Expected balance is 0 because the destination account is newly created
// and the rejected transfer must not change its balance.

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(sourceAccountId)
                .receiverAccountId(destinationAccountId)
                .amount(transferAmount)
                .build();

        UserSteps.makeTransfer(
                userRequest,
                transferRequest,
                ResponseSpecs.requestReturnsBadRequestWithText(
                        AlertMessage.TRANSFER_AMOUNT_TOO_LOW.getMessage()));

        CreateAccountResponse[] accountsAfterTransfer =
                UserSteps.getAccounts(userRequest);

        CreateAccountResponse sourceAccountAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getId() == sourceAccountId)
                .findFirst()
                .orElseThrow();


        CreateAccountResponse destinationAccountAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getId() == destinationAccountId)
                .findFirst()
                .orElseThrow();

        assertEquals(expectedSourceBalance, sourceAccountAfterTransfer.getBalance());
        assertEquals(expectedDestinationBalance, destinationAccountAfterTransfer.getBalance());
    }

    @Test
    public void userCannotTransferMoreThanAvailableBalanceTest() {
        CreateUserRequest userRequest = AdminSteps.createUser();

        // Create source account
        CreateAccountResponse sourceAccount = UserSteps.createAccount(userRequest);
        long sourceAccountId = sourceAccount.getId();

        // Create destination account
        CreateAccountResponse destinationAccount = UserSteps.createAccount(userRequest);
        long destinationAccountId = destinationAccount.getId();

        double depositAmount = TestDataGenerator.generateDepositAmount();
        double transferAmount = TestDataGenerator.generateTransferAmountAboveBalance(depositAmount);

        DepositRequest depositRequest = DepositRequest.builder()
                .id(sourceAccountId)
                .balance(depositAmount)
                .build();

        UserSteps.makeDeposit(
                userRequest,
                depositRequest,
                ResponseSpecs.requestReturnsOK());

        double expectedSourceBalance = depositAmount;
        double expectedDestinationBalance = 0;// 🐝 Expected balance is 0 because the destination account is newly created
// and the rejected transfer must not change its balance. QUOTE: "фиксированные значения оставляем только там, где конкретное значение является частью самого тестового сценария или необходимо для проверки граничного случая."

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(sourceAccountId)
                .receiverAccountId(destinationAccountId)
                .amount(transferAmount)
                .build();

        UserSteps.makeTransfer(
                userRequest,
                transferRequest,
                ResponseSpecs.requestReturnsBadRequestWithText(
                        AlertMessage.INSUFFICIENT_FUNDS.getMessage()));

        CreateAccountResponse[] accountsAfterTransfer =
                UserSteps.getAccounts(userRequest);

        CreateAccountResponse sourceAccountAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getId() == sourceAccountId)
                .findFirst()
                .orElseThrow();

        CreateAccountResponse destinationAccountAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getId() == destinationAccountId)
                .findFirst()
                .orElseThrow();

        assertEquals(expectedSourceBalance, sourceAccountAfterTransfer.getBalance());
        assertEquals(expectedDestinationBalance, destinationAccountAfterTransfer.getBalance());
    }
}



