package iteration2;

import enums.AlertMessage;
import generators.RandomData;
import generators.TestDataGenerator;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import iteration1.BaseTest;
import models.AccountResponse;
import models.CreateUserRequest;
import models.DepositRequest;
import models.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import requests.AdminCreateUserRequester;
import requests.CreateAccountRequester;
import requests.GetAccountsRequester;
import requests.MakeDepositRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


class DepositTest extends BaseTest {

    @ParameterizedTest
    @ValueSource(doubles = {5000, 4999.99, 0.01})
    public void userCanMakeDepositWithValidAmountTest(double amount) {
        //создаем модель пользователя
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        //Создание пользователя
        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(), //логинимся как админ
                ResponseSpecs.entityWasCreated())//
                .createUser(userRequest);//передаем модель пользователя

        //логинимся как юзер и создаем счет
        new CreateAccountRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .createAccount();

        //получаем HTTP-ответ со счетом
        ValidatableResponse response = new GetAccountsRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(),
                        userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();//метод реквестера с самим запросом

        //десериализуем JSON в массив объектов AccountResponse
        AccountResponse[] accounts = response.extract().as(AccountResponse[].class);

        //получаем id первого счета
        int accountId = accounts[0].getId();

        //создаем модель для депозита
        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountId)
                .balance(amount)
                .build();

        //отправляем депозит через реквестер
        new MakeDepositRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())//
                .makeDeposit(depositRequest);//передаем модель

        //проверяем баланс после депозита
        ValidatableResponse responseAfterDeposit = new GetAccountsRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(),
                        userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();

        AccountResponse[] accountsAfterDeposit = responseAfterDeposit.extract().as(AccountResponse[].class);

        assertEquals(amount, accountsAfterDeposit[0].getBalance());
    }

    @Test
    public void userCannotMakeDepositWithNegativeAmountTest() {
        //создаем модель пользователя
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        //Создание пользователя
        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(), //логинимся как админ
                ResponseSpecs.entityWasCreated())//
                .createUser(userRequest);//передаем модель пользователя

        //логинимся как юзер и создаем счет
        new CreateAccountRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .createAccount();

        //получаем айди счета
        ValidatableResponse response = new GetAccountsRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(),
                        userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();//метод реквестера с самим запросом

        //десериализуем JSON в массив объектов AccountResponse
        AccountResponse[] accounts = response.extract().as(AccountResponse[].class);

        //получаем id первого счета
        int accountId = accounts[0].getId();

        // создаем модель невалидного депозита
        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountId)
                .balance(TestDataGenerator.generateNegativeDepositAmount())
                .build();


        //пытаемся отправить негативный депозит через реквестер
        new MakeDepositRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequestWithText(AlertMessage.DEPOSIT_AMOUNT_TOO_LOW.getMessage()))
                .makeDeposit(depositRequest);


        //проверяем, что депозит не прошел и баланс остался нулевой
        ValidatableResponse responseAfterDeposit = new GetAccountsRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(),
                        userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();//метод реквестера с самим запросом

        AccountResponse[] accountsAfterDeposit = responseAfterDeposit.extract().as(AccountResponse[].class);
        assertEquals(0.0, accountsAfterDeposit[0].getBalance());
    }

    @ParameterizedTest
    @ValueSource(doubles = {5000.01})
    public void userCannotMakeDepositAboveMaximumAmountTest(double amount) {
        //создаем модель пользователя
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        //Создание пользователя админом
        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(), //логинимся как админ
                ResponseSpecs.entityWasCreated())//
                .createUser(userRequest);//передаем модель пользователя

        //логинимся как юзер и создаем счет
        new CreateAccountRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .createAccount();

        //получаем айди счета
        ValidatableResponse response = new GetAccountsRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(),
                        userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();

        //Десериализуем JSON
        AccountResponse[] accounts = response.extract().as(AccountResponse[].class);
        int accountId = accounts[0].getId();
        // создаем модель невалидного депозита
        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountId)
                .balance(amount)
                .build();

        //пытаемся отправить депозит выше максимума через реквестер
        new MakeDepositRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequestWithText("Deposit amount cannot exceed 5000"))
                .makeDeposit(depositRequest);

        //проверяем, что депозит не прошел и баланс остался нулевой
        new GetAccountsRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(),
                        userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAccounts()//метод реквестера с самим запросом
                .body("[0].balance", equalTo(0.0f));
    }

    @Test
    public void userCannotMakeDepositToAnotherUsersAccountTest() {
        //create User A model
        CreateUserRequest userARequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        //create user B model
        CreateUserRequest userBRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        // admin creates user A
        //Создание пользователя админом
        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(), //логинимся как админ
                ResponseSpecs.entityWasCreated())//
                .createUser(userARequest);//передаем модель пользователя A

        // admin creates user B
        //Создание пользователя админом
        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(), //логинимся как админ
                ResponseSpecs.entityWasCreated())//
                .createUser(userBRequest);//передаем модель пользователя B

        //login as user B and create account for user B
        new CreateAccountRequester(RequestSpecs.authAsUser(userBRequest.getUsername(), userBRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .createAccount();

        //extract the account id for user B
        ValidatableResponse responseB = new GetAccountsRequester(
                RequestSpecs.authAsUser(userBRequest.getUsername(), userBRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();

        AccountResponse[] accountsB = responseB.extract().as(AccountResponse[].class);
        int accountIdB = accountsB[0].getId();

        // создаем модель депозита на счет юзера B
        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountIdB)
                .balance(TestDataGenerator.generateDepositAmount())
                .build();

        //пытаемся отправить депозит как юзер А на счет юзера В
        new MakeDepositRequester(
                RequestSpecs.authAsUser(userARequest.getUsername(), userARequest.getPassword()),
                ResponseSpecs.requestReturnsForbidden(AlertMessage.UNAUTHORIZED_ACCESS.getMessage()))
                .makeDeposit(depositRequest);

        //делаем запрос от имени В и проверяем, что баланс его счета остался 0
        ValidatableResponse responseBAfterDeposit = new GetAccountsRequester(
                RequestSpecs.authAsUser(userBRequest.getUsername(),
                        userBRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();//метод реквестера с самим запросом

        //десериализуем ответ
        AccountResponse[] accountBAfterDeposit = responseBAfterDeposit.extract().as(AccountResponse[].class);
        assertEquals(0.0, accountBAfterDeposit[0].getBalance());

    }

    @Test
    public void userCannotMakeDepositToNonExistingAccountTest() {

        //создаем модель пользователя
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        //Создание пользователя админом
        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(), //логинимся как админ
                ResponseSpecs.entityWasCreated())//
                .createUser(userRequest);//передаем модель пользователя

        // создаем модель  депозита
        DepositRequest depositRequest = DepositRequest.builder()
                .id(TestDataGenerator.generateNonExistingAccountId())
                .balance(TestDataGenerator.generateDepositAmount())
                .build();

        new MakeDepositRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsForbidden(AlertMessage.UNAUTHORIZED_ACCESS.getMessage()))
                .makeDeposit(depositRequest);

        //проверяем, что счетов у юзера нет
        ValidatableResponse responseAfterDeposit = new GetAccountsRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(),
                        userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();//метод реквестера с самим запросом

        //десериализация

        AccountResponse[] accountsAfterDeposit  = responseAfterDeposit.extract().as(AccountResponse[].class);
        assertEquals(0,accountsAfterDeposit.length);
    }
}

