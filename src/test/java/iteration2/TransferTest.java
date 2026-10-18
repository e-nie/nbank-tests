package iteration2;

import enums.AlertMessage;
import generators.RandomData;
import generators.TestDataGenerator;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import models.*;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import requests.*;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransferTest {

    @ParameterizedTest
    @ValueSource(doubles = {0.01, 9999.99, 10000.00})
    public void userCanTransferBetweenOwnAccountsTest(double amount) {

        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())//
                .createUser(userRequest);

        RequestSpecification userSpec = RequestSpecs.authAsUser(
                userRequest.getUsername(),
                userRequest.getPassword());

        //create source account
        new CreateAccountRequester(userSpec, ResponseSpecs.entityWasCreated())
                .createAccount();

        AccountResponse[] sourceAccount = new GetAccountsRequester(
                userSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .as(AccountResponse[].class);

        int sourceAccountId = sourceAccount[0].getId();

        //create destination account
        new CreateAccountRequester(userSpec, ResponseSpecs.entityWasCreated())
                .createAccount();

        AccountResponse[] destinationAccount = new GetAccountsRequester(
                userSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .as(AccountResponse[].class);

        int destinationAccountId = Arrays.stream(destinationAccount)
                .filter(account -> account.getId() != sourceAccountId)
                .findFirst()
                .orElseThrow().getId();

        double deposit1 = TestDataGenerator.generateDepositForTransfer();
        double deposit2 = TestDataGenerator.generateDepositForTransfer();
        double deposit3 = TestDataGenerator.generateDepositForTransfer();

        for (double depositAmount : new double[]{deposit1, deposit2, deposit3}) {
            DepositRequest depositRequest = DepositRequest.builder()
                    .id(sourceAccountId)
                    .balance(depositAmount)
                    .build();

            new MakeDepositRequester(
                    userSpec,
                    ResponseSpecs.requestReturnsOK())
                    .makeDeposit(depositRequest);
        }

        double totalDepositAmount = deposit1 + deposit2 + deposit3;
        double expectedSourceBalance = totalDepositAmount - amount;
        double expectedDestinationBalance = amount;

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(sourceAccountId)
                .receiverAccountId(destinationAccountId)
                .amount(amount)
                .build();


        TransferResponse transferResponse = new TransferRequester(userSpec, ResponseSpecs.requestReturnsOK())
                .makeTransfer(transferRequest)
                .extract()
                .as(TransferResponse.class);

        assertEquals(AlertMessage.TRANSFER_SUCCESSFUL.getMessage(), transferResponse.getMessage());
        assertEquals(sourceAccountId, transferResponse.getSenderAccountId());
        assertEquals(destinationAccountId, transferResponse.getReceiverAccountId());
        assertEquals(amount, transferResponse.getAmount());

        AccountResponse[] accountsAfterTransfer = new GetAccountsRequester(userSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .as(AccountResponse[].class);

        AccountResponse sourceAccountAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getId() == sourceAccountId)
                .findFirst()
                .orElseThrow();


        AccountResponse destinationAccountAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getId() == destinationAccountId)
                .findFirst()
                .orElseThrow();

        assertEquals(expectedSourceBalance, sourceAccountAfterTransfer.getBalance());
        assertEquals(expectedDestinationBalance, destinationAccountAfterTransfer.getBalance());
    }

    @Test
    public void userCanMakeTransferToAnotherUsersAccountTest() {
        //create userA
        CreateUserRequest userARequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())//
                .createUser(userARequest);

        RequestSpecification userASpec = RequestSpecs.authAsUser(
                userARequest.getUsername(),
                userARequest.getPassword());

        //create source account
        new CreateAccountRequester(userASpec, ResponseSpecs.entityWasCreated())
                .createAccount();

        AccountResponse[] sourceAccount = new GetAccountsRequester(
                userASpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .as(AccountResponse[].class);

        int sourceAccountId = sourceAccount[0].getId();

        double depositAmount = TestDataGenerator.generateDepositAmount();

        DepositRequest depositRequest = DepositRequest.builder()
                .id(sourceAccountId)
                .balance(depositAmount)
                .build();

        new MakeDepositRequester(
                userASpec,
                ResponseSpecs.requestReturnsOK())
                .makeDeposit(depositRequest);

        //Create Receiver
        CreateUserRequest userBRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())//
                .createUser(userBRequest);

        RequestSpecification userBSpec = RequestSpecs.authAsUser(
                userBRequest.getUsername(),
                userBRequest.getPassword());

        //create destination account
        new CreateAccountRequester(userBSpec, ResponseSpecs.entityWasCreated())
                .createAccount();

        AccountResponse[] destinationAccount = new GetAccountsRequester(
                userBSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .as(AccountResponse[].class);

        int destinationAccountId = destinationAccount[0].getId();

        double transferAmount = depositAmount;//we transfer all deposited money

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(sourceAccountId)
                .receiverAccountId(destinationAccountId)
                .amount(transferAmount)
                .build();

        TransferResponse transferResponse = new TransferRequester(userASpec, ResponseSpecs.requestReturnsOK())
                .makeTransfer(transferRequest)
                .extract()
                .as(TransferResponse.class);

        assertEquals(AlertMessage.TRANSFER_SUCCESSFUL.getMessage(), transferResponse.getMessage());
        assertEquals(sourceAccountId, transferResponse.getSenderAccountId());
        assertEquals(destinationAccountId, transferResponse.getReceiverAccountId());
        assertEquals(transferAmount, transferResponse.getAmount());

        //check user A balance
        double expectedUserABalance = depositAmount - transferAmount;

        AccountResponse[] sourceAccountsAfterTransfer = new GetAccountsRequester(userASpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .as(AccountResponse[].class);

        AccountResponse sourceAccountAfterTransfer = sourceAccountsAfterTransfer[0];
        assertEquals(expectedUserABalance, sourceAccountAfterTransfer.getBalance());

        //Check user B balance
        double expectedUserBBalance = transferAmount;
        AccountResponse[] destinationAccountsAfterTransfer = new GetAccountsRequester(userBSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .as(AccountResponse[].class);

        AccountResponse destinationAccountAfterTransfer = destinationAccountsAfterTransfer[0];
        assertEquals(expectedUserBBalance, destinationAccountAfterTransfer.getBalance());
    }

    @ParameterizedTest
    @ValueSource(doubles = {10000.01})
    public void userCannotTransferAboveMaximumAmountTest(double transferAmount) {
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())//
                .createUser(userRequest);

        RequestSpecification userSpec = RequestSpecs.authAsUser(
                userRequest.getUsername(),
                userRequest.getPassword());

        //create source account
        new CreateAccountRequester(userSpec, ResponseSpecs.entityWasCreated())
                .createAccount();

        AccountResponse[] sourceAccounts = new GetAccountsRequester(
                userSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .as(AccountResponse[].class);

        int sourceAccountId = sourceAccounts[0].getId();


        //create destination account
        new CreateAccountRequester(userSpec, ResponseSpecs.entityWasCreated())
                .createAccount();

        AccountResponse[] destinationAccounts = new GetAccountsRequester(
                userSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .as(AccountResponse[].class);

        int destinationAccountId = Arrays.stream(destinationAccounts)
                .filter(account -> account.getId() != sourceAccountId)
                .findFirst()
                .orElseThrow()
                .getId();

        double deposit1 = TestDataGenerator.generateDepositForTransfer();
        double deposit2 = TestDataGenerator.generateDepositForTransfer();
        double deposit3 = TestDataGenerator.generateDepositForTransfer();

        //making deposits
        for (double depositAmount : new double[]{deposit1, deposit2, deposit3}) {
            DepositRequest depositRequest = DepositRequest.builder()
                    .id(sourceAccountId)
                    .balance(depositAmount)
                    .build();

            new MakeDepositRequester(
                    userSpec,
                    ResponseSpecs.requestReturnsOK())
                    .makeDeposit(depositRequest);
        }

        double totalDepositAmount = deposit1 + deposit2 + deposit3;
        double expectedSourceBalance = totalDepositAmount;
        double expectedDestinationBalance = 0;//🐝 Expected balance according to the test scenario, not arbitrary test data.

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(sourceAccountId)
                .receiverAccountId(destinationAccountId)
                .amount(transferAmount)
                .build();

        new TransferRequester(userSpec, ResponseSpecs.requestReturnsBadRequestWithText(AlertMessage.TRANSFER_AMOUNT_EXCEEDS_LIMIT.getMessage()))
                .makeTransfer(transferRequest);

        AccountResponse[] accountsAfterTransfer = new GetAccountsRequester(userSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .as(AccountResponse[].class);

        AccountResponse sourceAccountAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getId() == sourceAccountId)
                .findFirst()
                .orElseThrow();

        AccountResponse destinationAccountAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getId() == destinationAccountId)
                .findFirst()
                .orElseThrow();

        assertEquals(expectedSourceBalance, sourceAccountAfterTransfer.getBalance());
        assertEquals(expectedDestinationBalance, destinationAccountAfterTransfer.getBalance());
    }

    @Test
    public void userCannotTransferNegativeAmountTest() {

        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())//
                .createUser(userRequest);

        RequestSpecification userSpec = RequestSpecs.authAsUser(
                userRequest.getUsername(),
                userRequest.getPassword());

        //create source account
        new CreateAccountRequester(userSpec, ResponseSpecs.entityWasCreated())
                .createAccount();

        AccountResponse[] sourceAccounts = new GetAccountsRequester(
                userSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .as(AccountResponse[].class);

        int sourceAccountId = sourceAccounts[0].getId();

        //create destination account
        new CreateAccountRequester(userSpec, ResponseSpecs.entityWasCreated())
                .createAccount();

        AccountResponse[] destinationAccounts = new GetAccountsRequester(
                userSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .as(AccountResponse[].class);

        int destinationAccountId = Arrays.stream(destinationAccounts)
                .filter(account -> account.getId() != sourceAccountId)
                .findFirst()
                .orElseThrow()
                .getId();


        double depositAmount = TestDataGenerator.generateDepositAmount();
        double transferAmount = TestDataGenerator.generateNegativeTransferAmount(depositAmount);

        DepositRequest depositRequest = DepositRequest.builder()
                .id(sourceAccountId)
                .balance(depositAmount)
                .build();

        new MakeDepositRequester(
                userSpec,
                ResponseSpecs.requestReturnsOK())
                .makeDeposit(depositRequest);

        double expectedSourceBalance = depositAmount;
        double expectedDestinationBalance = 0;//🐝 Expected balance is 0 because the destination account is newly created
// and the rejected transfer must not change its balance.

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(sourceAccountId)
                .receiverAccountId(destinationAccountId)
                .amount(transferAmount)
                .build();

        new TransferRequester(userSpec, ResponseSpecs.requestReturnsBadRequestWithText(AlertMessage.TRANSFER_AMOUNT_TOO_LOW.getMessage()))
                .makeTransfer(transferRequest);

        AccountResponse[] accountsAfterTransfer = new GetAccountsRequester(userSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .as(AccountResponse[].class);

        AccountResponse sourceAccountAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getId() == sourceAccountId)
                .findFirst()
                .orElseThrow();


        AccountResponse destinationAccountAfterTranfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getId() != sourceAccountId)
                .findFirst()
                .orElseThrow();

        assertEquals(expectedSourceBalance, sourceAccountAfterTransfer.getBalance());
        assertEquals(expectedDestinationBalance, destinationAccountAfterTranfer.getBalance());
    }

    @Test
    public void userCannotTransferMoreThanAvailableBalanceTest() {
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())//
                .createUser(userRequest);

        RequestSpecification userSpec = RequestSpecs.authAsUser(
                userRequest.getUsername(),
                userRequest.getPassword());

        //create source account
        new CreateAccountRequester(userSpec, ResponseSpecs.entityWasCreated())
                .createAccount();

        AccountResponse[] sourceAccounts = new GetAccountsRequester(
                userSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .as(AccountResponse[].class);

        int sourceAccountId = sourceAccounts[0].getId();


        //create destination account
        new CreateAccountRequester(userSpec, ResponseSpecs.entityWasCreated())
                .createAccount();

        AccountResponse[] destinationAccounts = new GetAccountsRequester(
                userSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .as(AccountResponse[].class);

        int destinationAccountId = Arrays.stream(destinationAccounts)
                .filter(account -> account.getId() != sourceAccountId)
                .findFirst()
                .orElseThrow()
                .getId();

        double depositAmount = TestDataGenerator.generateDepositAmount();
        double transferAmount = TestDataGenerator.generateTransferAmountAboveBalance(depositAmount);

        DepositRequest depositRequest = DepositRequest.builder()
                .id(sourceAccountId)
                .balance(depositAmount)
                .build();

        new MakeDepositRequester(
                userSpec,
                ResponseSpecs.requestReturnsOK())
                .makeDeposit(depositRequest);

        double expectedSourceBalance = depositAmount;
        double expectedDestinationBalance = 0;// 🐝 Expected balance is 0 because the destination account is newly created
// and the rejected transfer must not change its balance. QUOTE: "фиксированные значения оставляем только там, где конкретное значение является частью самого тестового сценария или необходимо для проверки граничного случая."

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(sourceAccountId)
                .receiverAccountId(destinationAccountId)
                .amount(transferAmount)
                .build();

        new TransferRequester(userSpec, ResponseSpecs.requestReturnsBadRequestWithText("Invalid transfer: insufficient funds or invalid accounts"))
                .makeTransfer(transferRequest);

        AccountResponse[] accountsAfterTransfer = new GetAccountsRequester(userSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .as(AccountResponse[].class);

        AccountResponse sourceAccountAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getId() == sourceAccountId)
                .findFirst()
                .orElseThrow();

        AccountResponse destinationAccountAfterTransfer = Arrays.stream(accountsAfterTransfer)
                .filter(account -> account.getId() != sourceAccountId)
                .findFirst()
                .orElseThrow();

        assertEquals(expectedSourceBalance, sourceAccountAfterTransfer.getBalance());
        assertEquals(expectedDestinationBalance, destinationAccountAfterTransfer.getBalance());
    }
}



