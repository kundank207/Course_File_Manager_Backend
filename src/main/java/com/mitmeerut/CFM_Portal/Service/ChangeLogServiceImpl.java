package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.ChangeLog;
import com.mitmeerut.CFM_Portal.Model.CourseFile;
import com.mitmeerut.CFM_Portal.Model.Teacher;
import com.mitmeerut.CFM_Portal.Repository.ChangeLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ChangeLogServiceImpl implements ChangeLogService {

    @Autowired
    private ChangeLogRepository changeLogRepository;

    @Override
    public void logChange(CourseFile courseFile, Teacher updatedBy, String type, String section, String description) {
        ChangeLog log = new ChangeLog(
                courseFile,
                courseFile.getRevisionNumber(),
                updatedBy,
                type,
                section,
                description);
        changeLogRepository.save(log);
    }

    @Override
    public List<ChangeLog> getLogsForFile(Long courseFileId) {
        return changeLogRepository.findByCourseFileIdOrderByCreatedAtDesc(courseFileId);
    }

    @Override
    public List<ChangeLog> getLogsForVersion(Long courseFileId, Integer versionId) {
        return changeLogRepository.findByCourseFileIdAndVersionIdOrderByCreatedAtDesc(courseFileId, versionId);
    }
}
