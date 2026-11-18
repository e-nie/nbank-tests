package api.steps;

import api.models.*;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.ResponseSpecification;
import api.skeleton.Endpoint;
import api.skeleton.requesters.CrudRequester;
import api.skeleton.requesters.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import java.util.List;

public class UserSteps {
    private String username;
    private String password;

    public UserSteps(String username, String password) {
        this.username = username;
        this.password = password;
    }


    public static CreateAccountResponse createAccount(CreateUserRequest userRequest) {
        // Авторизуемся под пользователем и создаём ему банковский счёт.
        return new ValidatedCrudRequester<CreateAccountResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated())
                .post(null);
    }


    public static void makeDeposit(
            CreateUserRequest userRequest,
            DepositRequest depositRequest,
            ResponseSpecification responseSpecification) {

        new CrudRequester(
                RequestSpecs.authAsUser(
                        userRequest.getUsername(),
                        userRequest.getPassword()),
                Endpoint.DEPOSIT,
                responseSpecification)
                .post(depositRequest);
    }

    public static ValidatableResponse makeTransfer(
            CreateUserRequest userRequest,
            TransferRequest transferRequest,
            ResponseSpecification responseSpecification) {

        return new CrudRequester(
                RequestSpecs.authAsUser(
                        userRequest.getUsername(),
                        userRequest.getPassword()),
                Endpoint.TRANSFER,
                responseSpecification)
                .post(transferRequest);
    }

    public static CreateAccountResponse[] getAccounts(
            CreateUserRequest userRequest) {

        return new CrudRequester(
                RequestSpecs.authAsUser(
                        userRequest.getUsername(),
                        userRequest.getPassword()),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(CreateAccountResponse[].class);
    }

    //for ui tests

    public List<CreateAccountResponse> getAllAccounts() {
        return new ValidatedCrudRequester<CreateAccountResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()).getAll(CreateAccountResponse[].class);
    }


    public static GetProfileResponse getProfile(
            CreateUserRequest userRequest) {

        return new CrudRequester(
                RequestSpecs.authAsUser(
                        userRequest.getUsername(),
                        userRequest.getPassword()),
                Endpoint.PROFILE,
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(GetProfileResponse.class);
    }

    public static void updateProfile(
            CreateUserRequest userRequest,
            ChangeProfileRequest changeProfileRequest,
            ResponseSpecification responseSpecification) {

        new CrudRequester(
                RequestSpecs.authAsUser(
                        userRequest.getUsername(),
                        userRequest.getPassword()),
                Endpoint.PROFILE,
                responseSpecification)
                .update(changeProfileRequest);
    }
}
