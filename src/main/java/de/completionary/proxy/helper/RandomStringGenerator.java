package de.completionary.proxy.helper;

import java.util.Random;

public class RandomStringGenerator {

    static final char[] CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789" //!\"§$%&/()=?`´öäü+*#'_.:,; 
                    .toCharArray();

    static Random random = new Random();

    public static String getNextString(int length) {
        char[] result = new char[length];
        for (int i = 0; i < result.length; i++) {
            int randomCharIndex = random.nextInt(CHARS.length);
            result[i] = CHARS[randomCharIndex];
        }
        return new String(result);
    }
}
