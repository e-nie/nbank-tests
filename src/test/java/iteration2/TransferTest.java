package iteration2;

import generators.RandomData;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import models.CreateUserRequest;
import models.DepositRequest;
import models.TransferRequest;
import models.UserRole;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import requests.*;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

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

        int sourceAccountId = new GetAccountsRequester(
                userSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .path("[0].id");

        //create destination account
        new CreateAccountRequester(userSpec, ResponseSpecs.entityWasCreated())
                .createAccount();

        int destinationAccountId = new GetAccountsRequester(
                userSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .path("find {it.id != " + sourceAccountId + "}.id");

        double deposit1 = 5000;
        double deposit2 = 5000;
        double deposit3 = 100;

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


        new TransferRequester(userSpec, ResponseSpecs.requestReturnsOK())
                .makeTransfer(transferRequest)
                .body("message", equalTo("Transfer successful"))
                .body("senderAccountId", equalTo(sourceAccountId))
                .body("receiverAccountId", equalTo(destinationAccountId))
                .body("amount", equalTo((float) amount));

        new GetAccountsRequester(userSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .body("find {it.id == " + sourceAccountId + " }.balance",
                        equalTo((float) expectedSourceBalance))
                .body("find{ it.id == " + destinationAccountId + " }.balance",
                        equalTo((float) expectedDestinationBalance));
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

        int sourceAccountId = new GetAccountsRequester(
                userASpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .path("[0].id");

        double depositAmount = 5000;

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

        int destinationAccountId = new GetAccountsRequester(
                userBSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .path("[0].id");

        double transferAmount = 1000;

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(sourceAccountId)
                .receiverAccountId(destinationAccountId)
                .amount(transferAmount)
                .build();

        new TransferRequester(userASpec, ResponseSpecs.requestReturnsOK())
                .makeTransfer(transferRequest)
                .body("message", equalTo("Transfer successful"))
                .body("senderAccountId", equalTo(sourceAccountId))
                .body("receiverAccountId", equalTo(destinationAccountId))
                .body("amount", equalTo((float) transferAmount));

        //check user A balance
        double expectedUserABalance = depositAmount - transferAmount;

        new GetAccountsRequester(userASpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .body("find {it.id == " + sourceAccountId + " }.balance",
                        equalTo((float) expectedUserABalance));

        //Check user B balance
        double expectedUserBBalance = transferAmount;
        new GetAccountsRequester(userBSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .body("find {it.id == " + destinationAccountId + " }.balance",
                        equalTo((float) expectedUserBBalance));
    }

    @Test
    public void userCannotTransferAboveMaximumAmountTest() {

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

        int sourceAccountId = new GetAccountsRequester(
                userSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .path("[0].id");

        //create destination account
        new CreateAccountRequester(userSpec, ResponseSpecs.entityWasCreated())
                .createAccount();

        int destinationAccountId = new GetAccountsRequester(
                userSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .path("find {it.id != " + sourceAccountId + "}.id");

        double deposit1 = 5000;
        double deposit2 = 5000;
        double deposit3 = 20;
        double transferAmount = 10000.01;

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
        double expectedDestinationBalance = 0;

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(sourceAccountId)
                .receiverAccountId(destinationAccountId)
                .amount(transferAmount)
                .build();

        new TransferRequester(userSpec, ResponseSpecs.requestReturnsBadRequestWithText("Transfer amount cannot exceed 10000"))
                .makeTransfer(transferRequest);

        new GetAccountsRequester(userSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .body("find {it.id == " + sourceAccountId + " }.balance",
                        equalTo((float) expectedSourceBalance))
                .body("find { it.id == " + destinationAccountId + " }.balance",
                        equalTo((float) expectedDestinationBalance));
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

        int sourceAccountId = new GetAccountsRequester(
                userSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .path("[0].id");

        //create destination account
        new CreateAccountRequester(userSpec, ResponseSpecs.entityWasCreated())
                .createAccount();

        int destinationAccountId = new GetAccountsRequester(
                userSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .path("find {it.id != " + sourceAccountId + "}.id");

        double depositAmount = 5000;
        double transferAmount = -100;

        DepositRequest depositRequest = DepositRequest.builder()
                .id(sourceAccountId)
                .balance(depositAmount)
                .build();

        new MakeDepositRequester(
                userSpec,
                ResponseSpecs.requestReturnsOK())
                .makeDeposit(depositRequest);

        double expectedSourceBalance = depositAmount;
        double expectedDestinationBalance = 0;

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(sourceAccountId)
                .receiverAccountId(destinationAccountId)
                .amount(transferAmount)
                .build();

        new TransferRequester(userSpec, ResponseSpecs.requestReturnsBadRequestWithText("Transfer amount must be at least 0.01"))
                .makeTransfer(transferRequest);

        new GetAccountsRequester(userSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .body("find {it.id == " + sourceAccountId + " }.balance",
                        equalTo((float) expectedSourceBalance))
                .body("find { it.id == " + destinationAccountId + " }.balance",
                        equalTo((float) expectedDestinationBalance));

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

        int sourceAccountId = new GetAccountsRequester(
                userSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .path("[0].id");

        //create destination account
        new CreateAccountRequester(userSpec, ResponseSpecs.entityWasCreated())
                .createAccount();

        int destinationAccountId = new GetAccountsRequester(
                userSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .path("find {it.id != " + sourceAccountId + "}.id");

        double depositAmount = 5000;
        double transferAmount = 5100;

        DepositRequest depositRequest = DepositRequest.builder()
                .id(sourceAccountId)
                .balance(depositAmount)
                .build();

        new MakeDepositRequester(
                userSpec,
                ResponseSpecs.requestReturnsOK())
                .makeDeposit(depositRequest);

        double expectedSourceBalance = depositAmount;
        double expectedDestinationBalance = 0;

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(sourceAccountId)
                .receiverAccountId(destinationAccountId)
                .amount(transferAmount)
                .build();

        new TransferRequester(userSpec, ResponseSpecs.requestReturnsBadRequestWithText("Invalid transfer: insufficient funds or invalid accounts"))
                .makeTransfer(transferRequest);

        new GetAccountsRequester(userSpec, ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .body("find {it.id == " + sourceAccountId + " }.balance",
                        equalTo((float) expectedSourceBalance))
                .body("find { it.id == " + destinationAccountId + " }.balance",
                        equalTo((float) expectedDestinationBalance));

    }
}



