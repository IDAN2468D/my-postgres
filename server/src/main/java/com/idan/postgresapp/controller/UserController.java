package com.idan.postgresapp.controller;

import com.idan.postgresapp.model.User;
import com.idan.postgresapp.repository.UserRepository;
import com.idan.postgresapp.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role) {
        Map<String, Object> response = new HashMap<>();
        List<User> users = userRepository.findAllByOrderByIdDesc();

        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase();
            users = users.stream()
                    .filter(u -> (u.getName() != null && u.getName().toLowerCase().contains(q)) ||
                                 (u.getEmail() != null && u.getEmail().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        }

        if (role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role)) {
            String r = role.trim();
            users = users.stream()
                    .filter(u -> u.getRole() != null && u.getRole().equalsIgnoreCase(r))
                    .collect(Collectors.toList());
        }

        response.put("status", "success");
        response.put("data", users);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody User user) {
        Map<String, Object> response = new HashMap<>();
        if (user.getName() == null || user.getName().isBlank() ||
            user.getEmail() == null || user.getEmail().isBlank()) {
            response.put("status", "error");
            response.put("message", "שם ואימייל הם שדות חובה");
            return ResponseEntity.badRequest().body(response);
        }
        String cleanEmail = user.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(cleanEmail)) {
            response.put("status", "error");
            response.put("message", "כתובת אימייל זו כבר רשומה במערכת");
            return ResponseEntity.badRequest().body(response);
        }
        try {
            user.setName(user.getName().trim());
            user.setEmail(cleanEmail);
            if (user.getPassword() != null && !user.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            } else {
                user.setPassword(passwordEncoder.encode("password123"));
            }
            if (user.getRole() == null || user.getRole().isBlank()) {
                user.setRole("USER");
            } else {
                user.setRole(user.getRole().trim().toUpperCase());
            }
            User saved = userRepository.save(user);
            auditLogService.log(
                    "CREATE_USER",
                    "USER",
                    saved.getId(),
                    "נוצר משתמש חדש: " + saved.getName() + " (" + saved.getEmail() + ", תפקיד: " + saved.getRole() + ")"
            );
            response.put("status", "success");
            response.put("message", "המשתמש נוסף בהצלחה!");
            response.put("data", saved);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "שגיאה ביצירת משתמש: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        User existing = userRepository.findById(id).orElse(null);
        if (existing == null) {
            response.put("status", "error");
            response.put("message", "משתמש לא נמצא");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        if (payload.get("name") != null && !payload.get("name").isBlank()) {
            existing.setName(payload.get("name").trim());
        }
        if (payload.get("email") != null && !payload.get("email").isBlank()) {
            String newEmail = payload.get("email").trim().toLowerCase();
            if (!newEmail.equalsIgnoreCase(existing.getEmail()) && userRepository.existsByEmail(newEmail)) {
                response.put("status", "error");
                response.put("message", "אימייל זה כבר בשימוש על ידי משתמש אחר");
                return ResponseEntity.badRequest().body(response);
            }
            existing.setEmail(newEmail);
        }
        if (payload.get("role") != null && !payload.get("role").isBlank()) {
            existing.setRole(payload.get("role").trim().toUpperCase());
        }
        if (payload.get("password") != null && !payload.get("password").isBlank()) {
            existing.setPassword(passwordEncoder.encode(payload.get("password")));
        }

        User saved = userRepository.save(existing);
        auditLogService.log(
                "UPDATE_USER",
                "USER",
                saved.getId(),
                "עודכנו פרטי משתמש: " + saved.getName() + " (" + saved.getEmail() + ", תפקיד: " + saved.getRole() + ")"
        );

        response.put("status", "success");
        response.put("message", "פרטי המשתמש עודכנו בהצלחה!");
        response.put("data", saved);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        User existing = userRepository.findById(id).orElse(null);
        if (existing == null) {
            response.put("status", "error");
            response.put("message", "משתמש לא נמצא");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        if ("idan@example.com".equalsIgnoreCase(existing.getEmail())) {
            response.put("status", "error");
            response.put("message", "לא ניתן למחוק את חשבון מנהל המערכת הראשי (idan@example.com)");
            return ResponseEntity.badRequest().body(response);
        }
        String info = existing.getName() + " (" + existing.getEmail() + ")";
        userRepository.deleteById(id);
        auditLogService.log(
                "DELETE_USER",
                "USER",
                id,
                "נמחק משתמש מהמערכת: " + info
        );

        response.put("status", "success");
        response.put("message", "המשתמש נמחק בהצלחה");
        return ResponseEntity.ok(response);
    }
}
