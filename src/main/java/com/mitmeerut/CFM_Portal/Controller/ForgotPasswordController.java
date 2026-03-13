package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.Model.PasswordResetToken;
import com.mitmeerut.CFM_Portal.Model.User;
import com.mitmeerut.CFM_Portal.Repository.PasswordResetTokenRepository;
import com.mitmeerut.CFM_Portal.Repository.UserRepository;
import com.mitmeerut.CFM_Portal.Service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class ForgotPasswordController {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public ForgotPasswordController(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Step 1: Request password reset - sends OTP to email
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Email is required");
            return ResponseEntity.badRequest().body(response);
        }

        User user = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
        if (user == null) {
            // Don't reveal if email exists or not (security)
            Map<String, String> response = new HashMap<>();
            response.put("message", "If this email is registered, you will receive an OTP shortly");
            return ResponseEntity.ok(response);
        }

        // Generate 6-digit OTP
        String otp = generateOTP();

        // Create token with 5 minute expiry
        PasswordResetToken token = new PasswordResetToken(
                user,
                otp,
                LocalDateTime.now().plusMinutes(5));
        tokenRepository.save(token);

        // Send OTP email
        String subject = "CFM Portal - Password Reset OTP";
        String body = String.format(
                "Dear %s,\n\n" +
                        "You have requested to reset your password.\n\n" +
                        "Your OTP code is: %s\n\n" +
                        "This code will expire in 5 minutes.\n\n" +
                        "If you did not request this, please ignore this email.\n\n" +
                        "Best regards,\nCFM Portal Team",
                user.getUsername(), otp);

        try {
            emailService.sendEmail(user.getEmail(), subject, body);
        } catch (Exception e) {
            System.err.println("Failed to send OTP email: " + e.getMessage());
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "OTP sent to your registered email");
        response.put("email", maskEmail(user.getEmail()));
        return ResponseEntity.ok(response);
    }

    /**
     * Step 2: Verify OTP
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOTP(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");

        if (email == null || otp == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Email and OTP are required");
            return ResponseEntity.badRequest().body(response);
        }

        User user = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
        if (user == null) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "Invalid email");
            return ResponseEntity.badRequest().body(errorMap);
        }
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByUserAndOtpAndIsUsedFalse(user, otp);

        if (tokenOpt.isEmpty()) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "Invalid OTP");
            return ResponseEntity.badRequest().body(errorMap);
        }

        PasswordResetToken token = tokenOpt.get();
        if (token.isExpired()) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "OTP has expired. Please request a new one");
            return ResponseEntity.badRequest().body(response);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("valid", true);
        response.put("message", "OTP verified successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Step 3: Reset password with verified OTP
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        String newPassword = request.get("newPassword");

        if (email == null || otp == null || newPassword == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Email, OTP, and new password are required");
            return ResponseEntity.badRequest().body(response);
        }

        if (newPassword.length() < 6) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Password must be at least 6 characters");
            return ResponseEntity.badRequest().body(response);
        }

        User user = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
        if (user == null) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Invalid email");
            return ResponseEntity.badRequest().body(errorMap);
        }
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByUserAndOtpAndIsUsedFalse(user, otp);

        if (tokenOpt.isEmpty()) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Invalid OTP");
            return ResponseEntity.badRequest().body(errorMap);
        }

        PasswordResetToken token = tokenOpt.get();
        if (token.isExpired()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "OTP has expired");
            return ResponseEntity.badRequest().body(response);
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used
        token.setIsUsed(true);
        tokenRepository.save(token);

        // Send confirmation email
        String subject = "CFM Portal - Password Changed Successfully";
        String body = String.format(
                "Dear %s,\n\n" +
                        "Your password has been successfully changed.\n\n" +
                        "If you did not make this change, please contact support immediately.\n\n" +
                        "Best regards,\nCFM Portal Team",
                user.getUsername());

        try {
            emailService.sendEmail(user.getEmail(), subject, body);
        } catch (Exception e) {
            System.err.println("Failed to send password change confirmation: " + e.getMessage());
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Password reset successfully. Please login with your new password");
        return ResponseEntity.ok(response);
    }

    /**
     * Generate 6-digit OTP
     */
    private String generateOTP() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     * Mask email for display (e.g., k***@gmail.com)
     */
    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1)
            return email;
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
