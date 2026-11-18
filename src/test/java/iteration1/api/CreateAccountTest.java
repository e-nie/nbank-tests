package iteration1.api;

import api.models.CreateUserRequest;
import org.junit.jupiter.api.Test;
import api.skeleton.Endpoint;
import api.skeleton.requesters.CrudRequester;
import api.steps.AdminSteps;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;


public class CreateAccountTest extends BaseTest {

    @Test
    public void userCanCreateAccountTest() {
        CreateUserRequest userRequest = AdminSteps.createUser();

        new CrudRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated())
                .post(null);// запросить все аккаунты и удостовериться, что наш пользователь действительно создался



    }
}
