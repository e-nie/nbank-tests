package iteration1.ui;

import api.models.CreateAccountResponse;
import api.models.CreateUserRequest;
import api.steps.AdminSteps;
import api.steps.UserSteps;
import com.codeborne.selenide.Selenide;
import org.junit.jupiter.api.Test;
import ui.pages.BankAlerts;
import ui.pages.UserDashboardPage;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class CreateAccountTest extends BaseUiTest {
    @Test
    public void userCanCreateAccountTest() {
        CreateUserRequest user = AdminSteps.createUser();

        authAsUser(user);

        Selenide.open("/dashboard");

        new UserDashboardPage().open().createNewAccount();

        List<CreateAccountResponse> createdAccounts = new UserSteps(user.getUsername(), user.getPassword())
                .getAllAccounts();

        assertThat(createdAccounts).hasSize(1);
        new UserDashboardPage().checkAlertMessageAndAccept(BankAlerts.NEW_ACCOUNT_CREATED.getMessage() + createdAccounts.getFirst().getAccountNumber());

        assertThat(createdAccounts.getFirst().getBalance()).isZero();
    }
}
