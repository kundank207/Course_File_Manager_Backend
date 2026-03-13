package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.Notification;
import java.util.List;
import java.util.Optional;

public interface NotificationService {
    Notification send(Notification notification);

    List<Notification> findByUserId(Long userId);

    List<Notification> findByInstituteId(Long instituteId);

    Optional<Notification> findById(Long id);

    void delete(Long id);

    void markAsReadBySenderAndType(Long userId, Long senderId, String type);
}
