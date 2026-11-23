package ui.pages;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.exactValue;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Condition.exactText;

public class ProfilePage extends BasePage<ProfilePage> {

    private final SelenideElement nameInput = $("input[placeholder='Enter new name']");
    private final SelenideElement saveChangesButton = $$("button").findBy(exactText("💾 Save Changes"));

    @Override
    public String url() {
        return "/edit-profile";
    }

    public ProfilePage enterName(String name) {
        nameInput.setValue(name);
        return this;
    }

    public ProfilePage saveChanges() {
        saveChangesButton.click();
        return this;
    }

    public ProfilePage shouldHaveName(String name) {
        nameInput.shouldHave(exactValue(name));
        return this;
    }
}
