package com.exelynt.booking.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.exelynt.booking.entity.Role;
import com.exelynt.booking.entity.User;
import com.exelynt.booking.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        createUserIfNotExists("admin", "admin123", Role.ROLE_ADMIN);
        createUserIfNotExists("user", "user123", Role.ROLE_USER);
    }

    private void createUserIfNotExists(String username, String password, Role role) {

        if (userRepository.findByUserName(username).isEmpty()) {

            User user = new User();
            user.setUserName(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(role);

            userRepository.save(user);

            logger.info("Created user: {}", username);

        } else {
            logger.info("User already exists: {}", username);
        }
    }
}