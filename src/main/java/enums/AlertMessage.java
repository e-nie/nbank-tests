package enums;

import lombok.Getter;

public enum AlertMessage {
    //константы - это ОБЪЕКТЫ!!!
    UNAUTHORIZED_ACCESS("Unauthorized access to account"),

    SUCCESSFUL_DEPOSIT("✅ Successfully deposited $%s to account %s!"),

    INVALID_DEPOSIT_AMOUNT("❌ Please enter a valid amount."),
    DEPOSIT_AMOUNT_TOO_LOW("Deposit amount must be at least 0.01"),
    DEPOSIT_AMOUNT_TOO_HIGH("Deposit amount cannot exceed 5000"),//API text
    DEPOSIT_AMOUNT_TOO_HIGH_UI("❌ Please deposit less or equal to 5000$."),//UI text

    ACCOUNT_NOT_SELECTED("❌ Please select an account."),
    INVALID_ACCOUNT_OR_AMOUNT("Invalid account or amount"),
    DEPOSIT_UNSUCCESSFUL("❌ Failed to deposit. Please try again."),

    TRANSFER_SUCCESSFUL("Transfer successful"),//API
    TRANSFER_SUCCESSFUL_UI("✅ Successfully transferred $%s to account %s!"),//API
    RECIPIENT_NAME_MISMATCH("❌ The recipient name does not match the registered name."),

    TRANSFER_AMOUNT_EXCEEDS_LIMIT("Transfer amount cannot exceed 10000"),
    TRANSFER_FAILED_UI("❌ Transfer failed: Please try again."),
    REPEAT_TRANSFER_SUCCESSFUL_UI("✅ Transfer of $%s successful from Account %d to %d!"),
    TRANSFER_AMOUNT_EXCEEDS_LIMIT_UI("❌ Error: Transfer amount cannot exceed 10000"),
    TRANSFER_AMOUNT_TOO_LOW("Transfer amount must be at least 0.01"),
    INSUFFICIENT_FUNDS("Invalid transfer: insufficient funds or invalid accounts"),
    REQUIRED_FIELDS_NOT_FILLED("❌ Please fill all fields and confirm."),
    INVALID_TRANSFER("❌ Error: Invalid transfer: insufficient funds or invalid accounts"),
    INCORRECT_TRANSFER_NAME("❌ The recipient name does not match the registered name."),
    RECIPIENT_ACCOUNT_NOT_FOUND( "❌ No user found with this account number."),

    PROFILE_NAME_UPDATED_SUCCESSFULLY("✅ Name updated successfully!"),

    INVALID_PROFILE_NAME("Name must contain two words with letters only"),
    INVALID_PROFILE_NAME_INPUT("❌ Please enter a valid name."),//UI
    SAME_PROFILE_NAME("⚠️ New name is the same as the current one.");

    @Getter
    private final String message;

    AlertMessage(String message) {
        this.message = message;
    }

}
