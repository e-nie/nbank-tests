package iteration1;

import generators.RandomModelGenerator;
import models.CreateUserRequest;
import org.junit.jupiter.api.Test;
import requests.skeleton.Endpoint;
import requests.skeleton.requesters.CrudRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;


public class CreateAccountTest {

    @Test
    public void userCanCreateAccountTest() {
        CreateUserRequest userRequest = RandomModelGenerator.generate(CreateUserRequest.class);


        //Создание пользователя
        new CrudRequester(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USER,
                ResponseSpecs.entityWasCreated())
                .post(userRequest);

        new CrudRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated())
                .post(null);


// запросить все аккаунты и удостовериться, что наш пользователь действительно создался
    }
}
