package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.Model.Teacher;
import com.mitmeerut.CFM_Portal.Model.User;
import com.mitmeerut.CFM_Portal.Model.RolePermission;
import com.mitmeerut.CFM_Portal.Repository.TeacherRepository;
import com.mitmeerut.CFM_Portal.Repository.UserRepository;
import com.mitmeerut.CFM_Portal.Service.CloudinaryService;
import com.mitmeerut.CFM_Portal.Service.RolePermissionService;
import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = { "http://localhost:5000", "http://localhost:5173" })
public class UserProfileController {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final PasswordEncoder passwordEncoder;
    private final RolePermissionService rolePermissionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserProfileController(TeacherRepository teacherRepository,
            UserRepository userRepository,
            CloudinaryService cloudinaryService,
            PasswordEncoder passwordEncoder,
            RolePermissionService rolePermissionService) {
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
        this.passwordEncoder = passwordEncoder;
        this.rolePermissionService = rolePermissionService;
    }

    /**
     * Get current user's profile
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Teacher teacher = userDetails.getTeacher();
        User user = userDetails.getUser();

        Map<String, Object> profile = new HashMap<>();
        String fullName = teacher != null ? teacher.getName() : user.getUsername();
        profile.put("id", teacher != null ? teacher.getId() : user.getId());
        profile.put("name", fullName);

        // Split name into first and last for personalized UI
        String firstName = "";
        String lastName = "";
        if (fullName != null && !fullName.trim().isEmpty()) {
            String[] parts = fullName.trim().split("\\s+");
            firstName = parts[0];
            if (parts.length > 1) {
                lastName = parts[parts.length - 1]; // Use last part as last name
            }
        }
        profile.put("firstName", firstName);
        profile.put("lastName", lastName);

        profile.put("email", teacher != null ? teacher.getEmailOfficial() : user.getEmail());
        profile.put("designation", teacher != null ? teacher.getDesignation() : "Admin");
        profile.put("employeeCode", teacher != null ? teacher.getEmployeeCode() : null);
        profile.put("contactNumber", teacher != null ? teacher.getContactNumber() : null);
        // Return profileImageUrl from Teacher if exists, otherwise from User
        String profileImageUrl = teacher != null ? teacher.getProfileImageUrl() : user.getProfileImageUrl();
        profile.put("profileImageUrl", profileImageUrl);
        profile.put("role", user.getRole());
        profile.put("username", user.getUsername());

        if (teacher != null && teacher.getDepartment() != null) {
            profile.put("departmentId", teacher.getDepartment().getId());
            profile.put("departmentName", teacher.getDepartment().getName());
        }

        return ResponseEntity.ok(profile);
    }

    /**
     * Get current user's permissions based on their role
     */
    @GetMapping("/permissions")
    public ResponseEntity<Map<String, Object>> getPermissions(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String roleName = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .findFirst().orElse("TEACHER");

        try {
            RolePermission rp = rolePermissionService.getByRoleName(roleName)
                    .orElse(null);

            List<String> permissions;
            if (rp != null && rp.getPermissions() != null) {
                permissions = objectMapper.readValue(
                        rp.getPermissions(),
                        new TypeReference<List<String>>() {
                        });
            } else {
                permissions = new ArrayList<>();
            }

            Map<String, Object> result = new HashMap<>();
            result.put("role", roleName);
            result.put("permissions", permissions);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("role", roleName);
            errorMap.put("permissions", new ArrayList<>());
            return ResponseEntity.ok(errorMap);
        }
    }

    /**
     * Update profile information
     */
    @PutMapping
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Teacher teacher = userDetails.getTeacher();
        if (teacher == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Teacher profile not found"));
        }

        if (request.name != null && !request.name.trim().isEmpty()) {
            teacher.setName(request.name.trim());
        }

        if (request.email != null && !request.email.trim().isEmpty()) {
            teacher.setEmailOfficial(request.email.trim());
        }

        if (request.contactNumber != null) {
            teacher.setContactNumber(request.contactNumber.trim());
        }

        teacherRepository.save(teacher);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Profile updated successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Upload profile picture to Cloudinary
     */
    @PostMapping("/avatar")
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Teacher teacher = userDetails.getTeacher();
        User user = userDetails.getUser();

        try {
            String imageUrl = cloudinaryService.uploadImage(file, "cfm_avatars");

            // If user has a Teacher record, save to Teacher; otherwise save to User
            if (teacher != null) {
                teacher.setProfileImageUrl(imageUrl);
                teacherRepository.save(teacher);
            } else {
                user.setProfileImageUrl(imageUrl);
                userRepository.save(user);
            }

            Map<String, Object> avatarResponse = new HashMap<>();
            avatarResponse.put("message", "Avatar uploaded successfully");
            avatarResponse.put("profileImageUrl", imageUrl);
            return ResponseEntity.ok(avatarResponse);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to upload avatar: " + e.getMessage()));
        }
    }

    /**
     * Upload profile picture from base64
     */
    @PostMapping("/avatar/base64")
    public ResponseEntity<Map<String, Object>> uploadAvatarBase64(
            @RequestBody AvatarBase64Request request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Teacher teacher = userDetails.getTeacher();
        if (teacher == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Teacher profile not found"));
        }

        try {
            String imageUrl = cloudinaryService.uploadBase64Image(request.image, "cfm_avatars");
            teacher.setProfileImageUrl(imageUrl);
            teacherRepository.save(teacher);

            Map<String, Object> avatarResponse = new HashMap<>();
            avatarResponse.put("message", "Avatar uploaded successfully");
            avatarResponse.put("profileImageUrl", imageUrl);
            return ResponseEntity.ok(avatarResponse);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to upload avatar: " + e.getMessage()));
        }
    }

    /**
     * Change password
     */
    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        User user = userDetails.getUser();

        // Verify current password
        if (!passwordEncoder.matches(request.currentPassword, user.getPasswordHash())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Current password is incorrect");
            return ResponseEntity.badRequest().body(error);
        }

        // Validate new password
        if (request.newPassword == null || request.newPassword.length() < 6) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "New password must be at least 6 characters");
            return ResponseEntity.badRequest().body(error);
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.newPassword));
        userRepository.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Password changed successfully");
        return ResponseEntity.ok(response);
    }

    // ==================== Request DTOs ====================

    public static class UpdateProfileRequest {
        public String name;
        public String email;
        public String contactNumber;
    }

    public static class AvatarBase64Request {
        public String image;
    }

    public static class ChangePasswordRequest {
        public String currentPassword;
        public String newPassword;
    }
}
