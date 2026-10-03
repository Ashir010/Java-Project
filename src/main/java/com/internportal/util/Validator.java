package com.internportal.util;

import java.util.regex.Pattern;

/** Input validation shared by the UI and the service layer. */
public final class Validator {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^[6-9]\\d{9}$");   // Indian mobile number
    private static final Pattern NAME = Pattern.compile("^\\p{L}[\\p{L} .'-]{1,99}$");

    private Validator() { }

    public static boolean isValidEmail(String s) {
        return s != null && s.length() <= 100 && EMAIL.matcher(s).matches();
    }

    public static boolean isValidPhone(String s) {
        return s != null && PHONE.matcher(s).matches();
    }

    public static boolean isValidName(String s) {
        return s != null && NAME.matcher(s).matches();
    }

    /** Returns an error message, or null if the password is acceptable. */
    public static String passwordError(String p) {
        if (p == null || p.length() < 8) {
            return "Password must be at least 8 characters.";
        }
        if (p.length() > 72) {
            return "Password must be 72 characters or fewer.";   // BCrypt limit
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : p.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        if (!hasLetter || !hasDigit) {
            return "Password must contain at least one letter and one number.";
        }
        return null;
    }
}
