package generators;

import java.util.Random;

public class TestDataGenerator {
    private static final Random random = new Random();

    public static double generateNegativeDepositAmount() {
        return -random.nextInt(1, 500_001) / 100.0;
    }

    public static double generateDepositAmount() {
        return random.nextInt(1, 500_001) / 100.0;
    }

    public static int generateNonExistingAccountId() {
        return random.nextInt(1_000_000, 2_000_000);
    }

    public static String generateProfileName() {
        String[] firstNames = {"Anna", "Maria", "Sofia", "Elena", "Julia", "Lea", "Heike", "Jacqueline", "Barbara"};
        String[] lastNames = {"Schmidt", "Weber", "Muller", "Fischer", "Wagner", "Staub", "Weiss", "True", "Lindner"};

        String firstName = firstNames[random.nextInt(firstNames.length)];
        String lastName = lastNames[random.nextInt(lastNames.length)];

        return firstName + " " + lastName;
    }

    public static String generateInvalidProfileName() {
        String[] invalidNames = {
                "Anna",
                "Anna123",
                "Anna_Schmidt",
                "Anna Schmidt123",
                "Anna Schmidt Weber",
                "12345",
                ""
        };

        return invalidNames[random.nextInt(invalidNames.length)];
    }

    public static double generateDepositForTransfer() {
        return random.nextInt(3_334_00, 500_001) / 100.0;
    }

    public static double generateNegativeTransferAmount(double depositAmount) {
        int maxCents = (int) Math.round(depositAmount * 100);
        return -random.nextInt(1, maxCents + 1) / 100.0;
    }

    public static double generateTransferAmountAboveBalance(double depositAmount) {
        int minCents = (int) Math.round(depositAmount * 100) + 1;
        return random.nextInt(minCents, 1_000_001) / 100.0;
    }

}
