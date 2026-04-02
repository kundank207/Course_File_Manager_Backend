package com.mitmeerut.CFM_Portal.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mitmeerut.CFM_Portal.Model.Activity_Log;
import com.mitmeerut.CFM_Portal.Model.Notification;
import com.mitmeerut.CFM_Portal.Model.User;
import com.mitmeerut.CFM_Portal.Repository.ActivityLogRepository;
import com.mitmeerut.CFM_Portal.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AsyncEventService {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Async("taskExecutor")
    public void logRegistrationActivity(Long userId, String name, String email) {
        try {
            Activity_Log log = new Activity_Log();
            log.setActor(null);
            log.setAction("New Registration");
            log.setTargetType("User");
            log.setTargetId(userId);

            Map<String, String> logDetails = new HashMap<>();
            logDetails.put("event", "New teacher registration");
            logDetails.put("name", name);
            logDetails.put("email", email);
            log.setDetails(objectMapper.writeValueAsString(logDetails));

            activityLogRepository.save(log);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Async("taskExecutor")
    public void notifyAdminsOfRegistration(Long userId, String name, String email) {
        java.util.List<User> admins = userRepo.findByRole(User.userRole.ADMIN, org.springframework.data.domain.Pageable.unpaged()).getContent();
        for (User admin : admins) {
            Notification note = new Notification();
            note.setUser(admin);
            note.setType("NEW_USER_REGISTRATION");
            note.setTitle("New Teacher Registration");
            note.setMessage(name + " has registered and is pending approval.");

            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("userId", userId);
                payload.put("name", name);
                payload.put("email", email);
                note.setPayload(objectMapper.writeValueAsString(payload));
            } catch (Exception e) {
                note.setPayload("{}");
            }
            notificationService.send(note);
        }

        // Send Email Async
        String approveLink = baseUrl + "/api/admin/approve/" + userId;
        emailService.sendEmail(
                adminEmail,
                "New Teacher Registration",
                "A new teacher registered.\n\nName: " + name +
                        "\nEmail: " + email +
                        "\nApprove: " + approveLink);
    }
}
