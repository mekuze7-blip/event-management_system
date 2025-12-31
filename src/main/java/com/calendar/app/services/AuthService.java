package com.calendar.app.services;
import com.calendar.app.models.User;
public class AuthService {
    public User authenticate(String username, String password) {
        System.out.println("AuthService: authenticating " + username);
        
        // ALWAYS return success for testing
        User user = new User();
        user.setId(1);
        user.setUsername(username);
        user.setFullName("Test User");
        user.setEmail(username + "@example.com");
        user.setRole("USER");
        
        System.out.println("? Authentication always successful for testing");
        return user;
    }
}