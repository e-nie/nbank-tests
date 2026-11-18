package iteration1.ui;

import api.generators.RandomModelGenerator;
import api.models.CreateUserRequest;
import api.models.CreateUserResponse;
import api.models.comparison.ModelAssertions;
import api.steps.AdminSteps;
import com.codeborne.selenide.Condition;
import org.junit.jupiter.api.Test;
import ui.pages.AdminPanel;
import ui.pages.BankAlerts;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateUserTest extends BaseUiTest {

    @Test
    public void adminCanCreateUserTest() {
        //STEP 1: Admin logged in
        CreateUserRequest admin = CreateUserRequest.getAdmin();

        authAsUser(admin);

        //STEP 2: Admin creates user
        CreateUserRequest newUser = RandomModelGenerator.generate(CreateUserRequest.class);

        new AdminPanel().open().createUser(newUser.getUsername(), newUser.getPassword())//можно тоже перегрузить этот метод
                .checkAlertMessageAndAccept(BankAlerts.USER_CREATED_SUCCESSFULLY.getMessage())
                .getAllUsers()
                .findBy(Condition.exactText(newUser.getUsername() + "\nUSER")).shouldBe(Condition.visible);


        //STEP 5: Verify that the user was also successfully created via the API
        CreateUserResponse createdUser = AdminSteps.getAllUsers().stream()
                .filter(user -> user.getUsername().equals(newUser.getUsername()))
                .findFirst().get();

        ModelAssertions.assertThatModels(newUser, createdUser).match();
    }

    @Test
    public void adminCannotCreateUserWithInvalidDataTest() {
        //STEP 1: Admin logged in
        CreateUserRequest admin = CreateUserRequest.getAdmin();

        authAsUser(admin);

        //STEP 2: Admin creates user
        CreateUserRequest newUser = RandomModelGenerator.generate(CreateUserRequest.class);

        //create invalid userName
        newUser.setUsername("a");

        new AdminPanel().open().createUser(newUser.getUsername(), newUser.getPassword())
                .checkAlertMessageAndAccept(BankAlerts.USERNAME_MUST_BE_BETWEEN_3_AND_15_CHARACTERS.getMessage())
                .getAllUsers().findBy(Condition.exactText(newUser.getUsername() + "\nUSER")).shouldNotBe(Condition.exist);

        //STEP 5: Verify that the user was also NOT created via the API


        long usersWithSameUsernameAsNewUser = AdminSteps.getAllUsers().stream()
                .filter(user -> user.getUsername().equals(newUser.getUsername()))
                .count();
        assertThat(usersWithSameUsernameAsNewUser).isZero();
    }
}
