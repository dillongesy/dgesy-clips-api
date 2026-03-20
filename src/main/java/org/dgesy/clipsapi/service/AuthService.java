package org.dgesy.clipsapi.service;

import org.dgesy.clipsapi.model.User;
import org.dgesy.clipsapi.repository.UserRepository;
import org.dgesy.clipsapi.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public String register(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole("USER");
        userRepository.save(user);

        return jwtUtil.generateToken(username, "USER");
    }

    public String login(String username, String password) {
        System.out.println("Login attempt for: " + username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    System.out.println("User not found: " + username);
                    return new RuntimeException("Invalid credentials");
                });

        System.out.println("User found, banned: " + user.isBanned());
        System.out.println("Stored hash: " + user.getPasswordHash());
        System.out.println("Password matches: " + passwordEncoder.matches(password, user.getPasswordHash()));

        if (user.isBanned()) {
            throw new RuntimeException("Account suspended");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        return jwtUtil.generateToken(username, user.getRole());
    }
}