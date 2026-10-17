package enums;

public enum AlertMessage {
    DEPOSIT_AMOUNT_TOO_LOW("Deposit amount must be at least 0.01"),
    DEPOSIT_AMOUNT_TOO_HIGH("Deposit amount cannot exceed 5000"),
    UNAUTHORIZED_ACCESS("Unauthorized access to account"),
    INVALID_ACCOUNT_OR_AMOUNT("Invalid account or amount");


    private final String message;

    AlertMessage(String message){
        this.message = message;
    }

    public String getMessage(){
        return message;
    }

}
