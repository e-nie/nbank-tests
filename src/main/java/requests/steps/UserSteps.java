package requests.steps;

import models.CreateAccountResponse;
import models.CreateUserRequest;
import requests.skeleton.Endpoint;
import requests.skeleton.requesters.ValidatedCrudRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

public class UserSteps {
    public static CreateAccountResponse createAccount(CreateUserRequest userRequest) {
        // Авторизуемся под пользователем и создаём ему банковский счёт.
        return new ValidatedCrudRequester<CreateAccountResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated())
                .post(null);

    }
}
