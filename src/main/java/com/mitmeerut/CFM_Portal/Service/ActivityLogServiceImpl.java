package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.Activity_Log;
import com.mitmeerut.CFM_Portal.Repository.ActivityLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.Map;

@Service
public class ActivityLogServiceImpl implements ActivityLogService {
    private final ActivityLogRepository activityLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public ActivityLogServiceImpl(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @Override
    public Activity_Log log(Activity_Log entry) {
        return activityLogRepository.save(entry);
    }

    @Override
    public Activity_Log logAction(com.mitmeerut.CFM_Portal.Model.Teacher actor, String action, String targetType,
            Long targetId, String details) {
        Activity_Log log = new Activity_Log();
        log.setActor(actor);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);

        try {
            // Ensure details is valid JSON
            if (details != null && !details.trim().startsWith("{") && !details.trim().startsWith("[")) {
                Map<String, String> wrapper = java.util.Collections.singletonMap("message", details);
                log.setDetails(objectMapper.writeValueAsString(wrapper));
            } else {
                log.setDetails(details);
            }
        } catch (Exception e) {
            log.setDetails("{}");
        }

        return activityLogRepository.save(log);
    }

    @Override
    public List<Activity_Log> findByActorId(Long actorId) {
        return activityLogRepository.findByActor_Id(actorId);
    }

    @Override
    public Optional<Activity_Log> findById(Long id) {
        return activityLogRepository.findById(id);
    }

    @Override
    public void delete(Long id) {
        activityLogRepository.deleteById(id);
    }
}
