package iteration1.ui;

import com.codeborne.selenide.Condition;
import api.models.CreateUserRequest;
import org.junit.jupiter.api.Test;
import api.steps.AdminSteps;
import ui.pages.AdminPanel;
import ui.pages.LoginPage;
import ui.pages.UserDashboardPage;

import static com.codeborne.selenide.Selenide.$;

public class LoginUserTest extends BaseUiTest {
    @Test
    public void adminCanLoginWithCorrectDataTest() {
        CreateUserRequest admin = CreateUserRequest.getAdmin();
        new LoginPage().open().login(admin.getUsername(), admin.getPassword())
                .getPage(AdminPanel.class).getAdminPanelText().shouldBe(Condition.visible);

    }

    @Test
    public void userCanLoginWithCorrectDataTest() {
        CreateUserRequest user = AdminSteps.createUser();

        new LoginPage().open().login(user.getUsername(), user.getPassword())
                .getPage(UserDashboardPage.class).getWelcomeText()
                .shouldBe(Condition.visible)
                .shouldHave(Condition.text("Welcome, noname!"));

    }
}
