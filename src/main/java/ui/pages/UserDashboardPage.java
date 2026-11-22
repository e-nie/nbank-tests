package ui.pages;


import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

@Getter
public class UserDashboardPage extends BasePage<UserDashboardPage> {
    private SelenideElement welcomeText = $(Selectors.byClassName("welcome-text"));
    private SelenideElement createNewAccount = $(Selectors.byText("➕ Create New Account"));

    private final SelenideElement depositPageTitle = $(Selectors.byText("💰 Deposit Money"));
    private final SelenideElement depositMoneyButton = $$("button").findBy(exactText("💰 Deposit Money"));
    private final SelenideElement accountSelect = $("select");
    private final ElementsCollection accountOptions = accountSelect.$$("option");
    private final SelenideElement amountInput = $("[placeholder='Enter amount']");
    private final SelenideElement depositButton = $$("button").findBy(exactText("\uD83D\uDCB5 Deposit"));


    @Override
    public String url() {
        return "/dashboard";
    }

    public UserDashboardPage createNewAccount() {
        createNewAccount.click();
        return this;
    }


    public UserDashboardPage makeDeposit(double amount) {
        return makeDeposit(String.valueOf(amount));
    }

    //для TC-03, где тестируем на пустую строку
    public UserDashboardPage makeDeposit(String amount) {
        depositMoneyButton.click();
        accountSelect.click();
        accountSelect.selectOption(1);
        amountInput.setValue(amount);
        depositButton.click();
        return this;
    }

    public UserDashboardPage makeDepositWithoutSelectingAccount(double amount) {
        depositMoneyButton.click();
        amountInput.setValue(String.valueOf(amount));
        depositButton.click();
        return this;
    }

    public UserDashboardPage openDepositForm() {
        depositMoneyButton.click();
        accountSelect.click();
        return this;
    }

    public UserDashboardPage shouldSeeAccount(String accountNumber) {
        accountOptions.findBy(text(accountNumber)).should(exist);
        return this;
    }

    public UserDashboardPage shouldNotSeeAccount(String accountNumber) {
        accountOptions.findBy(text(accountNumber)).shouldNot(exist);
        return this;
    }


    public UserDashboardPage fillDepositForm(double amount) {
        depositMoneyButton.click();
        accountSelect.click();
        accountSelect.selectOption(1);
        amountInput.setValue(String.valueOf(amount));
        return this;
    }

    public UserDashboardPage submitDeposit() {
        depositButton.click();
        return this;
    }

    public UserDashboardPage shouldShowDepositForm() {
        depositPageTitle.shouldBe(visible);
        return this;
    }
}

