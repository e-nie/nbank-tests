package iteration2;

import enums.AlertMessage;
import generators.RandomData;
import generators.TestDataGenerator;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import iteration1.BaseTest;
import models.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import requests.AdminCreateUserRequester;
import requests.CreateAccountRequester;
import requests.GetAccountsRequester;
import requests.MakeDepositRequester;
import requests.skeleton.Endpoint;
import requests.skeleton.requesters.CrudRequester;
import requests.skeleton.requesters.ValidatedCrudRequester;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


class DepositTest extends BaseTest {

    @ParameterizedTest
    @ValueSource(doubles = {5000, 4999.99, 0.01})
    public void userCanMakeDepositWithValidAmountTest(double amount) {
        // Создаём нового пользователя через AdminSteps.createUser():
        // 1. Генерируется объект CreateUserRequest со случайными валидными данными
        //    (username, password и role) с помощью RandomModelGenerator.
        // 2. От имени администратора отправляется POST-запрос на создание пользователя.
        // 3. Проверяется успешный ответ сервера (HTTP 201 Created).
        // 4. Метод возвращает объект CreateUserRequest с данными созданного пользователя.
        // Сохраняем этот объект в переменную userRequest, чтобы затем использовать
        // username и password для авторизации пользователя в следующих запросах.
        CreateUserRequest userRequest = AdminSteps.createUser();//возвращает объект

        // Авторизуемся под пользователем и создаём ему счёт.
        CreateAccountResponse account = UserSteps.createAccount(userRequest);

        // Получаем ID созданного счёта непосредственно из ответа POST-запроса.
        long accountId = account.getId();

        //создаем модель для депозита
        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountId)
                .balance(amount)
                .build();

