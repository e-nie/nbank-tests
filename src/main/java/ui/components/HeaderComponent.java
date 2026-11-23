package ui.components;

import com.codeborne.selenide.SelenideElement;
import ui.pages.ProfilePage;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Selenide.$;

public class HeaderComponent {
    private final SelenideElement userName = $("span.user-name");
    private final SelenideElement profileHeader = $(".profile-header");

    public HeaderComponent shouldSeeUserName(String name) {
        userName.shouldHave(exactText(name));
        return this;
    }

    public ProfilePage openProfile() {
        profileHeader.click();
        return new ProfilePage();
    }
}
