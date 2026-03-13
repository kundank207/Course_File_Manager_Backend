package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.Teacher;
import com.mitmeerut.CFM_Portal.Model.User;
import com.mitmeerut.CFM_Portal.Model.User.userRole;
import com.mitmeerut.CFM_Portal.Repository.TeacherRepository;
import com.mitmeerut.CFM_Portal.Repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepo;
    @Autowired
    private TeacherRepository teacherRepo;
    @Autowired
    private com.mitmeerut.CFM_Portal.Repository.ActivityLogRepository activityLogRepository;
    @Autowired
    private com.mitmeerut.CFM_Portal.Repository.NotificationRepository notificationRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private NotificationService notificationService;
    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    public User registerTeacherUser(String name, String email, String password) {

        if (userRepo.existsByEmail(email))
            throw new RuntimeException("Email already registered!");

        if (userRepo.existsByUsername(name))
            throw new RuntimeException("Username already exists!");

        User user = new User();
        user.setUsername(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(userRole.TEACHER);
        user.setIsActive(false);

        User savedUser = userRepo.save(user);

        Teacher t = new Teacher();
        t.setName(name);
        t.setEmailOfficial(email);
        t.setIsActive(false);

        teacherRepo.save(t);

        savedUser.setTeacher(t);
        userRepo.save(savedUser);

        // Log activity
        try {
            com.mitmeerut.CFM_Portal.Model.Activity_Log log = new com.mitmeerut.CFM_Portal.Model.Activity_Log();
            log.setActor(null); // Registration is a system event
            log.setAction("New Registration");
            log.setTargetType("User");
            log.setTargetId(savedUser.getId());

            Map<String, String> logDetails = new HashMap<>();
            logDetails.put("event", "New teacher registration");
            logDetails.put("name", name);
            logDetails.put("email", email);
            log.setDetails(objectMapper.writeValueAsString(logDetails));

            activityLogRepository.save(log);
        } catch (Exception e) {
            e.printStackTrace(); // Log but don't fail registration
        }

        // Notify Admin
        List<User> admins = userRepo.findByRole(userRole.ADMIN);
        for (User admin : admins) {
            com.mitmeerut.CFM_Portal.Model.Notification note = new com.mitmeerut.CFM_Portal.Model.Notification();
            note.setUser(admin);
            note.setType("NEW_USER_REGISTRATION");
            note.setTitle("New Teacher Registration");
            note.setMessage(name + " has registered and is pending approval.");

            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("userId", savedUser.getId());
                payload.put("name", name);
                payload.put("email", email);
                note.setPayload(objectMapper.writeValueAsString(payload));
            } catch (Exception e) {
                note.setPayload("{}");
            }

            note.setCreatedAt(java.time.LocalDateTime.now());
            note.setIsRead(false);

            notificationService.send(note);
        }

        String approveLink = baseUrl + "/api/admin/approve/" + savedUser.getId();
        emailService.sendEmail(
                adminEmail,
                "New Teacher Registration",
                "A new teacher registered.\n\nName: " + name +
                        "\nEmail: " + email +
                        "\nApprove: " + approveLink);

        return savedUser;
    }

    @Override
    public Boolean login(String email, String password) {

        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null)
            return false;

        if (!Boolean.TRUE.equals(user.getIsActive()))
            return false;

        // return user.getPasswordHash().equals(password);
        return passwordEncoder.matches(password, user.getPasswordHash());
    }

    @Override
    public List<User> getPendingTeachers() {
        return userRepo.findByRoleAndIsActive(userRole.TEACHER, false);
    }

    @Override
    public List<User> getAllTeachers() {
        return userRepo.findByRole(userRole.TEACHER);
    }

    @Override
    public User approveUser(Long userId) {

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsActive(true);
        userRepo.save(user);

        if (user.getTeacher() != null) {
            Teacher t = user.getTeacher();
            t.setIsActive(true);
            teacherRepo.save(t);
        }

        // Notify Teacher of approval
        com.mitmeerut.CFM_Portal.Model.Notification note = new com.mitmeerut.CFM_Portal.Model.Notification();
        note.setUser(user);
        note.setType("USER_APPROVED");
        note.setTitle("Account Approved");
        note.setMessage("Your account has been approved by the Administrator. You can now login.");
        note.setCreatedAt(java.time.LocalDateTime.now());
        note.setIsRead(false);

        notificationService.send(note);

        return user;
    }

    @Override
    public void deleteUser(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getTeacher() != null)
            teacherRepo.delete(user.getTeacher());

        userRepo.delete(user);
    }

    @Override
    public User findByEmail(String email) {
        return userRepo.findByEmail(email).orElse(null);
    }

    @Override
    public User findById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Override
    public User updateUser(User user) {
        return userRepo.save(user);
    }

    @Autowired
    private com.mitmeerut.CFM_Portal.Repository.CourseTeacherRepository courseTeacherRepo;

    @Override
    public List<String> getEffectiveRoles(User user) {
        List<String> roles = new java.util.ArrayList<>();
        if (user.getRole() != null) {
            String primaryRole = user.getRole().name();
            roles.add(primaryRole);

            // HOD can also act as TEACHER
            if ("HOD".equals(primaryRole)) {
                if (!roles.contains("TEACHER"))
                    roles.add("TEACHER");
            }

            // Check if user is a Subject Head (even if primary role is TEACHER)
            if (user.getTeacher() != null) {
                boolean isSubjectHead = courseTeacherRepo
                        .existsByTeacherIdAndIsSubjectHeadTrue(user.getTeacher().getId());
                if (isSubjectHead) {
                    if (!roles.contains("SUBJECTHEAD"))
                        roles.add("SUBJECTHEAD");
                    if (!roles.contains("TEACHER"))
                        roles.add("TEACHER");
                }
            }
        }
        return roles;
    }

}
