package com.mitmeerut.CFM_Portal.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import com.mitmeerut.CFM_Portal.Model.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // find notifications by user
    List<Notification> findByUser_Id(Long userId);

    // find notifications by user, sorted by newest first
    List<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId);

    // count unread notifications for user
    long countByUser_IdAndIsReadFalse(Long userId);

    // find unread notifications for user
    List<Notification> findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    // find unread by user, sender and type
    List<Notification> findByUser_IdAndSender_IdAndTypeAndIsReadFalse(Long userId, Long senderId, String type);

    // notifications for an institute (via user -> teacher -> department ->
    // institute)
    List<Notification> findByUser_Teacher_Department_Institute_Id(Long instituteId);
}
