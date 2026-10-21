package com.idan.postgresapp.config;

import com.idan.postgresapp.model.AuditLog;
import com.idan.postgresapp.model.User;
import com.idan.postgresapp.repository.AuditLogRepository;
import com.idan.postgresapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            // 1. Update any existing users without a password or role
            List<User> users = userRepository.findAll();
            for (User u : users) {
                boolean modified = false;
                if (u.getPassword() == null || u.getPassword().isBlank()) {
                    u.setPassword(passwordEncoder.encode("password123"));
                    modified = true;
                }
                if (u.getRole() == null || u.getRole().isBlank()) {
                    if ("idan@example.com".equalsIgnoreCase(u.getEmail())) {
                        u.setRole("ADMIN");
                    } else {
                        u.setRole("USER");
                    }
                    modified = true;
                }
                if (modified) {
                    userRepository.save(u);
                    logger.info("Initialized credentials and role for existing user: {}", u.getEmail());
                }
            }

            // 2. Ensure default admin user exists
            if (!userRepository.existsByEmail("idan@example.com")) {
                User admin = new User("עידן כהן", "idan@example.com", passwordEncoder.encode("password123"), "ADMIN");
                userRepository.save(admin);
                logger.info("Created default admin user: idan@example.com / password123");
            }

            // 3. Ensure default demo user exists
            if (!userRepository.existsByEmail("demo@example.com")) {
                User demo = new User("משתמש דמו", "demo@example.com", passwordEncoder.encode("password123"), "USER");
                userRepository.save(demo);
                logger.info("Created default demo user: demo@example.com / password123");
            }

            // 4. Seed initial audit log if empty
            if (auditLogRepository.count() == 0) {
                auditLogRepository.save(new AuditLog(
                        "SYSTEM_INIT",
                        "SYSTEM",
                        null,
                        "המערכת הופעלה בהצלחה עם אימות JWT, ניהול מלאי ואנליטיקה מתקדמת",
                        "עידן כהן (idan@example.com)"
                ));
            }
        } catch (Exception e) {
            logger.warn("DataInitializer skipped or encountered an issue: {}", e.getMessage());
        }
    }
}
