package com.idan.postgresapp.controller;

import com.idan.postgresapp.dto.AuthResponse;
import com.idan.postgresapp.dto.LoginRequest;
import com.idan.postgresapp.dto.RegisterRequest;
import com.idan.postgresapp.model.User;
import com.idan.postgresapp.repository.UserRepository;
import com.idan.postgresapp.security.JwtUtils;
import com.idan.postgresapp.security.UserDetailsImpl;
import com.idan.postgresapp.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private AuditLogService auditLogService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        if (request.getName() == null || request.getName().isBlank() ||
            request.getEmail() == null || request.getEmail().isBlank() ||
            request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(
                    new AuthResponse("error", "שם, אימייל וסיסמה הם שדות חובה", null, null)
            );
        }

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(
                    new AuthResponse("error", "כתובת אימייל זו כבר קיימת במערכת", null, null)
            );
        }

        String role = (request.getRole() != null && !request.getRole().isBlank())
                ? request.getRole().trim().toUpperCase()
                : "USER";

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);

        User saved = userRepository.save(user);

        String token = jwtUtils.generateToken(saved.getEmail(), saved.getRole(), saved.getId(), saved.getName());

        auditLogService.logWithActor(
                "USER_REGISTER",
                "AUTH",
                saved.getId(),
                "משתמש חדש נרשם למערכת: " + saved.getName() + " (" + saved.getRole() + ")",
                saved.getName() + " (" + saved.getEmail() + ")"
        );

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", saved.getId());
        userMap.put("name", saved.getName());
        userMap.put("email", saved.getEmail());
        userMap.put("role", saved.getRole());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new AuthResponse("success", "המשתמש נרשם בהצלחה!", token, userMap)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank() ||
            request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(
                    new AuthResponse("error", "אימייל וסיסמה הם שדות חובה", null, null)
            );
        }

        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null || user.getPassword() == null ||
            !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new AuthResponse("error", "אימייל או סיסמה שגויים", null, null)
            );
        }

        String token = jwtUtils.generateToken(user.getEmail(), user.getRole(), user.getId(), user.getName());

        auditLogService.logWithActor(
                "USER_LOGIN",
                "AUTH",
                user.getId(),
                "התחברות מוצלחת למערכת באמצעות JWT",
                user.getName() + " (" + user.getEmail() + ")"
        );

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("name", user.getName());
        userMap.put("email", user.getEmail());
        userMap.put("role", user.getRole());

        return ResponseEntity.ok(
                new AuthResponse("success", "התחברת בהצלחה!", token, userMap)
        );
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> response = new HashMap<>();

        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() instanceof String) {
            response.put("status", "error");
            response.put("message", "לא מזוהה משתמש מחובר");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        if (auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", userDetails.getId());
            userData.put("name", userDetails.getName());
            userData.put("email", userDetails.getEmail());
            userData.put("role", userDetails.getRole());

            response.put("status", "success");
            response.put("user", userData);
            return ResponseEntity.ok(response);
        }

        response.put("status", "error");
        response.put("message", "משתמש לא נמצא");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
}
