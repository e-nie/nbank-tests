package generators;

import org.apache.commons.lang3.RandomStringUtils;

public class RandomData {
    private RandomData() {
    }

    public static String getUsername() {
        return RandomStringUtils.insecure().nextAlphanumeric(10);
    }

    public static String getPassword() {
        return RandomStringUtils.insecure().nextAlphabetic(3).toUpperCase() +
                RandomStringUtils.insecure().nextAlphabetic(3).toLowerCase() +
                RandomStringUtils.insecure().nextNumeric(3) + "!";

    }
}
