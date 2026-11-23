package ui.pages;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import java.util.Locale;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverConditions.urlContaining;

public class TransferPage extends BasePage<TransferPage> {

    private final SelenideElement senderAccountSelect = $("select.account-selector");
    private final SelenideElement recipientNameInput = $("input[placeholder='Enter recipient name']");
    private final SelenideElement recipientAccountNumberInput = $("input[placeholder='Enter recipient account number']");
    private final SelenideElement amountInput = $("input[placeholder='Enter amount']");
    private final SelenideElement confirmCheckbox = $("#confirmCheck");

    private final SelenideElement transferPageTitle = $(Selectors.byText("🔄 Make a Transfer"));
    private final SelenideElement sendTransferButton = $$("button").findBy(exactText("🚀 Send Transfer"));
    private final SelenideElement transferAgainButton = $$("button").findBy(exactText("🔁 Transfer Again"));

    private final SelenideElement transactionNameInput = $("input[placeholder='Enter name to find transactions']");
    private final SelenideElement searchTransactionsButton = $$("button").findBy(exactText("🔍 Search Transactions"));
    private final SelenideElement transactionList = $("ul.list-group");
    private final ElementsCollection transactionItems = transactionList.$$("li");
    private final WebElementCondition repeatButtonText = exactText("🔁 Repeat");
    private final String repeatButtonSelector = "button";
    private static final String TRANSFER_OUT_FORMAT = "TRANSFER_OUT - $%.2f";
    private final SelenideElement repeatTransferModalTitle = $("div.modal-title.h4");

    private final SelenideElement modalSenderAccountSelect = $(".modal.show select.form-control");
    private final SelenideElement modalAmountInput = $(".modal.show input[type='number']");
    private final SelenideElement modalSendTransferButton = $(".modal.show").$$("button").findBy(exactText("🚀 Send Transfer"));


    @Override
    public String url() {
        return "/transfer";
    }

    public TransferPage shouldShowTransferPage() {
        webdriver().shouldHave(urlContaining(url()));
        transferPageTitle.shouldBe(visible);
        return this;
    }

    public TransferPage selectSenderAccount(String accountNumber) {
        senderAccountSelect.selectOptionContainingText(accountNumber);
        return this;
    }

    public TransferPage fillTransferForm(String recipientName, String recipientAccountNumber, double amount) {
        recipientNameInput.setValue(recipientName);
        recipientAccountNumberInput.setValue(recipientAccountNumber);
        amountInput.setValue(String.valueOf(amount));
        return this;
    }

    public TransferPage confirmTransfer() {
        confirmCheckbox.click();
        return this;
    }

    public TransferPage submitTransfer() {
        sendTransferButton.click();
        return this;
    }

    public TransferPage enterRecipientName(String name) {
        recipientNameInput.setValue(name);
        return this;
    }

    public TransferPage enterRecipientAccountNumber(String accountNumber) {
        recipientAccountNumberInput.setValue(accountNumber);
        return this;
    }

    public TransferPage enterAmount(double amount) {
        amountInput.setValue(String.valueOf(amount));
        return this;
    }

    public TransferPage shouldHaveSelectedSenderAccount(long accountId) {
        senderAccountSelect.shouldHave(value(String.valueOf(accountId)));
        return this;
    }

    public TransferPage openTransferAgain() {
        transferAgainButton.click();
        return this;
    }

    public TransferPage searchTransactionByName(String recipientName) {
        transactionNameInput.setValue(recipientName);
        return this;
    }

    public TransferPage searchTransactions() {
        searchTransactionsButton.click();
        return this;
    }

    public TransferPage repeatTransaction(String transferText) {
        SelenideElement transaction = transactionItems.findBy(text(transferText));
        transaction.$$(repeatButtonSelector).findBy(repeatButtonText).click();
        return this;
    }

    public TransferPage repeatTransaction(double amount) {
        String transferText = String.format(Locale.US, TRANSFER_OUT_FORMAT, amount);
        SelenideElement transaction = transactionItems.findBy(text(transferText));
        transaction.$$(repeatButtonSelector).findBy(repeatButtonText).click();
        return this;
    }


    public TransferPage shouldShowRepeatTransferModal() {
        repeatTransferModalTitle.shouldBe(visible).shouldHave(exactText("🔁 Repeat Transfer"));
        return this;
    }

    public TransferPage selectModalSenderAccount(long accountId) {
        modalSenderAccountSelect.selectOptionByValue(String.valueOf(accountId));
        return this;
    }

    public TransferPage shouldHaveRepeatAmount(double amount) {
        modalAmountInput.shouldHave(value(String.valueOf(amount)));
        return this;
    }

    public TransferPage submitRepeatTransfer() {
        modalSendTransferButton.click();
        return this;
    }

    public TransferPage enterRepeatTransferAmount(double amount) {
        modalAmountInput.setValue(String.valueOf(amount));
        return this;
    }
}