package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.Activity_Log;
import java.util.List;
import java.util.Optional;

public interface ActivityLogService {
    Activity_Log log(Activity_Log entry);

    Activity_Log logAction(com.mitmeerut.CFM_Portal.Model.Teacher actor, String action, String targetType,
            Long targetId, String details);

    List<Activity_Log> findByActorId(Long actorId);

    Optional<Activity_Log> findById(Long id);

    void delete(Long id);
}
