package com.internportal.util;

import com.internportal.model.Role;
import com.internportal.model.User;

/** Holds the currently logged-in user (singleton). */
public final class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private User currentUser;

    private SessionManager() { }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void login(User user) {
        this.currentUser = user;
    }

    public void logout() {
        this.currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean hasRole(Role role) {
        return currentUser != null && currentUser.getRole() == role;
    }
}
