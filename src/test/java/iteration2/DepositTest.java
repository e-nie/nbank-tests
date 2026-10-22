package iteration2;

import enums.AlertMessage;
import generators.RandomModelGenerator;
import generators.TestDataGenerator;
import iteration1.BaseTest;
import models.CreateAccountResponse;
import models.CreateUserRequest;
import models.DepositRequest;
import models.comparison.ModelAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;
import specs.ResponseSpecs;

import java.util.Arrays;

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
        UserSteps.makeDeposit(
                userRequest,
                depositRequest,
                ResponseSpecs.requestReturnsOK());

        // Получаем счета после депозита
        CreateAccountResponse[] accountsAfterDeposit = UserSteps.getAccounts(userRequest);

        // Находим нужный счёт
        CreateAccountResponse accountAfterDeposit = Arrays.stream(accountsAfterDeposit)
                .filter(a -> a.getId() == accountId)
                .findFirst()
                .orElseThrow();

        // Сравниваем сумму депозита с итоговым балансом
        ModelAssertions.assertThatModels(depositRequest, accountAfterDeposit).match();

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
        UserSteps.makeDeposit(
                userRequest,
                depositRequest,
                ResponseSpecs.requestReturnsBadRequestWithText(
                        AlertMessage.DEPOSIT_AMOUNT_TOO_LOW.getMessage()));

        // Получаем счета пользователя после отклонённого депозита,
        // чтобы убедиться, что баланс не изменился.
        CreateAccountResponse[] accountsAfterDeposit =
                UserSteps.getAccounts(userRequest);

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
        UserSteps.makeDeposit(
                userRequest,
                depositRequest,
                ResponseSpecs.requestReturnsBadRequestWithText(
                        AlertMessage.DEPOSIT_AMOUNT_TOO_HIGH.getMessage()));

        // Получаем счета пользователя после отклонённого депозита.
        // Десериализуем JSON-массив в массив объектов AccountResponse.
        CreateAccountResponse[] accountsAfterDeposit = UserSteps.getAccounts(userRequest);


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
        DepositRequest depositRequest =
                RandomModelGenerator.generate(DepositRequest.class);

        depositRequest.setId(accountBId);

        // Attempt to deposit money into user B's account while authenticated as user A.
        UserSteps.makeDeposit(
                userARequest,
                depositRequest,
                ResponseSpecs.requestReturnsForbidden(
                        AlertMessage.UNAUTHORIZED_ACCESS.getMessage()));

        // Retrieve user B's accounts after the deposit attempt.
        CreateAccountResponse[] accountsBAfterDeposit =
                UserSteps.getAccounts(userBRequest);

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
        DepositRequest depositRequest = RandomModelGenerator.generate(DepositRequest.class);

        depositRequest.setId(TestDataGenerator.generateNonExistingAccountId());// Keep TestDataGenerator for negative test scenarios.

        UserSteps.makeDeposit(
                userRequest,
                depositRequest,
                ResponseSpecs.requestReturnsForbidden(
                        AlertMessage.UNAUTHORIZED_ACCESS.getMessage()));

        // Verify that the user still has no bank accounts.
        CreateAccountResponse[] accountsAfterDeposit = UserSteps.getAccounts(userRequest);

        assertEquals(0, accountsAfterDeposit.length);
    }
}

