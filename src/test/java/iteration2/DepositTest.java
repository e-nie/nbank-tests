package iteration2;

import generators.RandomData;
import io.restassured.http.ContentType;
import iteration1.BaseTest;
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

        //получаем айди счета
        int accountId = new GetAccountsRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(),
                        userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAccounts()//метож реквестера с самим запросом
                .extract()
                .path("[0].id");


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
        new GetAccountsRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(),
                        userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .body("[0].balance", equalTo((float) amount));
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
        int accountId = new GetAccountsRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(),
                        userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAccounts()//метод реквестера с самим запросом
                .extract()
                .path("[0].id");

        // создаем модель невалидного депозита
        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountId)
                .balance(-200)
                .build();


        //пытаемся отправить негативный депозит через реквестер
        new MakeDepositRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequestWithText("Deposit amount must be at least 0.01"))
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
    public void userCannotMakeDepositAboveMaximumAmountTest() {
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
        int accountId = new GetAccountsRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(),
                        userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAccounts()//метод реквестера с самим запросом
                .extract()
                .path("[0].id");

        // создаем модель невалидного депозита
        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountId)
                .balance(5000.01)
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
        int accountIdB = new GetAccountsRequester(
                RequestSpecs.authAsUser(userBRequest.getUsername(), userBRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAccounts()
                .extract()
                .path("[0].id");

        // создаем модель депозита на счет юзера B
        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountIdB)
                .balance(100)
                .build();

        //пытаемся отправить депозит как юзер А на счет юзера В
        new MakeDepositRequester(
                RequestSpecs.authAsUser(userARequest.getUsername(), userARequest.getPassword()),
                ResponseSpecs.requestReturnsForbidden("Unauthorized access to account"))
                .makeDeposit(depositRequest);

        //делаем запрос от имени В и проверяем, что баланс его счета остался 0
        new GetAccountsRequester(
                RequestSpecs.authAsUser(userBRequest.getUsername(),
                        userBRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAccounts()//метод реквестера с самим запросом
                .body("[0].balance", equalTo(0.0f));
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
        int nonExistingAccount = 9999999;
        DepositRequest depositRequest = DepositRequest.builder()
                .id(nonExistingAccount)
                .balance(100)
                .build();

        new MakeDepositRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsForbidden("Unauthorized access to account"))
                .makeDeposit(depositRequest);

        //проверяем, что счетов у юзера нет
        new GetAccountsRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(),
                        userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .getAccounts()//метод реквестера с самим запросом
                .body("$", hasSize(0));
    }
}

