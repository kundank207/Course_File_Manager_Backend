package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.Notification;
import com.mitmeerut.CFM_Portal.Model.User;
import com.mitmeerut.CFM_Portal.Repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper service to create notifications and send emails for system events
 */
@Service
public class NotificationHelperService {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    public NotificationHelperService(
            NotificationRepository notificationRepository,
            NotificationService notificationService,
            EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Send notification when a course file is submitted for review
     */
    public void notifyFileSubmitted(User recipient, String teacherName, String courseCode, Long courseFileId) {
        String message = String.format("%s has submitted course file for %s for your review", teacherName, courseCode);

        createNotification(recipient, "FILE_SUBMITTED", message, courseFileId);

        // Send email
        String emailSubject = "CFM: New Course File Submitted for Review";
        String emailBody = String.format(
                "Dear %s,\n\n%s\n\nPlease login to the CFM portal to review the submission.\n\nBest regards,\nCFM Portal",
                recipient.getUsername(), message);
        sendEmailAsync(recipient.getEmail(), emailSubject, emailBody);
    }

    /**
     * Send notification when a course file is approved
     */
    public void notifyFileApproved(User recipient, String approverName, String courseCode, String approverRole) {
        String message = String.format("Your course file for %s has been approved by %s (%s)", courseCode, approverName,
                approverRole);

        createNotification(recipient, "FILE_APPROVED", message, null);

        // Send email
        String emailSubject = "CFM: Course File Approved ✓";
        String emailBody = String.format(
                "Dear %s,\n\n%s\n\nCongratulations on your approval!\n\nBest regards,\nCFM Portal",
                recipient.getUsername(), message);
        sendEmailAsync(recipient.getEmail(), emailSubject, emailBody);
    }

    /**
     * Send notification when a course file is returned for revision
     */
    public void notifyFileReturned(User recipient, String reviewerName, String courseCode, String comment) {
        String message = String.format("Your course file for %s has been returned by %s for revision", courseCode,
                reviewerName);

        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        payload.put("comment", comment);

        createNotificationWithPayload(recipient, "FILE_RETURNED", payload);

        // Send email
        String emailSubject = "CFM: Course File Returned for Revision";
        String emailBody = String.format(
                "Dear %s,\n\n%s\n\nReviewer's Comment:\n\"%s\"\n\nPlease make the necessary changes and resubmit.\n\nBest regards,\nCFM Portal",
                recipient.getUsername(), message, comment != null ? comment : "No comment provided");
        sendEmailAsync(recipient.getEmail(), emailSubject, emailBody);
    }

    /**
     * Send notification when a comment is added to a course file
     */
    public void notifyCommentAdded(User recipient, String authorName, String courseCode) {
        String message = String.format("%s added a comment on %s", authorName, courseCode);

        createNotification(recipient, "COMMENT_ADDED", message, null);

        // Send email
        String emailSubject = "CFM: New Comment on Course File";
        String emailBody = String.format(
                "Dear %s,\n\n%s\n\nLogin to view and respond to the comment.\n\nBest regards,\nCFM Portal",
                recipient.getUsername(), message);
        sendEmailAsync(recipient.getEmail(), emailSubject, emailBody);
    }

    /**
     * Generic notification creation
     */
    public void createNotification(User recipient, String type, String message, Long relatedId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        if (relatedId != null) {
            payload.put("relatedId", relatedId);
        }
        createNotificationWithPayload(recipient, type, payload);
    }

    private void createNotificationWithPayload(User recipient, String type, Map<String, Object> payload) {
        try {
            Notification notification = new Notification();
            notification.setUser(recipient);
            notification.setType(type);

            String msg = (String) payload.get("message");
            notification.setTitle(type.replace("_", " "));
            notification.setMessage(msg);

            notification.setPayload(objectMapper.writeValueAsString(payload));
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());

            // Use notificationService for real-time broadcast
            notificationService.send(notification);
        } catch (Exception e) {
            System.err.println("Failed to create notification: " + e.getMessage());
        }
    }

    private void sendEmailAsync(String to, String subject, String body) {
        // Run email sending in a separate thread to not block the main flow
        new Thread(() -> {
            try {
                emailService.sendEmail(to, subject, body);
            } catch (Exception e) {
                System.err.println("Failed to send email: " + e.getMessage());
            }
        }).start();
    }
}
