package enums;

public enum AlertMessage {
    //константы - это ОБЪЕКТЫ!!!
    DEPOSIT_AMOUNT_TOO_LOW("Deposit amount must be at least 0.01"),
    DEPOSIT_AMOUNT_TOO_HIGH("Deposit amount cannot exceed 5000"),
    UNAUTHORIZED_ACCESS("Unauthorized access to account"),
    INVALID_ACCOUNT_OR_AMOUNT("Invalid account or amount"),
    INVALID_PROFILE_NAME("Name must contain two words with letters only"),
    TRANSFER_SUCCESSFUL("Transfer successful"),
    TRANSFER_AMOUNT_EXCEEDS_LIMIT("Transfer amount cannot exceed 10000"),
    TRANSFER_AMOUNT_TOO_LOW("Transfer amount must be at least 0.01");






    private final String message;

    AlertMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
