package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.Model.User;
import com.mitmeerut.CFM_Portal.Service.UserService;
import com.mitmeerut.CFM_Portal.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/auth")

public class AuthController {

    public AuthController() {
        super();
    }

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private com.mitmeerut.CFM_Portal.Service.RolePermissionService rolePermissionService;

    // ---------------- REGISTER ----------------

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> req) {

        String name = req.get("name");
        String email = req.get("email");
        String password = req.get("password");

        try {
            User user = userService.registerTeacherUser(name, email, password);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Registration submitted. Wait for admin approval.");
            response.put("id", user.getId());
            response.put("email", user.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ---------------- LOGIN ----------------

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> req) {

        String email = req.get("email");
        String password = req.get("password");

        User user = userService.login(email, password);
        if (user == null) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Invalid credentials or not approved yet");
            return ResponseEntity.status(401).body(error);
        }

        List<String> roles = userService.getEffectiveRoles(user);

        // 🔥 ROLE PRIORITY: HOD > lastActiveRole > SUBJECTHEAD > DEFAULT
        String activeRole = roles.contains("HOD") ? "HOD"
                : (user.getLastActiveRole() != null && roles.contains(user.getLastActiveRole()))
                        ? user.getLastActiveRole()
                        : roles.contains("SUBJECTHEAD") ? "SUBJECTHEAD"
                                : roles.contains("ADMIN") ? "ADMIN"
                                        : roles.contains("TEACHER") ? "TEACHER"
                                                : user.getRole().name();

        if (activeRole == null)
            activeRole = "TEACHER"; // absolute safety

        List<String> permissions = rolePermissionService.getPermissionsByRole(activeRole);

        String token = jwtTokenProvider.generateToken(email, user.getId(), activeRole, roles, permissions);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Login successful");
        response.put("token", token);
        response.put("tokenType", "Bearer");
        response.put("activeRole", activeRole);
        response.put("roles", roles);
        response.put("permissions", permissions);
        response.put("isActive", user.getIsActive());
        if (user.getTeacher() != null) {
            response.put("teacherId", user.getTeacher().getId());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/switch-role")
    public ResponseEntity<?> switchRole(@RequestBody Map<String, String> req) {
        String newRole = req.get("role");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null)
            return ResponseEntity.status(401).body("Not authenticated");

        User user = null;
        Object principal = auth.getPrincipal();

        try {
            if (principal instanceof com.mitmeerut.CFM_Portal.security.user.CustomUserDetails) {
                Long userId = ((com.mitmeerut.CFM_Portal.security.user.CustomUserDetails) principal).getUserId();
                user = userService.findById(userId);
            } else {
                String email = auth.getName();
                user = userService.findByEmail(email);
            }
        } catch (Exception e) {
            return ResponseEntity.status(404).body("User lookup exception: " + e.getMessage());
        }

        if (user == null) {
            String principalClass = principal != null ? principal.getClass().getName() : "null";
            String principalName = auth.getName();
            return ResponseEntity.status(404)
                    .body("User not found. Principal: " + principalClass + ", Name: " + principalName);
        }

        java.util.List<String> effectiveRoles = userService.getEffectiveRoles(user);
        if (!effectiveRoles.contains(newRole)) {
            return ResponseEntity.status(403).body("Role switch not allowed for: " + newRole);
        }

        // 🔥 PERSIST: Save last active role to DB
        user.setLastActiveRole(newRole);
        userService.updateUser(user);

        List<String> permissions = rolePermissionService.getPermissionsByRole(newRole);
        List<String> roles = userService.getEffectiveRoles(user);

        String newToken = jwtTokenProvider.generateToken(user.getEmail(), user.getId(), newRole, roles, permissions);

        Map<String, Object> response = new HashMap<>();
        response.put("token", newToken);
        response.put("activeRole", newRole);
        response.put("roles", roles);
        response.put("permissions", permissions);
        return ResponseEntity.ok(response);
    }
}
