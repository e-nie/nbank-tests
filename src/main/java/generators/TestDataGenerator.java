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
}
