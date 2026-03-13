package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.Model.Notification;
import com.mitmeerut.CFM_Portal.Service.NotificationService;
import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = { "http://localhost:5000", "http://localhost:5173" }, allowCredentials = "true")
public class NotificationController {

    private final NotificationService notificationService;
    private final com.mitmeerut.CFM_Portal.Repository.NotificationRepository notificationRepository;

    public NotificationController(NotificationService notificationService,
            com.mitmeerut.CFM_Portal.Repository.NotificationRepository notificationRepository) {
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(notificationRepository.findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(userId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<Notification>> getAllMyNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        long count = notificationRepository.countByUser_IdAndIsReadFalse(userId);

        // Return JSON object
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable("id") Long id) {
        notificationRepository.findById(id).ifPresent(note -> {
            note.setIsRead(true);
            notificationRepository.save(note);
        });
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Map<String, Object>> markAllRead(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        List<Notification> unread = notificationRepository.findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(userId);
        for (Notification note : unread) {
            note.setIsRead(true);
        }
        notificationRepository.saveAll(unread);
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteNotification(@PathVariable("id") Long id) {
        notificationService.delete(id);
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createNotification(
            @RequestBody Notification notification) {
        Notification saved = notificationService.send(notification);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("notification", saved);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/mark-read")
    public ResponseEntity<Map<String, Object>> markAllAsReadPut(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return markAllRead(userDetails);
    }

    @PutMapping("/mark-read/{id}")
    public ResponseEntity<Map<String, Object>> markAsReadPut(@PathVariable("id") Long id) {
        return markAsRead(id);
    }

    @PostMapping("/clear-chat")
    public ResponseEntity<Map<String, Object>> clearChatNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("senderId") Long senderId) {
        Long userId = userDetails.getUser().getId();
        notificationService.markAsReadBySenderAndType(userId, senderId, "CHAT_MESSAGE");
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
}
