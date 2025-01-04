package kz.ilotterytea.maxon.utils;

import kz.ilotterytea.maxon.utils.math.Math;

public class RandomUtils {
    public static final char[] CHARACTER_POOL = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    public static final int TOKEN_LENGTH = 32;

    public static String generateRandomString() {
        return generateRandomString(CHARACTER_POOL, TOKEN_LENGTH);
    }

    public static String generateRandomString(int length) {
        return generateRandomString(CHARACTER_POOL, length);
    }

    public static String generateRandomString(char[] characterPool, int length) {
        StringBuilder output = new StringBuilder();

        for (int i = 0; i < length; i++) {
            char character = characterPool[Math.getRandomNumber(0, characterPool.length)];
            output.append(character);
        }

        return output.toString();
    }
}
