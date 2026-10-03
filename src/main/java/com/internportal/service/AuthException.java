package com.internportal.service;

/** Thrown when login fails or the database cannot be reached. The message is safe to show to the user. */
public class AuthException extends Exception {

    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
