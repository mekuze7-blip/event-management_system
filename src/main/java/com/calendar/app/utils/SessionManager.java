package com.calendar.app.utils;

import com.calendar.app.models.User;

public class SessionManager {
    private static User currentUser;
    
    public static void setCurrentUser(User user) {
        currentUser = user;
        System.out.println("Session started for: " + user.getUsername());
    }
    
    public static User getCurrentUser() {
        return currentUser;
    }
    
    public static boolean isLoggedIn() {
        return currentUser != null;
    }
    
    public static void logout() {
        System.out.println("Logging out: " + (currentUser != null ? currentUser.getUsername() : "No user"));
        currentUser = null;
    }
}