        // Отправляем POST-запрос на пополнение счёта
        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsOK())
                .post(depositRequest);


        //проверяем баланс после депозита
        ValidatableResponse responseAfterDeposit = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(),
                        userRequest.getPassword()),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK())
                .get();

        AccountResponse[] accountsAfterDeposit =
                responseAfterDeposit.extract().as(AccountResponse[].class);

        assertEquals(amount,
                Arrays.stream(accountsAfterDeposit)
                        .filter(a -> a.getId() == accountId)
                        .findFirst()
                        .orElseThrow()
                        .getBalance()
        );
    }

    @Test
    public void userCannotMakeDepositWithNegativeAmountTest() {
        // Создаём пользователя с валидными случайными данными.
        // Метод также отправляет запрос на создание пользователя.
        CreateUserRequest userRequest = AdminSteps.createUser();

        // Авторизуемся под пользователем и создаём ему счёт.
        CreateAccountResponse account = UserSteps.createAccount(userRequest);

        // Получаем ID созданного счёта из ответа на POST-запрос.
        long accountId = account.getId();

        // Создаём модель запроса на депозит с отрицательной суммой.
        // accountId — ID созданного счёта, balance — невалидная сумма.
        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountId)
                .balance(TestDataGenerator.generateNegativeDepositAmount())
                .build();


        // Авторизуемся под пользователем и отправляем POST-запрос
        // с отрицательной суммой. Проверяем, что сервер отклоняет депозит.
        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsBadRequestWithText(AlertMessage.DEPOSIT_AMOUNT_TOO_LOW.getMessage()))
                .post(depositRequest);


        // Получаем счета пользователя после отклонённого депозита,
        // чтобы убедиться, что баланс не изменился.
        ValidatableResponse responseAfterDeposit = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(),
                        userRequest.getPassword()),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK())
                .get();//метод реквестера с самим запросом

        AccountResponse[] accountsAfterDeposit = responseAfterDeposit.extract().as(AccountResponse[].class);
        assertEquals(0.0,
                Arrays.stream(accountsAfterDeposit)
                        .filter(a -> a.getId() == accountId)
                        .findFirst()
                        .orElseThrow()
                        .getBalance()
        );
    }

    @ParameterizedTest
    @ValueSource(doubles = {5000.01})
    public void userCannotMakeDepositAboveMaximumAmountTest(double amount) {
        CreateUserRequest userRequest = AdminSteps.createUser();

        // Авторизуемся под пользователем и создаём ему счёт.
        CreateAccountResponse account = UserSteps.createAccount(userRequest);

        //получаем айди счета
        long accountId = account.getId();

        // Создаём модель депозита с суммой выше допустимого максимума.
        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountId)
                .balance(amount)
                .build();

        // Отправляем депозит выше допустимого максимума.
        // Проверяем статус 400 и текст ошибки.
        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsBadRequestWithText(AlertMessage.DEPOSIT_AMOUNT_TOO_HIGH.getMessage()))
                .post(depositRequest);

        // Получаем счета пользователя после отклонённого депозита.
        ValidatableResponse responseAfterDeposit = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(),
                        userRequest.getPassword()),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK())
                .get();

        // Десериализуем JSON-массив в массив объектов AccountResponse.
        AccountResponse[] accountsAfterDeposit =
                responseAfterDeposit.extract().as(AccountResponse[].class);

        // Находим созданный счёт по ID и проверяем,
        // что его баланс остался нулевым.
        assertEquals(0.0,
                Arrays.stream(accountsAfterDeposit)
                        .filter(a -> a.getId() == accountId)
                        .findFirst()
                        .orElseThrow()
                        .getBalance()
        );
    }

    @Test
    public void userCannotMakeDepositToAnotherUsersAccountTest() {

        // Администратор создаёт двух разных пользователей.
        CreateUserRequest userARequest = AdminSteps.createUser();
        CreateUserRequest userBRequest = AdminSteps.createUser();

        // Авторизуемся под пользователем B и создаём ему банковский счёт.
        CreateAccountResponse accountB = UserSteps.createAccount(userBRequest);

        // Get user B's account ID.
        long accountBId = accountB.getId();

        // Prepare a deposit request for user B's account.
        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountBId)
                .balance(TestDataGenerator.generateDepositAmount())
                .build();

        // Attempt to deposit money into user B's account while authenticated as user A.
        new CrudRequester(
                RequestSpecs.authAsUser(userARequest.getUsername(), userARequest.getPassword()),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsForbidden(AlertMessage.UNAUTHORIZED_ACCESS.getMessage()))
                .post(depositRequest);

        // Retrieve user B's accounts after the deposit attempt.
        ValidatableResponse responseBAfterDeposit = new CrudRequester(
                RequestSpecs.authAsUser(userBRequest.getUsername(),
                        userBRequest.getPassword()),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK())
                .get();

        AccountResponse[] accountsBAfterDeposit =
                responseBAfterDeposit.extract().as(AccountResponse[].class);

        // Verify that user B's account balance remains zero.
        assertEquals(0.0, Arrays.stream(accountsBAfterDeposit)
                .filter(account -> account.getId() == accountBId)
                .findFirst()
                .orElseThrow()
                .getBalance());
    }

    @Test
    public void userCannotMakeDepositToNonExistingAccountTest() {
        CreateUserRequest userRequest = AdminSteps.createUser();

        // Prepare a deposit request for a non-existing account.
        DepositRequest depositRequest = DepositRequest.builder()
                .id(TestDataGenerator.generateNonExistingAccountId())
                .balance(TestDataGenerator.generateDepositAmount())
                .build();

        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsForbidden(AlertMessage.UNAUTHORIZED_ACCESS.getMessage()))
                .post(depositRequest);

        // Verify that the user still has no bank accounts.
        ValidatableResponse responseAfterDeposit = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(),
                        userRequest.getPassword()),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK())
                .get();

        // Deserialize the response into an array of accounts.
        AccountResponse[] accountsAfterDeposit = responseAfterDeposit.extract().as(AccountResponse[].class);
        assertEquals(0, accountsAfterDeposit.length);
    }
}

