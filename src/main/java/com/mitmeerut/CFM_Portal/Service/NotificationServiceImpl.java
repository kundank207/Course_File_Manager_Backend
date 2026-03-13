package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.Notification;
import com.mitmeerut.CFM_Portal.Repository.NotificationRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
            org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public Notification send(Notification notification) {
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(java.time.LocalDateTime.now());
        }
        Notification saved = notificationRepository.save(notification);

        // Broadcast DTO to specific user to prevent recursion
        java.util.Map<String, Object> dto = new java.util.HashMap<>();
        dto.put("id", saved.getId());
        dto.put("type", saved.getType());
        dto.put("title", saved.getTitle());
        dto.put("message", saved.getMessage());
        dto.put("payload", saved.getPayload());
        dto.put("isRead", saved.getIsRead());
        dto.put("createdAt", saved.getCreatedAt().toString());
        if (saved.getSender() != null) {
            dto.put("senderName", saved.getSender().getUsername());
        }

        messagingTemplate.convertAndSend(
                "/topic/notifications/" + saved.getUser().getId(),
                dto);
        return saved;
    }

    @Override
    public List<Notification> findByUserId(Long userId) {
        return notificationRepository.findByUser_Id(userId);
    }

    @Override
    public List<Notification> findByInstituteId(Long instituteId) {
        return notificationRepository.findByUser_Teacher_Department_Institute_Id(instituteId);
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return notificationRepository.findById(id);
    }

    @Override
    public void delete(Long id) {
        notificationRepository.deleteById(id);
    }

    @Override
    public void markAsReadBySenderAndType(Long userId, Long senderId, String type) {
        List<Notification> notifications = notificationRepository.findByUser_IdAndSender_IdAndTypeAndIsReadFalse(userId,
                senderId, type);
        notifications.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(notifications);
    }
}
